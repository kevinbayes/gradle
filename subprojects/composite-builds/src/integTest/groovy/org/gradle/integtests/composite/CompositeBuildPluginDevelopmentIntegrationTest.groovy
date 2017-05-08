/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.integtests.composite

import org.gradle.integtests.fixtures.build.BuildTestFile

/**
 * Tests for plugin development scenarios within a composite build.
 */
class CompositeBuildPluginDevelopmentIntegrationTest extends AbstractCompositeBuildIntegrationTest {
    BuildTestFile buildB
    BuildTestFile pluginBuild

    def setup() {
        buildB = singleProjectBuild("buildB") {
            buildFile << """
                apply plugin: 'java'
                version "2.0"
"""
        }

        pluginBuild = pluginProjectBuild("pluginC")
    }

    def "can co-develop plugin and consumer with plugin as included build"() {
        given:
        applyPlugin(buildA)

        includeBuild pluginBuild

        when:
        execute(buildA, "tasks")

        then:
        outputContains("taskFromPluginC")
    }

    def "can co-develop plugin and consumer with both plugin and consumer as included builds"() {
        given:
        applyPlugin(buildB)

        buildA.buildFile << """
            dependencies {
                compile "org.test:buildB:1.0"
            }
"""

        includeBuild buildB, """
            substitute module("org.test:buildB") with project(":")
"""
        includeBuild pluginBuild

        when:
        execute(buildA, "assemble")

        then:
        executed ":pluginC:jar", ":buildB:jar", ":jar"
    }

    def "can co-develop plugin and consumer where plugin uses previous version of itself to build"() {
        given:
        // Ensure that 'plugin' is published with older version
        mavenRepo.module("org.test", "pluginC", "0.1").publish()

        pluginBuild.buildFile << """
            buildscript {
                repositories {
                    repositories {
                        maven { url "${mavenRepo.uri}" }
                    }
                }
                dependencies {
                    classpath 'org.test:pluginC:0.1'
                }
            }
"""

        applyPlugin(buildA)

        includeBuild pluginBuild, """
            // Only substitute version 1.0 with project dependency. This allows this project to build with the published dependency.
            substitute module("org.test:pluginC:1.0") with project(":")
"""

        when:
        execute(buildA, "tasks")

        then:
        outputContains("taskFromPluginC")
    }

    def "detects dependency cycle between included builds required for buildscript classpath"() {
        given:
        def buildD = singleProjectBuild("buildD") {
            buildFile << """
                apply plugin: 'java'
                version "2.0"
"""
        }

        dependency pluginBuild, "org.test:buildB:1.0"
        dependency buildB, "org.test:buildD:1.0"
        dependency buildD, "org.test:buildB:1.0"

        applyPlugin(buildA)

        includeBuild pluginBuild
        includeBuild buildB
        includeBuild buildD

        when:
        fails(buildA, "tasks")

        then:
        failure
            .assertHasDescription("Failed to build artifacts for build 'pluginC'")
            .assertHasCause("Failed to build artifacts for build 'buildB'")
            .assertHasCause("Failed to build artifacts for build 'buildD'")
            .assertHasCause("Could not determine the dependencies of task ':buildD:compileJava'.")
            .assertHasCause("Included build dependency cycle: build 'buildB' -> build 'buildD' -> build 'buildB'")
    }

    def applyPlugin(BuildTestFile build) {
        build.buildFile << """
            buildscript {
                dependencies {
                    classpath 'org.test:pluginC:1.0'
                }
            }
            apply plugin: 'org.test.plugin.pluginC'
"""
    }

    def pluginProjectBuild(String name) {
        def className = name.capitalize()
        singleProjectBuild(name) {
            buildFile << """
apply plugin: 'java-gradle-plugin'

gradlePlugin {
    plugins {
        ${name} {
            id = "org.test.plugin.$name"
            implementationClass = "org.test.$className"
        }
    }
}
"""
            file("src/main/java/org/test/${className}.java") << """
package org.test;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;

public class ${className} implements Plugin<Project> {
    public void apply(Project project) {
        Task task = project.task("taskFrom${className}");
        task.setGroup("Plugin");
    }
}
"""
        }

    }


}
