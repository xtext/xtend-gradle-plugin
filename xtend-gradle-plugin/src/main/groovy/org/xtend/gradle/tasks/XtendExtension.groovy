package org.xtend.gradle.tasks;
import java.util.regex.Pattern

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.file.FileCollection

class XtendExtension {
	Project project
	String sourceRelativeOutput = "xtend-gen"
	String encoding = "UTF-8"
	boolean hideSyntheticVariables = true
	boolean xtendAsPrimaryDebugSource = false
	boolean useDaemon = false
	int daemonPort = 3032

	XtendExtension(Project project) {
		this.project = project
	}

  private String getPluginVersion() {
    this.class.package.implementationVersion
  }

	def FileCollection inferXtendClasspath(FileCollection classpath) {
		def xtendPluginVersion = getPluginVersion()
		def pattern = Pattern.compile("org.eclipse.xtend.(core|lib)-(\\d.*?).jar")
		project.files {
			for (File file in classpath) {
				def matcher = pattern.matcher(file.getName())
				if (matcher.matches()) {
					def xtendVersion = matcher.group(2)
					List<Dependency> dependencies = new ArrayList();
					dependencies.add(project.getDependencies().create("org.eclipse.xtend:org.eclipse.xtend.core:${xtendVersion}") { 
            force = true
            exclude group: 'asm'
          });
          dependencies.add(project.getDependencies().create("org.xtend:xtend-gradle-lib:${xtendPluginVersion}") {
              exclude group: 'asm'
          })
          dependencies.add(project.getDependencies().create("org.eclipse.xtend:org.eclipse.xtend.lib:${xtendVersion}"));
					return project.getConfigurations().detachedConfiguration(dependencies as Dependency[]);
				}
			}
			throw new GradleException("Could not infer Xtend classpath, because no Xtend jar was found on the ${classpath} classpath")
		}
	}
}

