package org.xtend.gradle.tasks;

import java.io.File;
import java.util.List;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;

public class XtendCompilerClient {

	// TODO make port configurable
	private NailgunClient delegate = NailgunClient.onLocalHost(3032);

	public boolean compile(List<String> args) {
		return delegate.send("compile", args, new File(""), System.out, System.err) == 0;
	}

	public boolean enhance(List<String> args) {
		return delegate.send("enhance", args, new File(""), System.out, System.err) == 0;
	}

	public void requireServer(String classpath) {
		if (!isServerRunning()) {
			startServer(classpath);
		}
	}

	private boolean isServerRunning() {
		return delegate.serverAvailable();
	}

	private void startServer(String classpath) {
		List<String> command = Lists.newArrayList("java");
		command.add("-classpath");
		command.add(classpath);
		command.add("org.xtend.batch.daemon.XtendCompilerServer");
		try {
			Runtime.getRuntime().exec(command.toArray(new String[command.size()]));
		} catch (Exception e) {
			Throwables.propagate(e);
		}
		int count = 0;
		while (!isServerRunning() && count < 50) {
			try {
				Thread.sleep(100);
				count++;
			} catch (InterruptedException e) {
				Throwables.propagate(e);
			}
		}
	}
}