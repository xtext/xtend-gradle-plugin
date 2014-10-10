package org.xtend.gradle.tasks

import java.net.URLClassLoader
import java.util.regex.Pattern
import org.eclipse.xtend.lib.annotations.Accessors
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.internal.classloader.FilteringClassLoader

import static extension org.xtend.gradle.GradleExtensions.*

class XtendExtension {

	static val currentCompilerClassLoader = new ThreadLocal<URLClassLoader>() {
		override protected initialValue() {
			null
		}
	}
	
	Project project
	@Accessors String encoding = "UTF-8"
	@Accessors boolean hideSyntheticVariables = true
	@Accessors boolean xtendAsPrimaryDebugSource = false

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

	static package def ClassLoader getCompilerClassLoader(FileCollection classpath) {
		val classPathWithoutLog4j = classpath.filter[!name.contains("log4j")]
		val urls = classPathWithoutLog4j.map[absoluteFile.toURI.toURL].toList
		val currentClassLoader = currentCompilerClassLoader.get
		if (currentClassLoader !== null && currentClassLoader.URLs.toList == urls) {
			return currentClassLoader
		} else {
			val newClassLoader = new URLClassLoader(urls, loggingBridgeClassLoader)
			currentCompilerClassLoader.set(newClassLoader)
			return newClassLoader
		}
	}

	static private def loggingBridgeClassLoader() {
		new FilteringClassLoader(XtendExtension.classLoader) => [
			allowPackage("org.slf4j")
			allowPackage("org.apache.log4j")
		]
	}
}
