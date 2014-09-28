package org.xtend.gradle.tasks

import java.util.regex.Pattern
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.FileCollection

import static extension org.xtend.gradle.GradleExtensions.*
import org.eclipse.xtend.lib.annotations.Accessors

class XtendExtension {

	Project project
	@Accessors String encoding = "UTF-8"
	@Accessors boolean hideSyntheticVariables = true
	@Accessors boolean xtendAsPrimaryDebugSource = false
	@Accessors boolean fork = false
	@Accessors boolean useDaemon = false
	@Accessors int daemonPort = 3032

	new(Project project) {
		this.project = project
	}

	private def getPluginVersion() {
		this.class.package.implementationVersion
	}

	def FileCollection inferXtendClasspath(FileCollection classpath) {
		val pattern = Pattern.compile("org.eclipse.(xtend|xtext.xbase).(core|lib|lib.slim)-(\\d.*?).jar")
		for (file : classpath) {
			val matcher = pattern.matcher(file.name)
			if (matcher.matches) {
				val xtendVersion = matcher.group(3)
				val dependencies = #[
					project.dependencies.externalModule(
						'''org.eclipse.xtend:org.eclipse.xtend.core:«xtendVersion»''') [
						force = true
						exclude(#{'group' -> 'asm'})
					],
					project.dependencies.externalModule('''org.xtend:xtend-gradle-lib:«pluginVersion»''') [
						force = true
					],
					project.dependencies.externalModule('com.google.inject:guice:4.0-beta4')[]
				]
				return project.configurations.detachedConfiguration(dependencies)
			}
		}
		throw new GradleException(
			'''Could not infer Xtend classpath, because no Xtend jar was found on the «classpath» classpath''')
	}
}
