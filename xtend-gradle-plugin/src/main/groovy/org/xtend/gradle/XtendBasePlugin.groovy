package org.xtend.gradle;

import org.gradle.api.*
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.*
import org.gradle.plugins.ide.eclipse.EclipsePlugin
import org.gradle.plugins.ide.eclipse.model.EclipseModel
import org.xtend.gradle.tasks.XtendCompile
import org.xtend.gradle.tasks.XtendEclipseSettings
import org.xtend.gradle.tasks.XtendEnhance
import org.xtend.gradle.tasks.XtendExtension

class XtendBasePlugin implements Plugin<Project> {
	private XtendExtension xtend
	private Project project

	void apply(Project project) {
		this.project = project
		xtend = project.extensions.create("xtend", XtendExtension, project)

		project.plugins.apply(JavaBasePlugin)

		configureTaskConventions()
		configureEclipsePluginIfPresent()
	}

	private def configureTaskConventions() {
		project.tasks.withType(XtendCompile).all {configureCompileTaskConventions(it)}
		project.tasks.withType(XtendEnhance).all {configureEnhanceTaskConventions(it)}
		project.tasks.withType(XtendEclipseSettings).all {configureEclipseSettingsTaskConventions(it)}
	}

	private def configureEclipsePluginIfPresent() {
		project.afterEvaluate{
			def EclipseModel eclipse = project.extensions.findByType(EclipseModel)
			if (eclipse != null) {
				eclipse.getProject().buildCommand("org.eclipse.xtext.ui.shared.xtextBuilder")
				eclipse.getProject().natures("org.eclipse.xtext.ui.shared.xtextNature")
				def settingsTask = project.task(type: XtendEclipseSettings, "xtendEclipseSettings")
				project.tasks[EclipsePlugin.ECLIPSE_TASK_NAME].dependsOn(settingsTask)
			}
		}
	}

	private def configureCompileTaskConventions(XtendCompile compileTask) {
		def convention = compileTask.conventionMapping
		convention.encoding = {xtend.encoding}
		convention.useDaemon = {xtend.useDaemon}
		convention.daemonPort = {xtend.daemonPort}
	}

	private def configureEnhanceTaskConventions(XtendEnhance enhanceTask) {
		def convention = enhanceTask.conventionMapping
		convention.hideSyntheticVariables = {xtend.hideSyntheticVariables}
		convention.xtendAsPrimaryDebugSource = {xtend.xtendAsPrimaryDebugSource}
		convention.useDaemon = {xtend.useDaemon}
		convention.daemonPort = {xtend.daemonPort}
	}

	private def configureEclipseSettingsTaskConventions(XtendEclipseSettings settingsTask) {
		def convention = settingsTask.conventionMapping
		convention.sourceRelativeOutput = {xtend.sourceRelativeOutput}
		convention.hideSyntheticVariables = {xtend.hideSyntheticVariables}
		convention.xtendAsPrimaryDebugSource = {xtend.xtendAsPrimaryDebugSource}
	}
}