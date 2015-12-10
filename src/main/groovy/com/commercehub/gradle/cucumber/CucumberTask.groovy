package com.commercehub.gradle.cucumber

import net.masterthought.cucumber.ReportBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction
import org.gradle.logging.ConsoleRenderer
import org.gradle.logging.ProgressLoggerFactory

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Created by jgelais on 6/11/15.
 * rholman 12/12/15 - added logic to shorten uri in json files to relative path.
 */
class CucumberTask extends DefaultTask implements CucumberRunnerOptions {
    public static final String CUCUMBER_REPORTS_DIR = 'cucumber'
    public static final String CUCUMBER_EXTENSION_NAME = 'cucumber'

    SourceSet sourceSet
    private final CucumberExtension extension = project.extensions[CUCUMBER_EXTENSION_NAME]

    List<String> tags = null
    Integer maxParallelForks = null
    List<String> featureRoots = null
    List<String> stepDefinitionRoots = null
    Boolean isDryRun = null
    Boolean isMonochrome = null
    Boolean isStrict = null
    String snippets = null
    Map<String, String> systemProperties = [:]
    boolean junitReport = null

    @TaskAction
    void runTests() {
        ProgressLoggerFactory progressLoggerFactory = services.get(ProgressLoggerFactory)
        CucumberRunner runner = new CucumberRunner(this, new CucumberTestResultCounter(progressLoggerFactory, logger),
                systemProperties)
        boolean isPassing = runner.run(sourceSet, resultsDir, reportsDir)
        generateReport()

        if (!isPassing) {
            handleTestFailures()
        }
    }

    @SuppressWarnings('ConfusingMethodName')
    def sourceSet(SourceSet sourceSet) {
        this.sourceSet = sourceSet
    }

    private void generateReport() {
        List<String> jsonReportFiles = []
        resultsDir.eachFileMatch(~/^.*\.json$/) {
            jsonReportFiles << it.absolutePath
        }

        setJsonFileUriToRelativePaths(jsonReportFiles)

        ReportBuilder reportBuilder = new ReportBuilder(
                jsonReportFiles,
                reportsDir,
                '',
                '',
                project.name,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                true
        )
        reportBuilder.generateReports()
    }

    @SuppressWarnings('UnnecessarySubstring')
    /**
     *  shortens uri in json files to relative path.
     *  @param List<String> jsonFilesList - list of fully qualified paths to the JSON files to be modified.
     */
    private void setJsonFileUriToRelativePaths(List<String> jsonFileList) {
        String absolutePath
        String relativePath
        Pattern pattern = Pattern.compile('\\s.*"uri":(?: |)"(.*?)".*\\s')
        jsonFileList.each { String fileName ->
            File thisFile = new File(fileName)
            String content = thisFile.text
            if (content.contains('"uri":')) {
                Matcher matcher = pattern.matcher(content)
                absolutePath = matcher[0][1]
                String featureRoot = "src/${sourceSet.name}/resources/"
                relativePath = absolutePath.substring(absolutePath.lastIndexOf(featureRoot) + featureRoot.length())
                content = content.replace(absolutePath, relativePath)
                thisFile.write(content)
            }
        }
    }

    File getResultsDir() {
        File projectResultsDir = (File) project.property('testResultsDir')
        File cucumberResults = new File(projectResultsDir, CUCUMBER_REPORTS_DIR)
        File sourceSetResults = new File(cucumberResults, sourceSet.name)
        sourceSetResults.mkdirs()

        return sourceSetResults
    }

    File getReportsDir() {
        File projectReportsDir = (File) project.property('testReportDir')
        File cucumberReports = new File(projectReportsDir, CUCUMBER_REPORTS_DIR)
        File sourceSetReports = new File(cucumberReports, sourceSet.name)

        return sourceSetReports
    }

    private void handleTestFailures() {
        String reportUrl = new ConsoleRenderer().asClickableFileUrl(new File(reportsDir, 'feature-overview.html'))
        String message = "There were failing tests. See the report at: $reportUrl"

        throw new GradleException(message)
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

    void systemProperty(String property, String value) {
        systemProperties[property] = value
    }

    boolean getJunitReport() {
        return junitReport ?: extension.junitReport
    }
}
