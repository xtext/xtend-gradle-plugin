package org.xtend.gradle

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.BasePlugin
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import java.io.File
import javax.inject.Inject
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.plugins.UnknownPluginException
import org.xtend.gradle.tasks.DefaultXtendSourceSet
import org.xtend.gradle.tasks.XtendCompile
import org.xtend.gradle.tasks.XtendEnhance
import org.xtend.gradle.tasks.XtendExtension

import static extension org.xtend.gradle.GradleExtensions.*

class XtendAndroidPlugin implements Plugin<Project> {

	FileResolver fileResolver

	@Inject
	new(FileResolver fileResolver) {
		this.fileResolver = fileResolver
	}

	override apply(Project project) {
		project.plugins.<XtendBasePlugin>apply(XtendBasePlugin)
		val xtend = project.extensions.getByType(XtendExtension)
		xtend.xtendAsPrimaryDebugSource = true
		project.afterEvaluate [
			val android = project.extensions.getByName("android") as BaseExtension
			val variants = if (android instanceof AppExtension) {
					android.applicationVariants
				} else if (android instanceof LibraryExtension) {
					android.libraryVariants
				} else {
					throw new GradleException('''Unknown packaging type «android.class.simpleName»''')
				}
			variants.all [ variant |
				val compileTaskName = '''compile«variant.name.toFirstUpper»Xtend'''
				val xtendSources = new DefaultXtendSourceSet(fileResolver)
				val sourceDirs = newArrayList
				val javaDirs = variant.sourceSets.map[javaDirectories].flatten.filter[directory]
				sourceDirs.addAll(javaDirs)
				sourceDirs.addAll(variant.aidlCompile.sourceOutputDir)
				sourceDirs.addAll(variant.generateBuildConfig.sourceOutputDir)
				sourceDirs.addAll(variant.renderscriptCompile.sourceOutputDir)
				sourceDirs.addAll(variant.processResources.sourceOutputDir)
				xtendSources.xtend.srcDirs(sourceDirs)
				xtendSources.xtendOutputDir = '''build/xtend-gen/«variant.name»'''
				val xtendCompile = project.tasks.create(compileTaskName, XtendCompile)
				xtendCompile.srcDirs = xtendSources.xtend
				xtendCompile.classpath = variant.javaCompile.classpath
				xtendCompile.targetDir = xtendSources.xtendOutputDir
				xtendCompile.doFirst [
					val BasePlugin androidPlugin = try {
						project.plugins.<AppPlugin>getPlugin(AppPlugin)
					} catch (UnknownPluginException e) {
						project.plugins.<LibraryPlugin>getPlugin(LibraryPlugin)
					}
					xtendCompile.classpath = xtendCompile.classpath.plus(project.files(androidPlugin.bootClasspath))
				]
				xtendCompile.setDescription('''Compiles the «variant.name» Xtend sources''')
				variant.registerJavaGeneratingTask(xtendCompile, xtendCompile.targetDir)
				xtendCompile.dependsOn('''generate«variant.name.toFirstUpper»Sources''')
				val classesDir = variant.javaCompile.destinationDir
				val unenhancedClassesDir = new File(classesDir.absolutePath + "-unenhanced")
				variant.javaCompile.destinationDir = unenhancedClassesDir
				val enhanceTaskName = '''install«variant.name.toFirstUpper»XtendDebugInfo'''
				val enhanceTask = project.tasks.create(enhanceTaskName, XtendEnhance)
				enhanceTask.sourceFolders = project.files(xtendCompile.targetDir)
				enhanceTask.classesFolder = unenhancedClassesDir
				enhanceTask.targetFolder = classesDir
				enhanceTask.conventionMapping(
					#{
						"xtendClasspath" -> [ |
							xtend.inferXtendClasspath(variant.javaCompile.classpath)
						]
					})
				enhanceTask.dependsOn(variant.javaCompile)
				variant.assemble.dependsOn(enhanceTask)
			]
		]
	}
}
