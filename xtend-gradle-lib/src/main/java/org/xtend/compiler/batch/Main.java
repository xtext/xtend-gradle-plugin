package org.xtend.compiler.batch;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.xtend.core.XtendInjectorSingleton;
import org.eclipse.xtend.core.compiler.batch.XtendBatchCompiler;

import com.google.common.base.Joiner;
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
		XtendBatchCompiler xtendBatchCompiler = injector.getInstance(XtendBatchCompiler.class);
		Iterator<String> arguments = Arrays.asList(args).iterator();
		List<String> sourcePath = Lists.newArrayList();
		while (arguments.hasNext()) {
			String argument = arguments.next();
			if ("-d".equals(argument.trim())) {
				xtendBatchCompiler.setOutputPath(arguments.next().trim());
			} else if ("-classpath".equals(argument.trim()) || "-cp".equals(argument.trim())) {
				xtendBatchCompiler.setClassPath(arguments.next().trim());
			} else if ("-tempdir".equals(argument.trim()) || "-td".equals(argument.trim())) {
				xtendBatchCompiler.setTempDirectory(arguments.next().trim());
			} else if ("-encoding".equals(argument.trim())) {
				xtendBatchCompiler.setFileEncoding(arguments.next().trim());
			} else if ("-useCurrentClassLoader".equals(argument.trim())) {
				xtendBatchCompiler.setUseCurrentClassLoaderAsParent(true);
			} else {
				sourcePath.add(argument);
			}
		}
		xtendBatchCompiler.setSourcePath(Joiner.on(File.pathSeparator).join(sourcePath));
		return xtendBatchCompiler.compile();
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
