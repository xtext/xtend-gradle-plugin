package org.xtend.gradle.tasks;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;

public class XtendCompilerClient {

	private NailgunClient delegate;

	private int port;

	public XtendCompilerClient(int port) {
		this.port = port;
		this.delegate = NailgunClient.onLocalHost(port);
	}

	public boolean compile(List<String> args) {
		return delegate.send("compile", args, new File(""), System.out, System.err) == 0;
	}

	public boolean enhance(List<String> args) {
		return delegate.send("enhance", args, new File(""), System.out, System.err) == 0;
	}

	public void requireServer(String classpath) {
		if (!delegate.serverAvailable()) {
			startServer(classpath);
		}
		if (!getServerClasspath().equals(classpath)) {
			delegate.stopServer();
			startServer(classpath);
		}
	}

	private String getServerClasspath() {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		delegate.send("xtendClasspath", new ArrayList<String>(), new File(""), out, out);
		return new String(out.toByteArray(), delegate.getEncoding()).trim();
	}

	private void startServer(String classpath) {
		List<String> command = Lists.newArrayList("java");
		command.add("-classpath");
		command.add(classpath);
		command.add("org.xtend.batch.daemon.XtendCompilerServer");
		command.add(String.valueOf(port));
		try {
			Runtime.getRuntime().exec(command.toArray(new String[command.size()]));
		} catch (Exception e) {
			Throwables.propagate(e);
		}
		int count = 0;
		while (!delegate.serverAvailable() && count < 50) {
			try {
				Thread.sleep(100);
				count++;
			} catch (InterruptedException e) {
				Throwables.propagate(e);
			}
		}
	}
}