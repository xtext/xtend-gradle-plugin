package org.xtend.gradle.tasks;

import de.oehme.xtend.contrib.Property
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

class XtendEnhance extends DefaultTask {
	@InputFiles @Property FileCollection sourceFolders;
	@InputFiles @Property FileCollection xtendClasspath
	@Input @Property File classesFolder;
	@OutputDirectory @Property File targetFolder
	@Input @Property Boolean hideSyntheticVariables;
	@Input @Property Boolean xtendAsPrimaryDebugSource;
	@Input @Property Boolean fork
	@Input @Property Boolean useDaemon
	@Input @Property Integer daemonPort

	@TaskAction
	def enhance() {
		if(!getClassesFolder.exists) return;
		for (folder : getSourceFolders.files) {
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
			getSourceFolders.files.join(" ")
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
