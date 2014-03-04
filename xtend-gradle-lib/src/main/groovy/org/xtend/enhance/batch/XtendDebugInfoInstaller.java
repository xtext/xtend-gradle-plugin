package org.xtend.enhance.batch;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.xtext.generator.trace.AbstractTraceRegion;
import org.eclipse.xtext.generator.trace.ITraceToBytecodeInstaller;
import org.eclipse.xtext.generator.trace.TraceAsPrimarySourceInstaller;
import org.eclipse.xtext.generator.trace.TraceAsSmapInstaller;
import org.eclipse.xtext.generator.trace.TraceFileNameProvider;
import org.eclipse.xtext.generator.trace.TraceRegionSerializer;
import org.eclipse.xtext.util.Strings;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class XtendDebugInfoInstaller {

	@Inject
	private ClassFileDebugSourceExtractor classFileDebugSourceExtractor;

	@Inject
	private Provider<TraceAsPrimarySourceInstaller> traceAsPrimarySourceInstallerProvider;

	@Inject
	private Provider<TraceAsSmapInstaller> traceAsSmapInstaller;

	@Inject
	private TraceFileNameProvider traceFileNameProvider;

	@Inject
	private TraceRegionSerializer traceRegionSerializer;

	private List<File> inputDirectories = Lists.newArrayList();
	private File outputDirectory;
	private File classesDirectory;
	private boolean hideSyntheticVariables;
	private boolean xtendAsPrimaryDebugSource;

	public List<File> getInputDirectories() {
		return inputDirectories;
	}
	
	public File getClassesDirectory() {
		return classesDirectory;
	}
	
	public void setClassesDirectory(File classesDirectory) {
		this.classesDirectory = classesDirectory;
	}

	public File getOutputDirectory() {
		return outputDirectory;
	}

	public void setOutputDirectory(File outputDirectory) {
		this.outputDirectory = outputDirectory;
	}

	public boolean isHideSyntheticVariables() {
		return hideSyntheticVariables;
	}

	public void setHideSyntheticVariables(boolean hideSyntheticVariables) {
		this.hideSyntheticVariables = hideSyntheticVariables;
	}

	public boolean isXtendAsPrimaryDebugSource() {
		return xtendAsPrimaryDebugSource;
	}

	public void setXtendAsPrimaryDebugSource(boolean xtendAsPrimaryDebugSource) {
		this.xtendAsPrimaryDebugSource = xtendAsPrimaryDebugSource;
	}

	public void installDebugInfo() {
		Multimap<File, File> trace2class = createTraceToClassFileMap(inputDirectories, classesDirectory);
		logStatus(classesDirectory, trace2class);
		installTraces(trace2class);
	}

	private void collectJavaSourceFile2traceFile(File root, String subdir, Map<String, File> javaSourceFile2traceFile) {
		File file = new File(root + "/" + subdir);
		File[] listFiles = file.listFiles();
		if (listFiles == null) {
			getLogger().warn("Directory " + file.getPath() + " is empty. Can't process.");
			return;
		}
		for (File child : listFiles) {
			String name = child.getName();
			if (child.isDirectory())
				collectJavaSourceFile2traceFile(root, subdir + "/" + name, javaSourceFile2traceFile);
			else if (name.endsWith(TraceFileNameProvider.TRACE_FILE_EXTENSION)) {
				String javaSourceFile = subdir + "/" + traceFileNameProvider.getJavaFromTrace(name);
				javaSourceFile2traceFile.put(javaSourceFile, child);
			}
		}
	}

	private ITraceToBytecodeInstaller createTraceToBytecodeInstaller() {
		if (xtendAsPrimaryDebugSource) {
			TraceAsPrimarySourceInstaller installer = traceAsPrimarySourceInstallerProvider.get();
			installer.setHideSyntheticVariables(hideSyntheticVariables);
			return installer;
		} else {
			TraceAsSmapInstaller installer = traceAsSmapInstaller.get();
			return installer;
		}
	}

	private Multimap<File, File> createTraceToClassFileMap(Collection<File> sourceFolders, File outputFolder) {
		Map<String, File> javaSourceFile2traceFile = Maps.newLinkedHashMap();

		for (File sourceRoot : sourceFolders)
			collectJavaSourceFile2traceFile(sourceRoot, "", javaSourceFile2traceFile);

		Set<String> packageDirs = Sets.newLinkedHashSet();
		for (String javaSourceFile : javaSourceFile2traceFile.keySet())
			packageDirs.add(Strings.skipLastToken(javaSourceFile, "/"));

		Multimap<File, File> trace2class = LinkedHashMultimap.create();

		for (String packageDirName : packageDirs) {
			File packageDir = new File(outputFolder + "/" + packageDirName);
			if (packageDir.isDirectory()) {
				for (File classFile : packageDir.listFiles())
					if (classFile.getName().endsWith(".class"))
						try {
							String sourceFileName = classFileDebugSourceExtractor.getDebugSourceFileName(classFile);
							if (Strings.isEmpty(sourceFileName))
								continue;
							if (!sourceFileName.toLowerCase().endsWith(".java"))
								continue;
							File traceFile = javaSourceFile2traceFile.get(packageDirName + "/" + sourceFileName);
							if (traceFile != null)
								trace2class.put(traceFile, classFile);
						} catch (IOException e) {
							getLogger().error("Error reading " + classFile, e);
						}
			}
		}
		return trace2class;
	}

	private void installTrace(File traceFile, Collection<File> classFiles) throws FileNotFoundException, IOException {
		ITraceToBytecodeInstaller traceToBytecodeInstaller = createTraceToBytecodeInstaller();
		InputStream in = new FileInputStream(traceFile);
		try {
			AbstractTraceRegion traceRegion = traceRegionSerializer.readTraceRegionFrom(in);
			traceToBytecodeInstaller.setTrace(traceFileNameProvider.getJavaFromTrace(traceFile.getName()), traceRegion);
			if (getLogger().isDebugEnabled())
				getLogger().debug("Installing trace " + traceFile + " into:");
			for (File classFile : classFiles) {
				if (getLogger().isDebugEnabled())
					getLogger().debug("  " + classFile);
				File outputFile = new File(classFile.getAbsolutePath().replace(classesDirectory.getAbsolutePath(), outputDirectory.getAbsolutePath()));
				outputFile.getParentFile().mkdirs();
				Files.write(traceToBytecodeInstaller.installTrace(Files.toByteArray(classFile)), outputFile);
			}
		} finally {
			in.close();
		}
	}

	private void installTraces(Multimap<File, File> trace2class) {
		for (Map.Entry<File, Collection<File>> e : trace2class.asMap().entrySet()) {
			try {
				installTrace(e.getKey(), e.getValue());
			} catch (Exception e1) {
				getLogger().error(e1);
			}
		}
	}

	private void logStatus(File folder, Multimap<File, File> trace2class) {
		String p = xtendAsPrimaryDebugSource ? "primary" : "secondary (via SMAP)";
		int n = trace2class.size();
		getLogger().info("Installing Xtend files into " + n + " class files as " + p + " debug sources in: " + folder);
		getLogger().debug("xtendAsPrimaryDebugSource=" + xtendAsPrimaryDebugSource);
		getLogger().debug("hideSyntheticVariables=" + hideSyntheticVariables);
	}

	private Logger getLogger() {
		return Logger.getLogger(getClass());
	}

}