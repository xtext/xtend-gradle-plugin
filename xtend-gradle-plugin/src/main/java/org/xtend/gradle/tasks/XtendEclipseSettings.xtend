package org.xtend.gradle.tasks

import de.oehme.xtend.contrib.Property
import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import com.google.common.base.CharMatcher

class XtendEclipseSettings extends DefaultTask {
	@Input @Property Boolean hideSyntheticVariables
	@Input @Property Boolean xtendAsPrimaryDebugSource

	@TaskAction
	def writeSettings() {
		val settings = new XtendEclipsePreferences(project)
		settings.putBoolean("is_project_specific", true)
		settings.putBoolean("hideLocalSyntheticVariables".key, getHideSyntheticVariables)
		settings.putBoolean("installDslAsPrimarySource".key, getXtendAsPrimaryDebugSource)
		settings.putBoolean("userOutputPerSourceFolder".key, true)
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
