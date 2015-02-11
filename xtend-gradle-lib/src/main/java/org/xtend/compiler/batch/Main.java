package org.xtend.compiler.batch;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.xtend.core.XtendInjectorSingleton;
import org.eclipse.xtend.core.compiler.batch.XtendBatchCompiler;
import org.eclipse.xtext.util.Strings;

import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.inject.Injector;

public class Main {
	
	public static void main(String[] args) {
		if ((args == null) || (args.length == 0)) {
			printUsage();
			return;
		}
		if (!compile(args)) {
			System.exit(1);
		}
	}

	public static boolean compile(String[] args) {
		Injector injector = XtendInjectorSingleton.INJECTOR;
		FixedXtendBatchCompiler compiler = injector.getInstance(FixedXtendBatchCompiler.class);
		Iterator<String> arguments = Arrays.asList(args).iterator();
		List<String> sourcePath = Lists.newArrayList();
		while (arguments.hasNext()) {
			String argument = arguments.next();
			if ("-d".equals(argument.trim())) {
				compiler.setOutputPath(arguments.next().trim());
			} else if ("-classpath".equals(argument.trim()) || "-cp".equals(argument.trim())) {
				compiler.setClassPath(arguments.next().trim());
			} else if ("-bootClasspath".equals(argument.trim())) {
				setOption(compiler, "bootClassPath", arguments.next().trim());
			} else if ("-tempdir".equals(argument.trim()) || "-td".equals(argument.trim())) {
				compiler.setTempDirectory(arguments.next().trim());
			} else if ("-encoding".equals(argument.trim())) {
				compiler.setFileEncoding(arguments.next().trim());
			} else if ("-javaSourceVersion".equals(argument)) {
				setOption(compiler, "javaSourceVersion", arguments.next().trim());
			} else if ("-noSuppressWarningsAnnotation".equals(argument)) {
				setOption(compiler, "generateSyntheticSuppressWarnings", false);
			} else if ("-generateGeneratedAnnotation".equals(argument)) {
				setOption(compiler, "generateGeneratedAnnotation", true);
			} else if ("-includeDateInGeneratedAnnnotation".equals(argument)) {
				setOption(compiler, "includeDateInGeneratedAnnotation", true);
			} else if ("-generateAnnotationComment".equals(argument)) {
				setOption(compiler, "generatedAnnotationComment", arguments.next().trim());
			}else if ("-useCurrentClassLoader".equals(argument.trim())) {
				compiler.setUseCurrentClassLoaderAsParent(true);
			} else {
				sourcePath.add(argument);
			}
		}
		compiler.setSourcePath(Joiner.on(File.pathSeparator).join(sourcePath));
		return compiler.compile();
	}
	
	private static void setOption(XtendBatchCompiler compiler, String name, Object value) {
		try {
			Method setter = XtendBatchCompiler.class.getMethod("set" + Strings.toFirstUpper(name), value.getClass());
			setter.invoke(compiler, value);
		} catch (NoSuchMethodException e) {
			System.err.println("the - " + name + " option is not supported by this Xtend version");
		} catch (IllegalAccessException e) {
			Throwables.propagate(e);
		} catch (InvocationTargetException e) {
			Throwables.propagate(e.getCause());
		}
	}

	private static void printUsage() {
		System.out.println("Usage: Main <options> <source directories>");
		System.out.println("where possible options include:");
		System.out.println("-d <directory>             Specify where to place generated xtend files");
		System.out.println("-tp <path>                 Temp directory to hold generated stubs and classes");
		System.out.println("-cp <path>                 Specify where to find user class files");
		System.out.println("-encoding <encoding>       Specify character encoding used by source files");
		System.out.println("-useCurrentClassLoader	   Use current classloader as parent classloader");
	}

}
