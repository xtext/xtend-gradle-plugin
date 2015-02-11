package org.xtend.gradle.tasks

import java.io.File
import java.util.List
import org.eclipse.xtend.lib.annotations.Accessors
import org.gradle.api.GradleException
import org.gradle.api.file.FileCollection
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.TaskAction

@Accessors
class XtendCompile extends AbstractCompile {
	@InputFiles SourceDirectorySet srcDirs
	@InputFiles @Optional String bootClasspath
	@InputFiles FileCollection xtendClasspath
	@Input File classesDir;
	@Nested XtendCompileOptions options = new XtendCompileOptions

	@InputFiles @SkipWhenEmpty
	def getXtendSources() {
		getSrcDirs.filter[path.endsWith(".xtend")]
	}

	@TaskAction
	override compile() {
		val sourcePath = getSrcDirs.srcDirTrees.filter[dir.exists].map[dir.absolutePath].join(File.pathSeparator)
		val compilerArguments = newArrayList(
			"-cp",
			classpath.asPath,
			"-d",
			project.file(destinationDir).absolutePath,
			"-encoding",
			getOptions.encoding,
			"-td",
			new File(project.buildDir, "xtend-temp").absolutePath
		)
		if (getBootClasspath !== null) {
			compilerArguments += #[
				"-bootClasspath",
				getBootClasspath
			]
		}
		compilerArguments += #["-javaSourceVersion", sourceCompatibility]
		if (!getOptions.addSuppressWarnings) {
			compilerArguments += #["-noSuppressWarningsAnnotation"]
		}
		getOptions.generatedAnnotation => [
			if (active) {
				compilerArguments += #["-generateGeneratedAnnotation"]
				if (includeDate) {
					compilerArguments += #["-includeDateInGeneratedAnnnotation"]
				}
				if (comment != null) {
					compilerArguments += #["-generateAnnotationComment", comment]
				}
			}
		]
		compilerArguments.add(sourcePath)
		invoke("org.xtend.compiler.batch.Main", "compile", compilerArguments)
	}

	def enhance() {
		val existingSourceFolders = getSrcDirs.srcDirTrees.map[dir].filter[exists]
		for (folder : existingSourceFolders) {
			if(!folder.directory) throw new GradleException('''«folder» is not a directory''')
		}

		val enhanceArguments = newArrayList(
			"-c",
			getClassesDir.absolutePath,
			"-o",
			destinationDir.absolutePath
		)

		if (getOptions.hideSyntheticVariables) {
			enhanceArguments += #["-hideSynthetic"]
		}
		if (getOptions.xtendAsPrimaryDebugSource) {
			enhanceArguments += #["-xtendAsPrimary"]
		}
		enhanceArguments += existingSourceFolders.map[path]
		invoke("org.xtend.enhance.batch.Main", "enhance", enhanceArguments)
	}

	private def invoke(String className, String methodName, List<String> arguments) {
		System.setProperty("org.eclipse.emf.common.util.ReferenceClearingQueue", "false")
		val contextClassLoader = Thread.currentThread.contextClassLoader
		val classLoader = XtendRuntime.getCompilerClassLoader(getXtendClasspath)
		try {
			Thread.currentThread.contextClassLoader = classLoader
			val main = classLoader.loadClass(className)
			val method = main.getMethod(methodName, typeof(String[]))
			val success = method.invoke(null, #[arguments as String[]]) as Boolean
			if (!success) {
				throw new GradleException('''Xtend «methodName» failed''');
			}
		} finally {
			Thread.currentThread.contextClassLoader = contextClassLoader
		}
	}
}
