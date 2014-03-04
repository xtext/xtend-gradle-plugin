package org.xtend.gradle.tasks;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

class XtendEclipseSettings extends DefaultTask {
	@Input
	String sourceRelativeOutput
	
	//TODO make "hideSynthetic" and "primarySource" configurable via XtendXtension

	@TaskAction
	def writeSettings() {
		project.file(".settings/org.eclipse.xtend.core.Xtend.prefs").write(
				"""\
		autobuilding=true
		eclipse.preferences.version=1
		is_project_specific=true
		outlet.DEFAULT_OUTPUT.cleanDirectory=true
		outlet.DEFAULT_OUTPUT.cleanupDerived=true
		outlet.DEFAULT_OUTPUT.createDirectory=true
		outlet.DEFAULT_OUTPUT.derived=true
		outlet.DEFAULT_OUTPUT.directory=${getSourceRelativeOutput()}
		outlet.DEFAULT_OUTPUT.hideLocalSyntheticVariables=true
		outlet.DEFAULT_OUTPUT.installDslAsPrimarySource=false
		outlet.DEFAULT_OUTPUT.keepLocalHistory=false
		outlet.DEFAULT_OUTPUT.override=true
		""".stripIndent())
	}
}
