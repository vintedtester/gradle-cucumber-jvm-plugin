package com.commercehub.gradle.cucumber

import groovyx.gpars.GParsPool
import net.masterthought.cucumber.Configuration
import net.masterthought.cucumber.ReportParser
import net.masterthought.cucumber.ReportResult
import net.masterthought.cucumber.json.Feature
import org.gradle.api.GradleException
import org.gradle.api.file.FileTree
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.SourceSet

import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicBoolean
import java.util.jar.JarEntry
import java.util.jar.JarInputStream

/**
 * Created by jgelais on 6/16/15.
 */
class CucumberRunner {
    private static final String CUCUMBER_MAIN_NEW = 'io.cucumber.core.cli.Main'
    private static final String CUCUMBER_MAIN_OLD = 'cucumber.api.cli.Main'

    /**
     * Result files can be quite large when including embedded images, etc. When checking if a result file is empty,
     * we don't want to process large files. A valid, empty result file is a JSON array and generally looks like
     * "[ ]". It could have various whitespace around it or inside the brackets (spaces, newlines, etc.), so we'll
     * pick a number sufficiently larger to account for that.
     * Any file with a size of this or larger won't be considered when checking for an empty result.
     */
    private static final int EMPTY_RESULT_FILE_MAX_SIZE_IN_BYTES = 16

    CucumberRunnerOptions options
    CucumberTestResultCounter testResultCounter
    List<String> jvmArgs
    Map<String, String> systemProperties
    Configuration configuration
    Logger gradleLogger

    @SuppressWarnings('ParameterCount')
    CucumberRunner(CucumberRunnerOptions options, Configuration configuration,
                   CucumberTestResultCounter testResultCounter,
                   List<String> jvmArgs, Map<String, String> systemProperties,
                   Logger gradleLogger) {
        this.options = options
        this.testResultCounter = testResultCounter
        this.configuration = configuration
        this.jvmArgs = jvmArgs
        this.systemProperties = systemProperties
        this.gradleLogger = gradleLogger
    }

    boolean run(SourceSet sourceSet, File resultsDir) {
        resultsDir.deleteDir()
        resultsDir.mkdirs()

        AtomicBoolean hasFeatureParseErrors = new AtomicBoolean(false)

        def features = findFeatures(sourceSet)
        def classpath = sourceSet.runtimeClasspath.toList()
        def batchSize = (int) Math.ceil(features.files.size() / options.maxParallelForks)

        testResultCounter.beforeSuite(features.files.size())
        GParsPool.withPool(options.maxParallelForks) {
            features.files.collate(batchSize).eachWithIndexParallel { featureBatch, batchId ->
                def runId = "feature-batch-${batchId}"
                File resultsFile = new File(resultsDir, "${runId}.json")
                File consoleOutLogFile = new File(resultsDir, "${runId}-out.log")
                File consoleErrLogFile = new File(resultsDir, "${runId}-err.log")
                File junitResultsFile = new File(resultsDir, "${runId}.xml")

                List<String> args = new CommandArgumentsBuilder(options).buildArguments(featureBatch, resultsFile, junitResultsFile);

                new JavaProcessLauncher(cucumberMainClass(classpath), classpath)
                        .setArgs(args)
                        .setJvmArgs(jvmArgs)
                        .setConsoleOutLogFile(consoleOutLogFile)
                        .setConsoleErrLogFile(consoleErrLogFile)
                        .setSystemProperties(systemProperties)
                        .setGradleLogger(gradleLogger)
                        .execute()

                if (resultsFile.exists()) {
                    // if tags are used, they may exclude all features in a file, we don't want consider that an error
                    if (options.tags.empty || !isResultFileEmpty(resultsFile)) {
                        handleResult(resultsFile, consoleOutLogFile, hasFeatureParseErrors, sourceSet)
                    }
                } else {
                    hasFeatureParseErrors.set(true)
                    if (consoleErrLogFile.exists()) {
                        gradleLogger.error(consoleErrLogFile.text)
                    }
                }
            }
        }

        if (hasFeatureParseErrors.get()) {
            throw new GradleException('One or more feature files failed to parse. See error output above')
        }

        testResultCounter.afterSuite()
        return !testResultCounter.hadFailures()
    }

