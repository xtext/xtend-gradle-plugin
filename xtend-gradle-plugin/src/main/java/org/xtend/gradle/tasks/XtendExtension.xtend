package org.xtend.gradle.tasks

import de.oehme.xtend.contrib.Property
import java.util.regex.Pattern
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.FileCollection

import static extension org.xtend.gradle.GradleExtensions.*

class XtendExtension {

	Project project
	@Property String encoding = "UTF-8"
	@Property boolean hideSyntheticVariables = true
	@Property boolean xtendAsPrimaryDebugSource = false
	@Property boolean fork = false
	@Property boolean useDaemon = false
	@Property int daemonPort = 3032

	new(Project project) {
		this.project = project
	}

	private def getPluginVersion() {
		this.class.package.implementationVersion
	}

	def FileCollection inferXtendClasspath(FileCollection classpath) {
		val pattern = Pattern.compile("org.eclipse.xtend.(core|lib)-(\\d.*?).jar")
		for (file : classpath) {
			val matcher = pattern.matcher(file.name)
			if (matcher.matches) {
				val xtendVersion = matcher.group(2)
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
