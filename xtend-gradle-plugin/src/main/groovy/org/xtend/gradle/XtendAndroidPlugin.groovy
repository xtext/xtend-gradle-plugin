package org.xtend.gradle;

import javax.inject.Inject;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.DirectoryTree
import org.gradle.api.internal.file.DefaultSourceDirectorySet
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.UnknownPluginException;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.plugins.ide.eclipse.EclipsePlugin;
import org.gradle.plugins.ide.eclipse.model.EclipseModel
import org.xtend.gradle.tasks.XtendCompile;
import org.xtend.gradle.tasks.XtendEclipseSettings;
import org.xtend.gradle.tasks.XtendEnhance
import org.xtend.gradle.tasks.XtendExtension;

import com.android.build.gradle.AppExtension;
import com.android.build.gradle.AppPlugin;
import com.android.build.gradle.BaseExtension;
import com.android.build.gradle.LibraryPlugin;
import com.android.build.gradle.api.AndroidSourceSet;
import com.android.build.gradle.api.BaseVariant
import com.google.common.collect.Iterables;

class XtendAndroidPlugin implements Plugin<Project> {

	FileResolver fileResolver

	@Inject
	XtendAndroidPlugin(FileResolver fileResolver) {
		this.fileResolver = fileResolver
	}

	@Override
	public void apply(Project project) {
		project.plugins.apply(XtendBasePlugin)
		project.extensions.xtend.xtendAsPrimaryDebugSource = true
		project.afterEvaluate{
			def variants
			BaseExtension android = project.extensions.android
			if (android instanceof AppExtension) {
				variants = android.applicationVariants
			} else {
				variants = android.libraryVariants
			}
			variants.all{BaseVariant variant ->
				def compileTaskName = "compile${variant.getName().capitalize()}Xtend"
				def xtendSources = new DefaultSourceDirectorySet("xtend", fileResolver)
				def sourceDirs = new ArrayList()
				def javaDirs = variant.getSourceSets().collect{source-> source.javaDirectories}.flatten().grep{File dir -> dir.isDirectory()}
				sourceDirs.addAll(javaDirs)
				sourceDirs.addAll(variant.getAidlCompile().getSourceOutputDir())
				sourceDirs.addAll(variant.getGenerateBuildConfig().getSourceOutputDir())
				sourceDirs.addAll(variant.getRenderscriptCompile().getSourceOutputDir())
				sourceDirs.addAll(variant.getProcessResources().getSourceOutputDir())
				xtendSources.srcDirs(sourceDirs as File[])

				XtendCompile xtendCompile = project.task(type: XtendCompile, compileTaskName)
				xtendCompile.srcDirs = xtendSources
				xtendCompile.classpath = variant.getJavaCompile().getClasspath()
				xtendCompile.conventionMapping.targetDir = {
					def sourceBase = Iterables.getLast(variant.getSourceSets()).getJavaDirectories().toList().first().getParent()
					project.file("${sourceBase}/${project.extensions.xtend.sourceRelativeOutput}")
				}
				xtendCompile.doFirst{
					com.android.build.gradle.BasePlugin androidPlugin
					try {
						androidPlugin = project.plugins.getPlugin(AppPlugin)
					} catch(UnknownPluginException e) {
						androidPlugin = project.plugins.getPlugin(LibraryPlugin)
					}
					xtendCompile.classpath = xtendCompile.getClasspath() + project.files(androidPlugin.getRuntimeJarList() as String[])
				}
				xtendCompile.setDescription("Compiles the ${variant.getName()} Xtend sources")
				variant.registerJavaGeneratingTask(xtendCompile, xtendCompile.getTargetDir())
				xtendCompile.dependsOn("generate${variant.name.capitalize()}Sources")
				project.tasks["clean"].dependsOn("clean" + compileTaskName.capitalize())

				def classesDir = variant.getJavaCompile().getDestinationDir()
				def unenhancedClassesDir = new File(classesDir.absolutePath + "-unenhanced")
				variant.javaCompile.destinationDir = unenhancedClassesDir
				def enhanceTaskName = "install${variant.getName().capitalize()}XtendDebugInfo"
				XtendEnhance enhanceTask = project.task(type: XtendEnhance, enhanceTaskName)
				enhanceTask.sourceFolders = project.files(xtendCompile.getTargetDir())
				enhanceTask.classesFolder = unenhancedClassesDir
				enhanceTask.targetFolder = classesDir
				enhanceTask.conventionMapping.xtendClasspath = {
					project.extensions.xtend.inferXtendClasspath(variant.getJavaCompile().getClasspath())
				}
				enhanceTask.dependsOn(variant.getJavaCompile())
				variant.getAssemble().dependsOn(enhanceTask)
			}
		}
	}
}