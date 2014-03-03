package org.xtend.gradle.tasks;

import java.lang.ProcessBuilder.Redirect;

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.FileCollection
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

class XtendCompile extends DefaultTask {
	@InputFiles
	SourceDirectorySet srcDirs

	@InputFiles
	FileCollection classpath

	@OutputDirectory
	File targetDir

	@Input
	String encoding

	@InputFiles
	FileCollection xtendClasspath

	//TODO more options

	//TODO allow using a daemon instead of forking a new process each time
	@TaskAction
	def compile() {
		def sourcePath = getSrcDirs().srcDirTrees.collect{it.dir.absolutePath}.join(File.pathSeparator)
		def	command = [
			"java",
			"-cp",
			getXtendClasspath().asPath,
			"org.eclipse.xtend.core.compiler.batch.Main",
			"-cp",
			getClasspath().asPath,
			"-d",
			getTargetDir().absolutePath,
			"-encoding",
			getEncoding(),
			"-td",
			new File(project.getBuildDir(), "xtend-temp").absolutePath,
			sourcePath
		]
		def pb = new ProcessBuilder(command)
		pb.redirectErrorStream(true)
		def process = pb.start()
		def input = new BufferedReader(new InputStreamReader(process.getInputStream()))
		def line
		while ((line = input.readLine()) != null) {
			println(line)
		}
		def exitCode = process.waitFor()
		if (exitCode != 0) {
			throw new GradleException("Xtend Compilation failed");
		}
	}
}