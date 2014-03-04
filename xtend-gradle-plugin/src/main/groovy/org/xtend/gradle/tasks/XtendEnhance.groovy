package org.xtend.gradle.tasks;
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.FileCollection
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction

class XtendEnhance extends DefaultTask {
	@InputFiles
	FileCollection xtendClasspath

	@InputFiles
	FileCollection sourceFolders;

	@Input
	File classesFolder;

	@OutputDirectory
	File targetFolder

	@Input
	boolean hideSyntheticVariables;

	@Input
	boolean xtendAsPrimaryDebugSource;

	/*
	 * TODO this breaks incremental compilation, because it changes the output of the Java task, 
	 * causing it to run again.
	 */
	@TaskAction
	def enhance() {
		if (!classesFolder.exists()) return
		for (folder in getSourceFolders().files) {
			if (!folder.isDirectory()) throw new GradleException("${folder} is not a directory")
		}

		def	command = [
			"java",
			"-cp",
			getXtendClasspath().asPath,
			"org.xtend.enhance.batch.Main",
			"-c",
			getClassesFolder().absolutePath,
			"-o",
			getTargetFolder().absolutePath
		]
		if (hideSyntheticVariables) {
			command += ["-hideSynthetic"]
		}
		if (xtendAsPrimaryDebugSource) {
			command += ["-xtendAsPrimary"]
		}
		command += [
			getSourceFolders().files.join(" ")
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
			throw new GradleException("Installing debug information failed");
		}
	}
}
