package org.xtend.gradle

import javax.inject.Inject
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.plugins.ide.eclipse.EclipsePlugin
import org.xtend.gradle.tasks.DefaultXtendSourceSet
import org.xtend.gradle.tasks.XtendCompile

import static extension org.xtend.gradle.GradleExtensions.*
import org.xtend.gradle.tasks.XtendRuntime

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

		val xtendRuntime = project.extensions.getByType(XtendRuntime)
		val java = project.convention.getPlugin(JavaPluginConvention)
		
		java.sourceSets.all [ sourceSet |
			sourceSet.compileClasspath = sourceSet.compileClasspath + project.configurations.getAt("xtendCompileOnly")
			val xtendSourceSet = new DefaultXtendSourceSet(fileResolver)
			xtendSourceSet.xtend.source(sourceSet.java)
			xtendSourceSet.xtendOutputDir = '''«project.buildDir»/xtend-gen/«sourceSet.name»'''
			new DslObject(sourceSet).convention.plugins.put("xtend", xtendSourceSet);
			val compileTaskName = sourceSet.getCompileTaskName("xtend")
			val javaCompile = project.tasks.getAt(sourceSet.compileJavaTaskName) as JavaCompile
			val xtendCompile = project.tasks.create(compileTaskName, XtendCompile) [
				setDescription('''Compiles the «sourceSet.name» Xtend sources''')
				srcDirs = xtendSourceSet.xtend
				classpath = sourceSet.compileClasspath
				project.afterEvaluate [p|
					destinationDir = xtendSourceSet.xtendOutputDir
					bootClasspath = javaCompile.options.bootClasspath
					classesDir = javaCompile.destinationDir
					sourceCompatibility = java.sourceCompatibility.toString
					sourceSet.java.srcDir(destinationDir)
				]
				beforeExecute[
					xtendClasspath = xtendRuntime.inferXtendClasspath(classpath)
				]
			]
			javaCompile.dependsOn(xtendCompile)
			javaCompile.doLast[xtendCompile.enhance]
		]
	}
}
