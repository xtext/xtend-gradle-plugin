package org.xtend.gradle.tasks

import org.eclipse.xtend.lib.annotations.Accessors
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.compile.AbstractOptions

@Accessors
abstract class AbstractXtendOptions extends AbstractOptions {
	@Input boolean hideSyntheticVariables = true
	@Input boolean xtendAsPrimaryDebugSource = false
	@Input boolean addSuppressWarnings = true
	@Nested GeneratedAnnotationOptions generatedAnnotation = new GeneratedAnnotationOptions
}

@Accessors
class GeneratedAnnotationOptions extends AbstractOptions {
	@Input boolean active
	@Input boolean includeDate
	@Input @Optional String comment
}

@Accessors
class XtendCompileOptions extends AbstractXtendOptions {
	@Input String encoding = "UTF-8"
}

@Accessors
class XtendEclipseOptions extends AbstractXtendOptions {
}
