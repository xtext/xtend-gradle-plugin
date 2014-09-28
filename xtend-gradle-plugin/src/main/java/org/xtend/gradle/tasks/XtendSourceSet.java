package org.xtend.gradle.tasks;

import java.io.File;

import groovy.lang.Closure;

import org.gradle.api.file.SourceDirectorySet;

public interface XtendSourceSet {
	SourceDirectorySet getXtend();

	XtendSourceSet xtend(Closure<SourceDirectorySet> configureClosure);

	File getXtendOutputDir();

	void setXtendOutputDir(Object dir);
}
