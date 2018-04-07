package com.commercehub.gradle.cucumber

import net.masterthought.cucumber.ReportBuilder
import net.masterthought.cucumber.Configuration

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Created by jgelais on 12/10/15.
 */
class MasterThoughtReportGenerator implements ReportGenerator {

    CucumberTask parentTask
    Configuration configuration

    MasterThoughtReportGenerator(CucumberTask parentTask, Configuration configuration) {
        this.parentTask = parentTask
        this.configuration = configuration
    }

    @Override
    void generateReport(List<File> jsonReports) {
        List<String> jsonReportFiles = []
        jsonReports.each {
            jsonReportFiles << it.absolutePath
        }

        jsonFileUriToRelativePaths = jsonReportFiles

        ReportBuilder reportBuilder = new ReportBuilder(jsonReportFiles, configuration)
        reportBuilder.generateReports()

    }

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
                String featureRoot = "src/${parentTask.sourceSet.name}/resources/"
                def lastIndexOfFeatureRoot = absolutePath.lastIndexOf(featureRoot)
                if (lastIndexOfFeatureRoot != -1) {
                    relativePath = absolutePath[absolutePath.lastIndexOf(featureRoot) + featureRoot.length()..-1]
                    content = content.replace(absolutePath, relativePath)
                    thisFile.write(content)
                }
            }
        }
    }
}
