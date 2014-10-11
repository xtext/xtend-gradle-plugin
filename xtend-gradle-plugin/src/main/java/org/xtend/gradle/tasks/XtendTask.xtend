package org.xtend.gradle.tasks

import java.util.List
import org.eclipse.xtend.lib.annotations.Accessors
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputFiles

abstract class XtendTask extends DefaultTask {

	@InputFiles @Accessors FileCollection xtendClasspath

	protected final def invoke(String className, String methodName, List<String> arguments) {
		System.setProperty("org.eclipse.emf.common.util.ReferenceClearingQueue", "false")
		val contextClassLoader = Thread.currentThread.contextClassLoader
		val classLoader = XtendExtension.getCompilerClassLoader(getXtendClasspath)
		try {
			Thread.currentThread.contextClassLoader = classLoader
			val main = classLoader.loadClass(className)
			val method = main.getMethod(methodName, typeof(String[]))
			val success = method.invoke(null, #[arguments as String[]]) as Boolean
			if (!success) {
				throw new GradleException('''Xtend «methodName» failed''');
			}
		} finally {
			Thread.currentThread.contextClassLoader = contextClassLoader
		}
	}
}
