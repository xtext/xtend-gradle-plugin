package org.xtend.batch.daemon;

import java.net.InetAddress;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import com.martiansoftware.nailgun.Alias;
import com.martiansoftware.nailgun.NGServer;

public class XtendCompilerServer {

	public static void main(String[] args) throws Exception {
		XtendCompilerServer server = new XtendCompilerServer(InetAddress.getLocalHost(), Integer.valueOf(args[0]));
		server.start();
	}

	private NGServer server;

	public XtendCompilerServer(InetAddress address, int port) {
		server = new NGServer(address, port);
		server.getAliasManager().addAlias(
				new Alias("compile", "Compile Xtend files", org.xtend.compiler.batch.Main.class));
		server.getAliasManager().addAlias(
				new Alias("enhance", "Enhance Java classes with Xtend debug info", org.xtend.enhance.batch.Main.class));
		server.getAliasManager().addAlias(
				new Alias("xtendClasspath", "Returns the classpath argument that was used for creating this server", ClassPathPrinter.class));
	}

	public void start() {
		TimerTask shutDown = new TimerTask() {
			@Override
			public void run() {
				server.shutdown(true);
			}
		};
		Timer timer = new Timer("Xtend compiler shutdown timer", true);
		timer.schedule(shutDown, TimeUnit.MINUTES.toMillis(10));
		Thread thread = new Thread(server);
		thread.setName("Xtend compiler server");
		thread.start();
	}
}