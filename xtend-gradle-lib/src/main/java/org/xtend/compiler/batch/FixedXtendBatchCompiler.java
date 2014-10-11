package org.xtend.compiler.batch;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URLClassLoader;
import java.util.List;

import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtend.core.compiler.batch.XtendBatchCompiler;
import org.eclipse.xtend.core.macro.ProcessorInstanceForJvmTypeProvider;
import org.eclipse.xtext.resource.XtextResourceSet;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

public class FixedXtendBatchCompiler extends XtendBatchCompiler {

	@Inject
	private ProcessorInstanceForJvmTypeProvider annotationProcessorFactory;
	
	private List<URLClassLoader> classLoaders = Lists.newArrayList();
	
	@Override
	public boolean compile() {
		try {
			return super.compile();
		} finally {
			for (URLClassLoader classLoader : classLoaders) {
				if (classLoader instanceof Closeable) {
					try {
						classLoader.close();
					} catch (IOException ignored) {
					}
				}
			}
		}
	}
	
	@Override
	protected void installJvmTypeProvider(ResourceSet resourceSet,
			File tmpClassDirectory, boolean skipIndexLookup) {
		super.installJvmTypeProvider(resourceSet, tmpClassDirectory, skipIndexLookup);
		URLClassLoader classLoader = (URLClassLoader) ((XtextResourceSet) resourceSet).getClasspathURIContext();
		classLoaders.add(classLoader);
		try {
			Field classLoaderField = ProcessorInstanceForJvmTypeProvider.class.getDeclaredField("classLoader");
			URLClassLoader annotationClassLoader = (URLClassLoader) classLoaderField.get(annotationProcessorFactory);
			classLoaders.add(annotationClassLoader);
		} catch (Exception ignored) {
		}
	}
}
