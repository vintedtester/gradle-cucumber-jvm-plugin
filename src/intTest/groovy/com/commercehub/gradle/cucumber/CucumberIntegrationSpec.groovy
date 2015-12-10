package com.commercehub.gradle.cucumber

import nebula.test.IntegrationSpec
import nebula.test.functional.ExecutionResult

/**
 * Created by jgelais on 11/19/15.
 */
@SuppressWarnings('DuplicateStringLiteral')
class CucumberIntegrationSpec extends IntegrationSpec {
    def setup() {
        copyResources('teststeps/TestSteps.groovy', 'src/test/groovy/cucumber/steps/TestSteps.groovy')
        buildFile << '''
            apply plugin: 'groovy'
            apply plugin: 'cucumber-jvm'

            addCucumberSuite 'test'

            repositories {
                jcenter()
            }

            dependencies {
                compile 'org.codehaus.groovy:groovy-all:2.4.1'
                compile 'info.cukes:cucumber-java:1.2.2'
            }
        '''.stripIndent()
    }

    def testHappyPath() {
        given:
        copyResources('testfeatures/happypath.feature', 'src/test/resources/features/happypath.features')

        when:
        ExecutionResult result = runTasksSuccessfully('test')

        then:
        !result.wasUpToDate(':compileTestGroovy')
        result.wasExecuted(':test')
    }

    def testFailingBackgroundStep() {
        given:
        copyResources('testfeatures/failing-background-test.feature',
                'src/test/resources/features/failing-background-test.features')

        when:
        ExecutionResult result = runTasksSuccessfully('test')

        then:
        result.success
    }
}
