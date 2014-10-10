package org.xtend.gradle.tasks

import java.io.File
import java.util.List
import org.eclipse.xtend.lib.annotations.Accessors
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.FileCollection
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.Optional

class XtendCompile extends DefaultTask {
	@InputFiles @Accessors SourceDirectorySet srcDirs
	@InputFiles @Accessors FileCollection classpath
	@InputFiles @Optional @Accessors String bootClasspath
	@OutputDirectory @Accessors File targetDir
	@Input @Accessors String encoding
	@InputFiles @Accessors FileCollection xtendClasspath

	@TaskAction
	def compile() {
		if (getSrcDirs.isEmpty) {
			logger.info("Nothing to compile")
			return
		}
		val sourcePath = getSrcDirs.srcDirTrees.filter[dir.exists].map[dir.absolutePath].join(File.pathSeparator)
		val compilerArguments = newArrayList(
			"-cp",
			getClasspath.asPath,
			"-d",
			project.file(getTargetDir).absolutePath,
			"-encoding",
			getEncoding,
			"-td",
			new File(project.buildDir, "xtend-temp").absolutePath
			)
		if (getBootClasspath !== null) {
			compilerArguments += #[
				"-bootClasspath",
				getBootClasspath
			]
		}
		compilerArguments.add(sourcePath)
		compile(compilerArguments)
	}

	def compile(List<String> arguments) {
		System.setProperty("org.eclipse.emf.common.util.ReferenceClearingQueue", "false")
		val contextClassLoader = Thread.currentThread.contextClassLoader
		val classLoader = XtendExtension.getCompilerClassLoader(getXtendClasspath)
		try {
			Thread.currentThread.contextClassLoader = classLoader
			val main = classLoader.loadClass("org.xtend.compiler.batch.Main")
			val compileMethod = main.getMethod("compile", typeof(String[]))
			val success = compileMethod.invoke(null, #[arguments as String[]]) as Boolean
			if (!success) {
				throw new GradleException("Xtend Compilation failed");
			}
		} finally {
			Thread.currentThread.contextClassLoader = contextClassLoader
		}
	}
}