    private static String cucumberMainClass(List<File> classpath) {
        File jar = classpath.find({ file -> file.name.startsWith('cucumber-core') })
        if (jar) {
            JarInputStream jarFile = new JarInputStream(new FileInputStream(jar))
            JarEntry jarEntry
            while (jarEntry = jarFile.nextJarEntry) {
                if (jarEntry.name.endsWith(".class")) {
                    String className = jarEntry.name.replaceAll('/', '.');
                    className = className.substring(0, className.lastIndexOf('.'));
                    if (className == CUCUMBER_MAIN_NEW) {
                        return CUCUMBER_MAIN_NEW
                    }
                }
            }
        }
        // fallback to old
        return CUCUMBER_MAIN_OLD;
    }

    private boolean isResultFileEmpty(File resultsFile) {
        resultsFile.size() == 0 ||
                (resultsFile.size() < EMPTY_RESULT_FILE_MAX_SIZE_IN_BYTES &&
                        resultsFile.text.replaceAll(/\s+/, '') == '[]')
    }

    String getFeatureNameFromFile(File file, SourceSet sourceSet) {
        String featureName = file.name
        sourceSet.resources.srcDirs.each { File resourceDir ->
            if (isFileChildOfDirectory(file, resourceDir)) {
                featureName = convertPathToPackage(getReleativePath(file, resourceDir))
            }
        }

        return featureName
    }

    List<Feature> parseFeatureResult(File jsonReport) {
        configuration.getEmbeddingDirectory().mkdirs()
        ReportParser reportParser = new ReportParser(configuration);
        List<Feature> featuresFromJson = reportParser.parseJsonFiles([jsonReport.absolutePath])
        ReportResult reportResult = new ReportResult(featuresFromJson, configuration)
        return reportResult.getAllFeatures()
    }

    CucumberFeatureResult createResult(Feature feature) {
        CucumberFeatureResult result = new CucumberFeatureResult(
                totalScenarios: feature.passedScenarios + feature.failedScenarios,
                failedScenarios: feature.failedScenarios,
                totalSteps: feature.passedSteps + feature.failedSteps,
                failedSteps: feature.failedSteps,
                skippedSteps: feature.skippedSteps,
                pendingSteps: feature.pendingSteps,
                undefinedSteps: feature.undefinedSteps
        )

        return result
    }

    protected FileTree findFeatures(SourceSet sourceSet) {
        sourceSet.resources.matching {
            options.featureRoots.each {
                include("${it}/**/*.feature")
            }
        }
    }

    private void handleResult(File resultsFile, File consoleOutLogFile,
                              AtomicBoolean hasFeatureParseErrors, SourceSet sourceSet) {
        List<CucumberFeatureResult> results = parseFeatureResult(resultsFile).collect {
            gradleLogger.debug("Logging result for $it.name")
            createResult(it)
        }
        results.each { CucumberFeatureResult result ->
            testResultCounter.afterFeature(result)

            if (result.hadFailures()) {
                if (result.undefinedSteps > 0) {
                    hasFeatureParseErrors.set(true)
                }
                gradleLogger.error('{}:\r\n {}', sourceSet.name, consoleOutLogFile.text)
            }
        }
    }

    private String convertPathToPackage(Path path) {
        return path.toString().replace(File.separator, '.')
    }

    private Path getReleativePath(File file, File dir) {
        return Paths.get(dir.toURI()).relativize(Paths.get(file.toURI()))
    }

    private boolean isFileChildOfDirectory(File file, File dir) {
        Path child = Paths.get(file.toURI())
        Path parent = Paths.get(dir.toURI())
        return child.startsWith(parent)
    }
}
