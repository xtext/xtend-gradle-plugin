package org.xtend.gradle;

import java.util.regex.Pattern;

import javax.inject.Inject;

import org.gradle.api.*;
import org.gradle.api.artifacts.Dependency
import org.gradle.api.file.DirectoryTree;
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTreeElement;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.file.DefaultSourceDirectorySet
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.internal.tasks.DefaultSourceSet
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.*;
import org.gradle.plugins.ide.eclipse.EclipsePlugin;
import org.gradle.plugins.ide.eclipse.model.EclipseModel;

class XtendPlugin implements Plugin<Project> {

	FileResolver fileResolver

	@Inject
	XtendPlugin(FileResolver fileResolver) {
		this.fileResolver = fileResolver
	}

	void apply(Project project) {
		project.extensions.create("xtend", XtendExtension)
		project.extensions.create("xtendRuntime", XtendRuntime, project)

		project.plugins.apply(JavaPlugin)
		JavaPluginConvention java = project.convention.getPlugin(JavaPluginConvention)
		java.sourceSets.all{SourceSet sourceSet ->
			def compileTaskName = sourceSet.getCompileTaskName("xtend")
			XtendCompile compileTask = project.task(type: XtendCompile, compileTaskName) { XtendCompile it ->
				it.srcDirs = new DefaultSourceDirectorySet("xtend", fileResolver) {
							def Set<DirectoryTree> getSrcDirTrees() {
								def all = super.getSrcDirTrees()
								all.grep{tree -> tree.dir != it.getTargetDir()}
							};
						}
				it.srcDirs.source(sourceSet.getJava())
				it.classpath = sourceSet.compileClasspath
				it.encoding = "UTF-8"
				it.conventionMapping.targetDir = {
					project.file("src/${sourceSet.getName()}/${project.extensions.xtend.sourceRelativeOutput}")
				}
				it.conventionMapping.xtendClasspath = {
					project.extensions.xtendRuntime.inferXtendClasspath(sourceSet.compileClasspath)
				}
				it.setDescription("Compiles the ${sourceSet.getName()} Xtend sources")
			}
			sourceSet.getJava().srcDir{compileTask.getTargetDir()}
			project.tasks[sourceSet.compileJavaTaskName].dependsOn(compileTask)
			project.tasks["clean"].dependsOn("clean" + compileTaskName.capitalize())
		}

		project.plugins.apply(EclipsePlugin)
		def EclipseModel eclipse = project.extensions.getByType(EclipseModel)
		eclipse.getProject().buildCommand("org.eclipse.xtext.ui.shared.xtextBuilder")
		eclipse.getProject().natures("org.eclipse.xtext.ui.shared.xtextNature")
		def settingsTask = project.task(type: XtendEclipseSettings, "xtendEclipseSettings")
		project.tasks[EclipsePlugin.ECLIPSE_TASK_NAME].dependsOn(settingsTask)
	}
}

class XtendEclipseSettings extends DefaultTask {
	@TaskAction
	def writeSettings() {
		project.file(".settings/org.eclipse.xtend.core.Xtend.prefs").write(
		"""\
		autobuilding=true
		eclipse.preferences.version=1
		is_project_specific=true
		outlet.DEFAULT_OUTPUT.cleanDirectory=true
		outlet.DEFAULT_OUTPUT.cleanupDerived=true
		outlet.DEFAULT_OUTPUT.createDirectory=true
		outlet.DEFAULT_OUTPUT.derived=true
		outlet.DEFAULT_OUTPUT.directory=${project.convention.xtend.sourceRelativeOutput}
		outlet.DEFAULT_OUTPUT.hideLocalSyntheticVariables=true
		outlet.DEFAULT_OUTPUT.installDslAsPrimarySource=false
		outlet.DEFAULT_OUTPUT.keepLocalHistory=false
		outlet.DEFAULT_OUTPUT.override=true
		""".stripIndent())
	}
}

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

	//TODO more options (as soon as they are supported by Main)

	//TODO allow using a daemon instead of forking a new process each time (this only makes sense once the compiler Main class caches the injector)
	@TaskAction
	def compile() {
		def sourcePath = getSrcDirs().srcDirTrees.collect{it.dir.absolutePath}.join(File.pathSeparator)
		def process = Runtime.runtime.exec("java -cp ${getXtendClasspath().asPath} org.eclipse.xtend.core.compiler.batch.Main -cp ${getClasspath().asPath} -d ${getTargetDir().absolutePath} -encoding ${getEncoding()} ${sourcePath}")
		//TODO if there is an exception during injector creation, the subprocess hangs indefinitely
		def exitCode = process.waitFor()
		if (exitCode != 0) {
			throw new GradleException("Xtend Compilation failed");
		}
	}
}

//TODO enhancement task, currently blocked because there is no simple Main class for this in xtend-core (instead it is "hidden" in the Maven plugin)

class XtendExtension {
	String sourceRelativeOutput = "xtend-gen"
}

class XtendRuntime {
	Project project

	XtendRuntime(Project project) {
		this.project = project
	}

	def FileCollection inferXtendClasspath(FileCollection classpath) {
		def pattern = Pattern.compile("org.eclipse.xtend.(core|lib)-(\\d.*?).jar")
		project.files {
			for (File file in classpath) {
				def matcher = pattern.matcher(file.getName())
				if (matcher.matches()) {
					def version = matcher.group(2)
					List<Dependency> dependencies = new ArrayList();
					dependencies.add(project.getDependencies().create("org.eclipse.xtend:org.eclipse.xtend.core:${version}"));
					if (version < "2.6.0") {
						dependencies.add(project.getDependencies().create("org.eclipse.xtend:org.eclipse.xtend.lib:${version}"));
					}
					return project.getConfigurations().detachedConfiguration(dependencies as Dependency[]);
				}
			}
			throw new GradleException("Could not infer Xtend classpath, because no Xtend jar was found on the ${classpath} classpath")
		}
	}
}