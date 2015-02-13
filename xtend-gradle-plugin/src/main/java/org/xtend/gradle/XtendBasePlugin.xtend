package org.xtend.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.plugins.ide.eclipse.EclipsePlugin
import org.gradle.plugins.ide.eclipse.model.EclipseModel
import org.xtend.gradle.tasks.XtendEclipseSettings
import org.xtend.gradle.tasks.XtendRuntime

class XtendBasePlugin implements Plugin<Project> {
	Project project
	Configuration xtendCompileOnlyConfiguration

	override apply(Project project) {
		this.project = project
		project.extensions.create("xtendRuntime", XtendRuntime, project)
		xtendCompileOnlyConfiguration = project.configurations.create("xtendCompileOnly")
		project.plugins.<JavaBasePlugin>apply(JavaBasePlugin) //Xtend Bug 435429
		configureEclipsePluginIfPresent
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
