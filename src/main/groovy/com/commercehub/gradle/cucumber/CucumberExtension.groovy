package com.commercehub.gradle.cucumber

import org.gradle.api.Project

/**
 * The gradle cucumber-jvm plugin provides the ability to run cucumber acceptance tests directly from a gradle build.
 */
class CucumberExtension {
    /**
     * A list of tags to identify scenarios to run. Defaults to an empty list.
     */
    List<String> tags = []

    /**
     * Maximum number of forked Java processes to start to run tests in parallel. Defaults to `1`.
     */
    int maxParallelForks = 1

    /**
     * A list of root packages to scan the classpath for glue code. Defaults to `['cucumber.steps', 'cucumber.hooks']`.
     */
    List<String> stepDefinitionRoots = ['cucumber.steps', 'cucumber.hooks']

    /**
     * A list of root packages to scan the resources folders on the classpath for feature files. Defaults to `['features']`.
     */
    List<String> featureRoots = ['features']

    /**
     * A list of cucumber plugins passed for execution. Defaults to an empty list.
     */
    List<String> plugins = []

    /**
     * A boolean value indicating whether glue code execution should be skipped. Defaults to `false`.
     */
    boolean isDryRun = false

    /**
     * A boolean value indicating whether terminal output should be without colours. Defaults to `false`.
     */
    boolean isMonochrome = false

    /**
     * A boolean value indicating whether scenarios should be evaluated strictly. Defaults to `false`.
     */
    boolean isStrict = false

    /**
     * Indicator to cucumber on what style to use for generated step examples. Valid values include `camelcase`, `underscore`. Defaults to `camelcase`.
     */
    String snippets = 'camelcase'

    /**
     * Property to enable/disable JUnit reporting. Defaults to `false`.
     */
    boolean junitReport = false

    /**
     * Property to cause or prevent build failure when cucumber tests fail. Defaults to `false`.
     */
    boolean ignoreFailures = false

    private final Project project

    private final CucumberPlugin plugin

    CucumberExtension(Project project, CucumberPlugin plugin) {
        this.project = project
        this.plugin = plugin
    }

    def cucumber(Closure closure) {
        closure.setDelegate this
        closure.call()
    }

    void setMaxParallelForks(int maxParallelForks) {
        if (maxParallelForks < 1) {
            throw new IllegalArgumentException('maxParallelForks most be a positive integer. ' +
                    "You supplied: $maxParallelForks")
        }
        this.maxParallelForks = maxParallelForks
    }

    @SuppressWarnings('DuplicateStringLiteral')
    void setSnippets(String snippets) {
        if (!['camelcase', 'underscore', null].contains(snippets)) {
            throw new IllegalArgumentException('Legal values for snippets include [camelcase, underscore]. ' +
                    "You provided: ${snippets}")
        }
        this.snippets = snippets
    }

    /**
     * Register a new Cucumber suite in the project.
     *
     * @param name The name of the suite, e.g. "cucumberTest".
     */
    void suite(String name) {
        plugin.addSuite(name, project)
    }
}
