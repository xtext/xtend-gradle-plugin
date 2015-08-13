package org.xtend.gradle.tasks

import java.net.URLClassLoader
import java.util.regex.Pattern
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.invocation.Gradle

import static extension org.xtend.gradle.GradleExtensions.*
import java.util.concurrent.Callable

class XtendRuntime {
	static val XTEND_LIB_PATTERN = Pattern.compile("org.eclipse.(xtend|xtext.xbase).(core|lib|lib.slim|lib.gwt)-(\\d.*?).jar")

	Project project

	new(Project project) {
		this.project = project
	}

	def FileCollection inferXtendClasspath(FileCollection classpath) {
		project.files([|
			for (file : classpath) {
					val matcher = XTEND_LIB_PATTERN.matcher(file.name)
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
		] as Callable<FileCollection>)
		.builtBy(classpath.buildDependencies)
	}

	private def getPluginVersion() {
		this.class.package.implementationVersion
	}

	private static val currentCompilerClassLoader = new ThreadLocal<URLClassLoader>() {
		override protected initialValue() {
			null
		}
	}

	static package def ClassLoader getCompilerClassLoader(FileCollection classpath) {
		val classPathWithoutLog4j = classpath.filter[!name.contains("log4j")]
		val urls = classPathWithoutLog4j.map[absoluteFile.toURI.toURL].toList
		val currentClassLoader = currentCompilerClassLoader.get
		if (currentClassLoader !== null && currentClassLoader.URLs.toList == urls) {
			return currentClassLoader
		} else {
			val newClassLoader = new URLClassLoader(urls, Gradle.classLoader)
			currentCompilerClassLoader.set(newClassLoader)
			return newClassLoader
		}
	}
}
