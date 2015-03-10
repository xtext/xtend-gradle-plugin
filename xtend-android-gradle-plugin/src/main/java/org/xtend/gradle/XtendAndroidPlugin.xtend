package org.xtend.gradle

import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import java.io.File
import javax.inject.Inject
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.plugins.UnknownPluginException
import org.xtend.gradle.tasks.DefaultXtendSourceSet
import org.xtend.gradle.tasks.XtendCompile

import static extension org.xtend.gradle.GradleExtensions.*
import org.xtend.gradle.tasks.XtendRuntime

class XtendAndroidPlugin implements Plugin<Project> {

	FileResolver fileResolver

	@Inject
	new(FileResolver fileResolver) {
		this.fileResolver = fileResolver
	}

	override apply(Project project) {
		project.plugins.<XtendBasePlugin>apply(XtendBasePlugin)
		val xtend = project.extensions.getByType(XtendRuntime)
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
				variant.javaCompile.classpath = variant.javaCompile.classpath + project.configurations.getAt("xtendCompileOnly")
				val compileTaskName = '''compile«variant.name.toFirstUpper»Xtend'''
				val xtendSources = new DefaultXtendSourceSet(fileResolver)
				val sourceDirs = newArrayList
				val javaDirs = variant.sourceSets.map[javaDirectories].flatten.filter[directory]
				sourceDirs += javaDirs
				sourceDirs += #[
					variant.aidlCompile.sourceOutputDir,
					variant.generateBuildConfig.sourceOutputDir,
					variant.renderscriptCompile.sourceOutputDir					
				]
				sourceDirs += variant.outputs.map[processResources.sourceOutputDir]
				xtendSources.xtend.srcDirs = sourceDirs
				xtendSources.xtendOutputDir = '''«project.buildDir»/generated/source/xtend/«variant.name»'''
				val xtendCompile = project.tasks.create(compileTaskName, XtendCompile)
				xtendCompile.srcDirs = xtendSources.xtend
				xtendCompile.classpath = variant.javaCompile.classpath
				xtendCompile.destinationDir = xtendSources.xtendOutputDir
				xtendCompile.bootClasspath = android.bootClasspath.join(File.pathSeparator)
				xtendCompile.classpath = xtendCompile.classpath + project.files(android.bootClasspath)
				xtendCompile.classesDir = variant.javaCompile.destinationDir
				xtendCompile.options.xtendAsPrimaryDebugSource = true
				xtendCompile.beforeExecute [
					xtendClasspath = xtend.inferXtendClasspath(classpath)
				]
				xtendCompile.setDescription('''Compiles the «variant.name» Xtend sources''')
				variant.registerJavaGeneratingTask(xtendCompile, xtendCompile.destinationDir)
				xtendCompile.dependsOn(variant.aidlCompile, variant.renderscriptCompile, variant.generateBuildConfig)
				xtendCompile.dependsOn(variant.outputs.map[processResources])
				variant.javaCompile.doLast[xtendCompile.enhance]
			]
		]
	}
}
