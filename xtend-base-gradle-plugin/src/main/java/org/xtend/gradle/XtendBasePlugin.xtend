package org.xtend.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.plugins.ide.eclipse.EclipsePlugin
import org.gradle.plugins.ide.eclipse.model.EclipseModel
import org.xtend.gradle.tasks.XtendCompile
import org.xtend.gradle.tasks.XtendEclipseSettings
import org.xtend.gradle.tasks.XtendEnhance
import org.xtend.gradle.tasks.XtendExtension

import static extension org.xtend.gradle.GradleExtensions.*
import org.gradle.api.artifacts.Configuration

class XtendBasePlugin implements Plugin<Project> {
	Project project
	XtendExtension xtend
	Configuration xtendCompileOnlyConfiguration

	override apply(Project project) {
		this.project = project
		xtend = project.extensions.create("xtend", XtendExtension, project)
		xtendCompileOnlyConfiguration = project.configurations.create("xtendCompileOnly")
		project.plugins.<JavaBasePlugin>apply(JavaBasePlugin) //Xtend Bug 435429
		configureTaskConventions
		configureEclipsePluginIfPresent
	}

	private def configureTaskConventions() {
		project.tasks.withType(XtendCompile).all[configureConventions]
		project.tasks.withType(XtendEnhance).all[configureConventions]
		project.tasks.withType(XtendEclipseSettings).all[configureConventions]
	}

	private def configureConventions(XtendCompile compileTask) {
		compileTask.conventionMapping(
			#{
				"encoding" -> [|xtend.encoding],
				"fork" -> [|xtend.fork],
				"useDaemon" -> [|xtend.useDaemon],
				"daemonPort" -> [|xtend.daemonPort],
				"xtendClasspath" -> [|xtend.inferXtendClasspath(compileTask.getClasspath())]
			}
		)
	}

	private def configureConventions(XtendEnhance enhanceTask) {
		enhanceTask.conventionMapping(
			#{
				"hideSyntheticVariables" -> [|xtend.hideSyntheticVariables],
				"xtendAsPrimaryDebugSource" -> [|xtend.xtendAsPrimaryDebugSource],
				"fork" -> [|xtend.fork],
				"useDaemon" -> [|xtend.useDaemon],
				"daemonPort" -> [|xtend.daemonPort]
			})
	}

	private def configureConventions(XtendEclipseSettings settingsTask) {
		settingsTask.conventionMapping(
			#{
				"hideSyntheticVariables" -> [|xtend.hideSyntheticVariables],
				"xtendAsPrimaryDebugSource" -> [|xtend.xtendAsPrimaryDebugSource]
			})
	}

	private def configureEclipsePluginIfPresent() {
		project.afterEvaluate [
			val eclipse = project.extensions.findByType(EclipseModel)
			if (eclipse !== null) {
				eclipse.project.buildCommand("org.eclipse.xtext.ui.shared.xtextBuilder")
				eclipse.project.natures("org.eclipse.xtext.ui.shared.xtextNature")
				eclipse.classpath.plusConfigurations += xtendCompileOnlyConfiguration
				val settingsTask = project.tasks.create("xtendEclipseSettings", XtendEclipseSettings)
				project.tasks.getAt(EclipsePlugin.ECLIPSE_TASK_NAME).dependsOn(settingsTask)
			}
		]
	}
}
