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
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.plugins.ide.eclipse.EclipsePlugin;
import org.gradle.plugins.ide.eclipse.model.EclipseModel;
import org.xtend.gradle.tasks.XtendCompile;
import org.xtend.gradle.tasks.XtendEclipseSettings;
import org.xtend.gradle.tasks.XtendEnhance;
import org.xtend.gradle.tasks.XtendExtension;

class XtendPlugin implements Plugin<Project> {

	FileResolver fileResolver

	@Inject
	XtendPlugin(FileResolver fileResolver) {
		this.fileResolver = fileResolver
	}

	void apply(Project project) {
		project.extensions.create("xtend", XtendExtension, project)

		project.plugins.apply(JavaPlugin)
		JavaPluginConvention java = project.convention.getPlugin(JavaPluginConvention)
		java.sourceSets.all{SourceSet sourceSet ->
			def compileTaskName = sourceSet.getCompileTaskName("xtend")
			XtendCompile compileTask = project.task(type: XtendCompile, compileTaskName) { XtendCompile it ->
				it.srcDirs = new DefaultSourceDirectorySet("xtend", fileResolver) {
							def Set<DirectoryTree> getSrcDirTrees() {
								def all = super.getSrcDirTrees()
								all.grep{tree -> tree.dir != it.getTargetDir()}
							}
						}
				it.srcDirs.source(sourceSet.getJava())
				it.classpath = sourceSet.compileClasspath
				it.encoding = "UTF-8"
				it.conventionMapping.targetDir = {
					project.file("src/${sourceSet.getName()}/${project.extensions.xtend.sourceRelativeOutput}")
				}
				it.conventionMapping.xtendClasspath = {
					project.extensions.xtend.inferXtendClasspath(sourceSet.compileClasspath)
				}
				it.setDescription("Compiles the ${sourceSet.getName()} Xtend sources")
			}
			sourceSet.getJava().srcDir{compileTask.getTargetDir()}
			project.tasks[sourceSet.compileJavaTaskName].dependsOn(compileTask)
			project.tasks["clean"].dependsOn("clean" + compileTaskName.capitalize())

			def enhanceTaskName = "install${sourceSet.getName()}XtendDebugInfo"
			XtendEnhance enhanceTask = project.task(type: XtendEnhance, enhanceTaskName) {XtendEnhance it ->
				it.classesFolder = sourceSet.output.classesDir
				it.conventionMapping.xtendClasspath = {
					project.extensions.xtend.inferXtendClasspath(sourceSet.compileClasspath)
				}
				it.conventionMapping.sourceFolders = {
					project.files(compileTask.getTargetDir())
				}
			}
			enhanceTask.dependsOn(project.tasks[sourceSet.compileJavaTaskName])
			project.tasks[sourceSet.classesTaskName].dependsOn(enhanceTask)
		}

		project.plugins.apply(EclipsePlugin)
		def EclipseModel eclipse = project.extensions.getByType(EclipseModel)
		eclipse.getProject().buildCommand("org.eclipse.xtext.ui.shared.xtextBuilder")
		eclipse.getProject().natures("org.eclipse.xtext.ui.shared.xtextNature")
		def settingsTask = project.task(type: XtendEclipseSettings, "xtendEclipseSettings")
		settingsTask.conventionMapping.sourceRelativeOutput = {
			project.extensions.xtend.sourceRelativeOutput
		}
		project.tasks[EclipsePlugin.ECLIPSE_TASK_NAME].dependsOn(settingsTask)
	}
}