package org.xtend.gradle

import java.io.File
import javax.inject.Inject
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.plugins.ide.eclipse.EclipsePlugin
import org.xtend.gradle.tasks.DefaultXtendSourceSet
import org.xtend.gradle.tasks.XtendCompile
import org.xtend.gradle.tasks.XtendEnhance
import org.xtend.gradle.tasks.XtendExtension

import static extension org.xtend.gradle.GradleExtensions.*

class XtendPlugin implements Plugin<Project> {

	FileResolver fileResolver

	@Inject
	new(FileResolver fileResolver) {
		this.fileResolver = fileResolver
	}

	override apply(Project project) {
		project.plugins.<XtendBasePlugin>apply(XtendBasePlugin)
		project.plugins.<JavaPlugin>apply(JavaPlugin)
		project.plugins.<EclipsePlugin>apply(EclipsePlugin)

		val java = project.convention.getPlugin(JavaPluginConvention)
		java.sourceSets.all [ sourceSet |
			sourceSet.compileClasspath = sourceSet.compileClasspath + project.configurations.getAt("xtendCompileOnly")
			val xtendSourceSet = new DefaultXtendSourceSet(fileResolver)
			xtendSourceSet.xtend.source(sourceSet.java)
			xtendSourceSet.xtendOutputDir = '''«project.buildDir»/xtend-gen/«sourceSet.name»'''
			new DslObject(sourceSet).convention.plugins.put("xtend", xtendSourceSet);
			val compileTaskName = sourceSet.getCompileTaskName("xtend")
			val javaCompile = project.tasks.getAt(sourceSet.compileJavaTaskName) as JavaCompile
			val compileTask = project.tasks.create(compileTaskName, XtendCompile) [
				srcDirs = xtendSourceSet.xtend
				conventionMapping(
					#{
						"targetDir" -> [|xtendSourceSet.xtendOutputDir],
						"classpath" -> [|sourceSet.compileClasspath],
						"bootClasspath" -> [|javaCompile.options.bootClasspath]
					})
				setDescription('''Compiles the «sourceSet.name» Xtend sources''')
			]
			project.afterEvaluate [
				sourceSet.java.srcDir(compileTask.targetDir)
			]
			javaCompile.dependsOn(compileTask)
			val classesDir = sourceSet.output.classesDir
			val unenhancedClassesDir = new File(classesDir.absolutePath + "-unenhanced")
			val enhanceTaskName = '''install«sourceSet.name.toFirstUpper»XtendDebugInfo'''
			val enhanceTask = project.tasks.create(enhanceTaskName, XtendEnhance) [
				targetFolder = classesDir
				classesFolder = unenhancedClassesDir
				conventionMapping(
					#{
						"xtendClasspath" -> [|
							project.extensions.getByType(XtendExtension).inferXtendClasspath(
								sourceSet.compileClasspath)],
						"sourceFolders" -> [|project.files(compileTask.targetDir) as FileCollection]
					})
			]
			javaCompile.setDestinationDir(unenhancedClassesDir)
			enhanceTask.dependsOn(javaCompile)
			project.tasks.getAt(sourceSet.classesTaskName).dependsOn(enhanceTask)
		]
	}
}
