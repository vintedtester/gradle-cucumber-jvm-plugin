package com.commercehub.gradle.cucumber

import net.masterthought.cucumber.Configuration
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.testing.Test
import org.gradle.internal.logging.ConsoleRenderer
import org.gradle.internal.logging.progress.ProgressLoggerFactory

/**
 * Created by jgelais on 6/11/15.
 * rholman 12/12/15 - added logic to shorten uri in json files to relative path.
 */
class CucumberTask extends Test implements CucumberRunnerOptions {
    public static final String CUCUMBER_REPORTS_DIR = 'cucumber'
    public static final String CUCUMBER_EXTENSION_NAME = 'cucumber'

    @Internal
    SourceSet sourceSet
    private final CucumberExtension extension = project.extensions[CUCUMBER_EXTENSION_NAME]

    @Input
    List<String> tags = null
    @Input
    Integer maxParallelForks = null
    @Input
    List<String> featureRoots = null
    @Input
    List<String> stepDefinitionRoots = null
    @Input
    List<String> plugins = null
    @Input
    Boolean isDryRun = null
    @Input
    Boolean isMonochrome = null
    @Input
    Boolean isStrict = null
    @Input
    String snippets = null
    @Input
    boolean junitReport = null

    @Override
    @TaskAction
    void executeTests() {
        CucumberRunner runner = createRunner()
        boolean isPassing = runner.run(sourceSet, getResultsDir(), getReportsDir())
        new MasterThoughtReportGenerator(this, getConfiguration()).generateReport(jsonReportFiles)

        if (!isPassing) {
            handleTestFailures()
        }
    }

    protected CucumberRunner createRunner() {
        ProgressLoggerFactory progressLoggerFactory = services.get(ProgressLoggerFactory)
        new CucumberRunner(this, getConfiguration(),
                new CucumberTestResultCounter(progressLoggerFactory, logger), jvmArgs, systemProperties, logger)
    }

    @OutputFiles
    List<File> getJsonReportFiles() {
        List<File> files = []
        getResultsDir().eachFileMatch(~/^.*\.json$/) {
            files << it
        }

        return files
    }

    @Internal
    Configuration getConfiguration() {
        Configuration configuration = new Configuration(getReportsDir(), "${project.name}-${sourceSet.name}")
        configuration.parallelTesting = true
        configuration.runWithJenkins = false

        return configuration
    }

    @SuppressWarnings('ConfusingMethodName')
    def sourceSet(SourceSet sourceSet) {
        this.sourceSet = sourceSet
    }

    @Internal
    File getResultsDir() {
        File projectResultsDir = (File) project.property('testResultsDir')
        File cucumberResults = new File(projectResultsDir, CUCUMBER_REPORTS_DIR)
        File sourceSetResults = new File(cucumberResults, sourceSet.name)
        sourceSetResults.mkdirs()

        return sourceSetResults
    }

    @Internal
    File getReportsDir() {
        File projectReportsDir = (File) project.property('reportsDir')
        File sourceSetReports = new File(projectReportsDir, sourceSet.name)

        return sourceSetReports
    }

    private void handleTestFailures() {
        String reportUrl = new ConsoleRenderer().asClickableFileUrl(new File(getReportsDir(), 'cucumber-html-reports/feature-overview.html'))
        String message = "There were failing tests. See the report at: $reportUrl"

        if (ignoreFailures ?: extension.ignoreFailures) {
            logger.warn(message)
        } else {
            throw new GradleException(message)
        }
    }

    SourceSet getSourceSet() {
        return sourceSet
    }

    List<String> getTags() {
        return tags ?: extension.tags
    }

    int getMaxParallelForks() {
        return maxParallelForks ?: extension.maxParallelForks
    }

    List<String> getStepDefinitionRoots() {
        return stepDefinitionRoots ?: extension.stepDefinitionRoots
    }

    List<String> getFeatureRoots() {
        return featureRoots ?: extension.featureRoots
    }

    @Override
    List<String> getPlugins() {
        return plugins ?: extension.plugins
    }

    boolean getIsDryRun() {
        return isDryRun ?: extension.isDryRun
    }

    boolean getIsMonochrome() {
        return isMonochrome ?: extension.isMonochrome
    }

    boolean getIsStrict() {
        return isStrict ?: extension.isStrict
    }

    String getSnippets() {
        return snippets ?: extension.snippets
    }

    boolean getJunitReport() {
        return junitReport ?: extension.junitReport
    }
}
