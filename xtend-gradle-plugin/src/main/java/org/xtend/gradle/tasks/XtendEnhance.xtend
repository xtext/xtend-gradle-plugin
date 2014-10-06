package org.xtend.gradle.tasks;

import java.io.File
import java.net.URLClassLoader
import java.util.List
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.classloader.FilteringClassLoader

import static extension org.xtend.gradle.GradleExtensions.*
import org.eclipse.xtend.lib.annotations.Accessors

class XtendEnhance extends DefaultTask {
	@InputFiles @Accessors FileCollection sourceFolders;
	@InputFiles @Accessors FileCollection xtendClasspath
	@Input @Accessors File classesFolder;
	@OutputDirectory @Accessors File targetFolder
	@Input @Accessors Boolean hideSyntheticVariables;
	@Input @Accessors Boolean xtendAsPrimaryDebugSource;
	@Input @Accessors Boolean fork
	@Input @Accessors Boolean useDaemon
	@Input @Accessors Integer daemonPort

	@TaskAction
	def enhance() {
		if(!getClassesFolder.exists) return;
		val existingSourceFolders = getSourceFolders.files.filter[exists]
		for (folder : existingSourceFolders) {
			if(!folder.directory) throw new GradleException('''«folder» is not a directory''')
		}

		val enhanceArguments = newArrayList(
			"-c",
			getClassesFolder.absolutePath,
			"-o",
			getTargetFolder.absolutePath
		)

		if (getHideSyntheticVariables) {
			enhanceArguments += #["-hideSynthetic"]
		}
		if (getXtendAsPrimaryDebugSource) {
			enhanceArguments += #["-xtendAsPrimary"]
		}
		enhanceArguments += #[
			existingSourceFolders.join(" ")
		]

		project.copy [
			from = getClassesFolder.absolutePath
			into = getTargetFolder.absolutePath
		]
		if (getFork) {
			if (getUseDaemon) {
				enhanceWithDaemon(enhanceArguments)
			} else {
				enhanceWithoutDaemon(enhanceArguments)
			}
		} else {
			enhanceNonForked(enhanceArguments)
		}
	}

	def enhanceNonForked(List<String> arguments) {
		System.setProperty("org.eclipse.emf.common.util.ReferenceClearingQueue", "false")
		val contextClassLoader = Thread.currentThread.contextClassLoader
		val classLoader = new URLClassLoader(getXtendClasspathWithoutLog4j.map[absoluteFile.toURI.toURL], loggingBridgeClassLoader)
		try {
			Thread.currentThread.contextClassLoader = classLoader
			val main = classLoader.loadClass("org.xtend.enhance.batch.Main")
			val mainMethod = main.getMethod("main", typeof(String[]))
			mainMethod.invoke(null, #[arguments as String[]])
		} finally {
			Thread.currentThread.contextClassLoader = contextClassLoader
		}
	}
	
	def getXtendClasspathWithoutLog4j() {
		getXtendClasspath.filter[!name.contains("log4j")]
	}

	def loggingBridgeClassLoader() {
		new FilteringClassLoader(class.classLoader) => [
			allowPackage("org.slf4j")
			allowPackage("org.apache.log4j")
		]
	}

	def enhanceWithDaemon(List<String> arguments) {
		val compiler = new XtendCompilerClient(getDaemonPort)
		compiler.requireServer(getXtendClasspath.asPath)
		if (!compiler.enhance(arguments)) {
			throw new GradleException("Installing debug information failed");
		}
	}

	def enhanceWithoutDaemon(List<String> arguments) {
		val result = project.javaexec [
			main = "org.xtend.enhance.batch.Main"
			classpath = getXtendClasspath
			setArgs(arguments)
		]
		if (result.exitValue !== 0) {
			throw new GradleException("Installing debug information failed");
		}
	}
}
