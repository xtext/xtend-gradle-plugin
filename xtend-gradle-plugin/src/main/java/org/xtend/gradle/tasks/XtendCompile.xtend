package org.xtend.gradle.tasks

import java.io.File
import org.eclipse.xtend.lib.annotations.Accessors
import org.gradle.api.file.FileCollection
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.SkipWhenEmpty

class XtendCompile extends XtendTask {
	@InputFiles @Accessors SourceDirectorySet srcDirs
	@InputFiles @Accessors FileCollection classpath
	@InputFiles @Optional @Accessors String bootClasspath
	@OutputDirectory @Accessors File targetDir
	@Input @Accessors String encoding
	
	@InputFiles @SkipWhenEmpty
	def getXtendSources() {
		getSrcDirs.filter[path.endsWith(".xtend")]
	}
	
	@TaskAction
	def compile() {
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
		invoke("org.xtend.compiler.batch.Main", "compile", compilerArguments)
	}
}
