package com.commercehub.gradle.cucumber

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Shared
import spock.lang.Specification

/**
 * Created by jgelais on 7/8/15.
 */
@SuppressWarnings('DuplicateStringLiteral')
@SuppressWarnings('DuplicateMapLiteral')
class CucumberTaskSpec extends Specification {
    @Shared CucumberRunner cucumberRunnerMock

    def setupSpec() {
        cucumberRunnerMock = GroovyMock(CucumberRunner) {
            _ * run(_, _, _) >> true
        }
        CucumberTask.metaClass.createRunner = { -> cucumberRunnerMock }
    }

    def cleanupSpec() {
        CucumberTask.metaClass.createRunner = null
    }

    def testTaskExecution() {
        expect:
        Project project = ProjectBuilder.builder().build()
        project.apply(plugin: 'cucumber-jvm')
        project.addCucumberSuite('test')
        CucumberTask task = (CucumberTask) project.tasks.getByPath('test')
        task.executeTests()
    }

    def testTaskExecutionWithIdeaPlugin() {
        expect:
        Project project = ProjectBuilder.builder().build()
        project.apply(plugin: 'idea')
        project.apply(plugin: 'cucumber-jvm')
        project.addCucumberSuite('test')
        CucumberTask task = (CucumberTask) project.tasks.getByPath('test')
        task.executeTests()
    }
}
