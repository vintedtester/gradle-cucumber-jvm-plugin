package com.commercehub.gradle.cucumber

class CommandArgumentsBuilder {

    private static final String PLUGIN = '--plugin'
    private static final String TILDE = '~'
    private static final String TAGS = '--tags '

    private List<String> args = []
    private CucumberRunnerOptions options

    CommandArgumentsBuilder(CucumberRunnerOptions options) {
        this.options = options
    }

    List<String> buildArguments(List<File> featureBatch, File resultsFile, File junitResultsFile) {
        applyGlueArguments()
        applyPluginArguments(resultsFile, junitResultsFile)
        applyDryRunArguments()
        applyMonochromeArguments()
        applyStrictArguments()
        applyTagsArguments()
        applySnippetArguments()
        featureBatch.each { featureFile ->
            args << featureFile.absolutePath
        }
        return args
    }

    private List<String> applyGlueArguments() {
        options.stepDefinitionRoots.each {
            args << '--glue'
            args << it
        }
    }

    private void applyPluginArguments(File resultsFile, File junitResultsFile) {
        args << PLUGIN
        args << 'pretty'
        args << PLUGIN
        args << "json:${resultsFile.absolutePath}"
        if (options.junitReport) {
            args << PLUGIN
            args << "junit:${junitResultsFile.absolutePath}"
        }
        if (!options.plugins.empty) {
            options.plugins.each {
                args << PLUGIN
                args << it
            }
        }
    }

    private void applyDryRunArguments() {
        if (options.isDryRun) {
            args << '--dry-run'
        }
    }

    private void applyMonochromeArguments() {
        if (options.isMonochrome) {
            args << '--monochrome'
        }
    }

    private void applyStrictArguments() {
        if (options.isStrict) {
            args << '--strict'
        }
    }

    protected void applyTagsArguments() {
        if (!options.tags.isEmpty()) {
            applyTagsToCheck()
            applyTagsToIgnore()
        }
    }

    private void applyTagsToCheck() {
        def tagsToCheck = ''
        def hasTags = false
        options.tags.each {
            if (!it.contains(TILDE)) {
                tagsToCheck += it + ' or '
                hasTags = true
            }
        }
        if (hasTags) {
            args << TAGS
            args << tagsToCheck[0..-5]
        }
    }

    private void applyTagsToIgnore() {
        options.tags.each {
            if (it.contains(TILDE)) {
                args << TAGS
                args << it.replaceFirst(TILDE, 'not ')
            }
        }
    }

    private void applySnippetArguments() {
        args << '--snippets'
        args << options.snippets
    }

}
