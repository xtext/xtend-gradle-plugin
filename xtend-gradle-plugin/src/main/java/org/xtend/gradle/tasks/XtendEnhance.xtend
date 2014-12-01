package org.xtend.gradle.tasks;

import java.io.File
import org.eclipse.xtend.lib.annotations.Accessors
import org.gradle.api.GradleException
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

import static extension org.xtend.gradle.GradleExtensions.*
import org.gradle.api.tasks.SkipWhenEmpty

class XtendEnhance extends XtendTask {
	@InputFiles @Accessors FileCollection sourceFolders;
	@Input @Accessors File classesFolder;
	@OutputDirectory @Accessors File targetFolder
	@Input @Accessors Boolean hideSyntheticVariables;
	@Input @Accessors Boolean xtendAsPrimaryDebugSource;
	
	@InputFiles @SkipWhenEmpty
	def getTraceFiles() {
		getSourceFolders.filter[exists].map[project.fileTree(it).filter[path.endsWith("._trace")]].reduce[$0.plus($1)]
	}

	@TaskAction
	def enhance() {
		if(!getClassesFolder.exists) return;
		val existingSourceFolders = getSourceFolders.files.filter[exists]
		for (folder : existingSourceFolders) {
			if(!folder.directory) throw new GradleException('''«folder» is not a directory''')
		}

		val enhanceArguments = newArrayList(
			"-c",
			getClassesFolder.absolutePath,
			"-o",
			getTargetFolder.absolutePath
		)

		if (getHideSyntheticVariables) {
			enhanceArguments += #["-hideSynthetic"]
		}
		if (getXtendAsPrimaryDebugSource) {
			enhanceArguments += #["-xtendAsPrimary"]
		}
		enhanceArguments += #[
			existingSourceFolders.join(" ")
		]

		project.copy [
			from = getClassesFolder.absolutePath
			into = getTargetFolder.absolutePath
		]
		invoke("org.xtend.enhance.batch.Main", "enhance", enhanceArguments)
	}
}
