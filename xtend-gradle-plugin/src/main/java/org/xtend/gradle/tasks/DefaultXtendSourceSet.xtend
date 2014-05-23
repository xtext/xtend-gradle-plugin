package org.xtend.gradle.tasks

import groovy.lang.Closure
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.file.DefaultSourceDirectorySet
import org.gradle.api.internal.file.FileResolver

class DefaultXtendSourceSet implements XtendSourceSet {

	SourceDirectorySet srcDirs
	Object xtendOutputDir
	FileResolver fileResolver

	new(FileResolver fileResolver) {
		this.fileResolver = fileResolver
		this.srcDirs = new DefaultSourceDirectorySet("xtend", fileResolver) {
			override getSrcDirTrees() {
				super.srcDirTrees.filter[dir != getXtendOutputDir].toSet
			}
		}
	}

	override getXtend() {
		srcDirs
	}

	override getXtendOutputDir() {
		fileResolver.resolve(xtendOutputDir)
	}

	override setXtendOutputDir(Object dir) {
		this.xtendOutputDir = dir
	}

	override xtend(Closure<SourceDirectorySet> configureClosure) {
		configureClosure.call(srcDirs)
		this
	}
}
