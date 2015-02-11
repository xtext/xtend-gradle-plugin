package org.xtend.gradle.tasks

import com.google.common.base.CharMatcher
import java.io.File
import org.eclipse.xtend.lib.annotations.Accessors
import org.gradle.api.DefaultTask
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.Input

@Accessors
class XtendEclipseSettings extends DefaultTask {
	@Input String sourceCompatibility;
	@Nested XtendEclipseOptions options = new XtendEclipseOptions

	@TaskAction
	def writeSettings() {
		val settings = new XtendEclipsePreferences(project)
		settings.load
		settings.putBoolean("is_project_specific", true)
		settings.putBoolean("hideLocalSyntheticVariables".key, getOptions.hideSyntheticVariables)
		settings.putBoolean("installDslAsPrimarySource".key, getOptions.xtendAsPrimaryDebugSource)
		settings.putBoolean("userOutputPerSourceFolder".key, true)
		settings.put("targetJavaVersion".key, getSourceCompatibility)
		settings.putBoolean("generateSuppressWarnings".key, getOptions.addSuppressWarnings)
		getOptions.generatedAnnotation => [
			settings.putBoolean("generateGeneratedAnnotation".key, isActive)
			settings.putBoolean("includeDateInGenerated".key, includeDate)
			settings.put("generatedAnnotationComment".key, comment ?: "")
		]
		val java = project.convention.getPlugin(JavaPluginConvention)
		java.sourceSets.all [ sourceSet |
			val xtendSourceSet = new DslObject(sourceSet).convention.plugins.get("xtend") as XtendSourceSet
			xtendSourceSet.xtend.srcDirs.forEach [
				settings.put(projectRelativePath.outputForSourceFolderKey,
					xtendSourceSet.xtendOutputDir.projectRelativePath)
			]
		]
		settings.save
	}

	private def projectRelativePath(File file) {
		project.projectDir.toURI.relativize(file.toURI).path.trimTrailingSeparator
	}

	private def trimTrailingSeparator(String path) {
		CharMatcher.anyOf("/\\").trimTrailingFrom(path)
	}

	private def String getKey(String preferenceName) {
		return "outlet" + "." + "DEFAULT_OUTPUT" + "." + preferenceName;
	}

	private def String getOutputForSourceFolderKey(String sourceFolder) {
		return getKey("sourceFolder" + "." + sourceFolder + "." + "directory");
	}
}
