package org.xtend.gradle.tasks

import org.eclipse.core.internal.preferences.EclipsePreferences
import org.eclipse.core.runtime.Path
import org.gradle.api.Project
import org.osgi.service.prefs.BackingStoreException

class XtendEclipsePreferences extends EclipsePreferences {
	
	Project project
	
	new (Project project) {
		this.project = project
	}
	
	override protected getLocation() {
		val path = new Path(project.projectDir.absolutePath)
		computeLocation(path, "org.eclipse.xtend.core.Xtend")
	}
	
	override public save() throws BackingStoreException {
		super.save
	}
	
	override public load() throws BackingStoreException {
		super.load
	}
	
}