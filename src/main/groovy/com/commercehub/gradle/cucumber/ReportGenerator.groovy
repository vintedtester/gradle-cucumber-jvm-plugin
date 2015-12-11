package com.commercehub.gradle.cucumber

/**
 * Created by jgelais on 12/10/15.
 */
interface ReportGenerator {
    void generateReport(List<File> jsonReports)
}
