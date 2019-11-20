package com.commercehub.gradle.cucumber

import net.masterthought.cucumber.Configuration
import org.gradle.api.GradleException
import org.gradle.api.tasks.*
import org.gradle.api.tasks.testing.Test
import org.gradle.internal.logging.ConsoleRenderer
import org.gradle.internal.logging.progress.ProgressLoggerFactory

/**
 * Parameters set in task overrides values from global plugin configuration.
 * @see com.commercehub.gradle.cucumber.CucumberExtension
 */
class CucumberTask extends Test implements CucumberRunnerOptions {
    public static final String CUCUMBER_REPORTS_DIR = 'cucumber'
    public static final String CUCUMBER_EXTENSION_NAME = 'cucumber'

    @Internal
    SourceSet sourceSet
    private final CucumberExtension extension = project.extensions[CUCUMBER_EXTENSION_NAME]

    /**
     * A list of tags to identify scenarios to run. Defaults to global plugin configuration.
     */
    @Input
    List<String> tags = null
    /**
     * Maximum number of forked Java processes to start to run tests in parallel.
     * Defaults to global plugin configuration.
     * @see com.commercehub.gradle.cucumber.CucumberExtension#maxParallelForks
     */
    @Input
    Integer maxParallelForks = null
    /**
     * A list of root packages to scan the resources folders on the classpath for feature files.
     * Defaults to global plugin configuration.
     * @see com.commercehub.gradle.cucumber.CucumberExtension#featureRoots
     */
    @Input
    List<String> featureRoots = null
    /**
     * A list of root packages to scan the classpath for glue code.
     * Defaults to global plugin configuration.
     * @see com.commercehub.gradle.cucumber.CucumberExtension#stepDefinitionRoots
     */
    @Input
    List<String> stepDefinitionRoots = null
    /**
     * A list of cucumber plugins passed for execution.
     * Defaults to global plugin configuration.
     * @see com.commercehub.gradle.cucumber.CucumberExtension#plugins
     */
    @Input
    List<String> plugins = null
    /**
     * A boolean value indicating whether glue code execution should be skipped.
     * Defaults to global plugin configuration.
     * @see com.commercehub.gradle.cucumber.CucumberExtension#isDryRun
     */
    @Input
    Boolean isDryRun = null
    /**
     * A boolean value indicating whether terminal output should be without colours.
     * Defaults to global plugin configuration.
     * @see com.commercehub.gradle.cucumber.CucumberExtension#isMonochrome
     */
    @Input
    Boolean isMonochrome = null
    /**
     * A boolean value indicating whether scenarios should be evaluated strictly.
     * Defaults to global plugin configuration.
     * @see com.commercehub.gradle.cucumber.CucumberExtension#isStrict
     */
    @Input
    Boolean isStrict = null
    /**
     * Indicator to cucumber on what style to use for generated step examples. Valid values include `camelcase`, `underscore`.
     * Defaults to global plugin configuration.
     * @see com.commercehub.gradle.cucumber.CucumberExtension#snippets
     */
    @Input
    String snippets = null
    /**
     * List of arguments to pass to the forked test running JVMs.
     * Defaults to empty list.
     */
    @Input
    Boolean junitReport = null

    CucumberTask() {
        group = 'verification'
        description = 'Runs the cucumber tests.'
    }

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
        return new Configuration(getReportsDir(), "${project.name}-${sourceSet.name}")
    }

    @SuppressWarnings('ConfusingMethodName')
    def sourceSet(SourceSet sourceSet) {
        setTestClassesDirs(sourceSet.output.classesDirs)
        this.sourceSet = sourceSet
    }

    @OutputDirectory
    File getResultsDir() {
        File projectResultsDir = (File) project.property('testResultsDir')
        File cucumberResults = new File(projectResultsDir, CUCUMBER_REPORTS_DIR)
        File sourceSetResults = new File(cucumberResults, sourceSet.name)
        sourceSetResults.mkdirs()

        return sourceSetResults
    }

    @OutputDirectory
    File getReportsDir() {
        File projectReportsDir = (File) project.property('reportsDir')
        File sourceSetReports = new File(projectReportsDir, sourceSet.name)

        return sourceSetReports
    }

    private void handleTestFailures() {
        String reportUrl = new ConsoleRenderer().asClickableFileUrl(new File(getReportsDir(), 'cucumber-html-reports/overview-failures.html'))
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
