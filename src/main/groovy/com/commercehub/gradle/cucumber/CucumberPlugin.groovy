package com.commercehub.gradle.cucumber

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.plugins.ide.idea.IdeaPlugin

/**
 * This is the main plugin file. Put a description of your plugin here.
 */
class CucumberPlugin implements Plugin<Project> {

    public static final String DEFAULT_PARENT_SOURCESET = 'main'

    void apply(Project project) {
        project.plugins.apply(JavaPlugin)

        project.extensions.create('cucumber', CucumberExtension, project, this)
        project.metaClass.addCucumberSuite = { String sourceSetName ->
            addSuite(sourceSetName, project)
        }
    }

    def addSuite(String sourceSetName, Project project) {
        SourceSet cucumberSuiteSourceSet =
                project.sourceSets.findByName(sourceSetName) ?: project.sourceSets.create(sourceSetName) {
                    compileClasspath += project.sourceSets[DEFAULT_PARENT_SOURCESET].output
                    compileClasspath += project.sourceSets[DEFAULT_PARENT_SOURCESET].compileClasspath
                    runtimeClasspath = it.output + it.compileClasspath
                }
        CucumberTask task
        try {
            task = project.tasks.create(sourceSetName, CucumberTask)
        } catch (GradleException ignored) {
            task = project.tasks.replace(sourceSetName, CucumberTask)
        }
        task.dependsOn cucumberSuiteSourceSet.classesTaskName
        task.sourceSet(cucumberSuiteSourceSet)

        // configure source set in intellij if plugin is applied
        project.plugins.withType(IdeaPlugin) {
            project.idea.module {
                testSourceDirs += cucumberSuiteSourceSet.allSource.srcDirs
                testSourceDirs += cucumberSuiteSourceSet.resources.srcDirs
            }
        }

        return cucumberSuiteSourceSet
    }
}
