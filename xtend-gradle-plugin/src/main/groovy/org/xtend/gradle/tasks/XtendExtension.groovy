package org.xtend.gradle.tasks;
import java.util.regex.Pattern

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.file.FileCollection

class XtendExtension {
	Project project
	String sourceRelativeOutput = "xtend-gen"

	XtendExtension(Project project) {
		this.project = project
	}

	def FileCollection inferXtendClasspath(FileCollection classpath) {
		def pattern = Pattern.compile("org.eclipse.xtend.(core|lib)-(\\d.*?).jar")
		project.files {
			for (File file in classpath) {
				def matcher = pattern.matcher(file.getName())
				if (matcher.matches()) {
					def version = matcher.group(2)
					List<Dependency> dependencies = new ArrayList();
					dependencies.add(project.getDependencies().create("org.eclipse.xtend:org.eclipse.xtend.core:${version}"));
					if (version < "2.6.0") {
						dependencies.add(project.getDependencies().create("org.eclipse.xtend:org.eclipse.xtend.lib:${version}"));
						dependencies.add(project.getDependencies().create("org.xtend:xtend-gradle-lib:0.0.1"))
					}
					return project.getConfigurations().detachedConfiguration(dependencies as Dependency[]);
				}
			}
			throw new GradleException("Could not infer Xtend classpath, because no Xtend jar was found on the ${classpath} classpath")
		}
	}
}

