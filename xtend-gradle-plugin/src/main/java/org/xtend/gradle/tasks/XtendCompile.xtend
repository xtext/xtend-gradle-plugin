package org.xtend.gradle.tasks

import de.oehme.xtend.contrib.Property
import java.io.File
import java.util.List
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.FileCollection
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

import static extension org.xtend.gradle.GradleExtensions.*

class XtendCompile extends DefaultTask {
	@InputFiles @Property SourceDirectorySet srcDirs
	@InputFiles @Property FileCollection classpath
	@OutputDirectory @Property File targetDir
	@Input @Property String encoding
	@InputFiles @Property FileCollection xtendClasspath
	@Input @Property Boolean useDaemon
	@Input @Property Integer daemonPort

	@TaskAction
	def compile() {
		val sourcePath = getSrcDirs.srcDirTrees.map[dir.absolutePath].join(File.pathSeparator)
		val compilerArguments = #[
			"-cp",
			getClasspath.asPath,
			"-d",
			project.file(getTargetDir).absolutePath,
			"-encoding",
			getEncoding,
			"-td",
			new File(project.buildDir, "xtend-temp").absolutePath,
			sourcePath
		]
		if (getUseDaemon) {
			compileWithDaemon(compilerArguments)
		} else {
			compileWithoutDaemon(compilerArguments)
		}
	}

	def compileWithDaemon(List<String> arguments) {
		val compiler = new XtendCompilerClient(getDaemonPort)
		compiler.requireServer(getXtendClasspath.asPath)
		if (!compiler.compile(arguments)) {
			throw new GradleException("Xtend Compilation failed");
		}
	}

	def compileWithoutDaemon(List<String> arguments) {
		val result = project.javaexec [
			main = "org.xtend.compiler.batch.Main"
			it.classpath = getXtendClasspath //Bug, shouldn't it take precedence?
			setArgs(arguments)
		]
		if (result.exitValue != 0) {
			throw new GradleException("Xtend Compilation failed");
		}
	}
}
