package org.xtend.gradle.tasks;
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.FileCollection
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

class XtendEnhance extends DefaultTask {
	@InputFiles
	FileCollection sourceFolders;
	@OutputDirectory
	File classesFolder;
	@Input
	boolean hideSyntheticVariables;
	@Input
	boolean xtendAsPrimaryDebugSource;

	@InputFiles
	FileCollection xtendClasspath

	@TaskAction
	def enhance() {
		for (folder in getSourceFolders().files) {
			if (!folder.isDirectory()) throw new GradleException("${folder} is not a directory")
		}
		
		def	command = [
			"java",
			"-cp",
			getXtendClasspath().asPath,
			"org.xtend.enhance.batch.Main",
			"-d",
			getClassesFolder().absolutePath
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
