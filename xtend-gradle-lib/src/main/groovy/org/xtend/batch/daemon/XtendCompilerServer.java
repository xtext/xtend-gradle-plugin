package org.xtend.batch.daemon;

import com.martiansoftware.nailgun.Alias;
import com.martiansoftware.nailgun.NGServer;

public class XtendCompilerServer {

	public static void main(String[] args) throws Exception {
		NGServer server = new NGServer(null, 3032);
		server.getAliasManager().addAlias(new Alias("compile", 
				"Compile Xtend files", 
				org.xtend.compiler.batch.Main.class));
		server.getAliasManager().addAlias(new Alias("enhance", 
				"Enhance Java classes with Xtend debug info", 
				org.xtend.enhance.batch.Main.class));
		Thread thread = new Thread(server);
		thread.setName("Xtend compiler server");
		thread.start();
	}
}