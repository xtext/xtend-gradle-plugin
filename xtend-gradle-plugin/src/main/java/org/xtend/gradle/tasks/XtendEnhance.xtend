package org.xtend.gradle.tasks;

import java.io.File
import java.util.List
import org.eclipse.xtend.lib.annotations.Accessors
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

import static extension org.xtend.gradle.GradleExtensions.*

class XtendEnhance extends DefaultTask {
	@InputFiles @Accessors FileCollection sourceFolders;
	@InputFiles @Accessors FileCollection xtendClasspath
	@Input @Accessors File classesFolder;
	@OutputDirectory @Accessors File targetFolder
	@Input @Accessors Boolean hideSyntheticVariables;
	@Input @Accessors Boolean xtendAsPrimaryDebugSource;

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
		enhance(enhanceArguments)
	}

	def enhance(List<String> arguments) {
		System.setProperty("org.eclipse.emf.common.util.ReferenceClearingQueue", "false")
		val contextClassLoader = Thread.currentThread.contextClassLoader
		val classLoader = XtendExtension.getCompilerClassLoader(getXtendClasspath)
		try {
			Thread.currentThread.contextClassLoader = classLoader
			val main = classLoader.loadClass("org.xtend.enhance.batch.Main")
			val mainMethod = main.getMethod("main", typeof(String[]))
			mainMethod.invoke(null, #[arguments as String[]])
		} finally {
			Thread.currentThread.contextClassLoader = contextClassLoader
		}
	}

}
