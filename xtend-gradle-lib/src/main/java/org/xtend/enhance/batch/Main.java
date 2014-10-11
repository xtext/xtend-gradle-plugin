package org.xtend.enhance.batch;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;

import org.eclipse.xtend.core.XtendInjectorSingleton;

import com.google.inject.Injector;

public class Main {

	public static void main(String[] args) {
		enhance(args);
	}

	public static boolean enhance(String[] args) {
		Injector injector = XtendInjectorSingleton.INJECTOR;
		XtendDebugInfoInstaller installer = injector.getInstance(XtendDebugInfoInstaller.class);
		if ((args == null) || (args.length == 0)) {
			printUsage();
			return false;
		}
		Iterator<String> arguments = Arrays.asList(args).iterator();
		while (arguments.hasNext()) {
			String argument = arguments.next();
			if ("-o".equals(argument.trim())) {
				installer.setOutputDirectory(new File(arguments.next().trim()));
			} else if ("-hideSynthetic".equals(argument.trim())) {
				installer.setHideSyntheticVariables(true);
			} else if ("-xtendAsPrimary".equals(argument.trim())) {
				installer.setXtendAsPrimaryDebugSource(true);
			} else if ("-c".equals(argument.trim())) {
				installer.setClassesDirectory(new File(arguments.next().trim()));
			} else {
				installer.getInputDirectories().add(new File(argument));
			}
		}
		try {
			installer.installDebugInfo();
		} catch (Exception e) {
			e.printStackTrace(System.out);
			return false;
		}
		return true;
	}

	private static void printUsage() {
		System.out.println("Usage: Main <options> <source directories>");
		System.out.println("where possible options include:");
		System.out.println("-d <directory>             Specify the classes directory that should be enhanced");
		System.out.println("-hideSynthetic			   Hide synthetic variables in debugger");
		System.out.println("-xtendAsPrimary			   Install Xtend as main debug source (useful for Android)");
	}
}
