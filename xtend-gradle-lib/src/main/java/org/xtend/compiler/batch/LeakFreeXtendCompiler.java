package org.xtend.compiler.batch;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;

import org.apache.log4j.Logger;
import org.eclipse.xtend.core.compiler.batch.XtendBatchCompiler;

public class LeakFreeXtendCompiler extends XtendBatchCompiler {
	private static Logger logger = Logger.getLogger(XtendBatchCompiler.class);

	protected void destroyClassLoader(ClassLoader loader) {
		if (loader instanceof Closeable) {
			try {
				((Closeable) loader).close();
			} catch (IOException e) {
				logger.warn("Could not close a classloader", e);
			}
		}
		if (loader instanceof URLClassLoader) {
			clearSunJarFileFactoryCache((URLClassLoader) loader, 5);
		}
	}

	// Copied from OpenEJB
	private void clearSunJarFileFactoryCache(URLClassLoader classLoader, int attempt) {
		logger.debug("Clearing Sun JarFileFactory cache");

		try {
			Class jarFileFactory = Class.forName("sun.net.www.protocol.jar.JarFileFactory");

			// Do not generify these maps as their contents are NOT stable
			// across runtimes.
			Field fileCacheField = jarFileFactory.getDeclaredField("fileCache");
			fileCacheField.setAccessible(true);
			Map fileCache = (Map) fileCacheField.get(null);
			Map fileCacheCopy = new HashMap(fileCache);

			Field urlCacheField = jarFileFactory.getDeclaredField("urlCache");
			urlCacheField.setAccessible(true);
			Map urlCache = (Map) urlCacheField.get(null);
			Map urlCacheCopy = new HashMap(urlCache);

			// The only stable item we have here is the JarFile/ZipFile in this
			// map
			Iterator iterator = urlCacheCopy.entrySet().iterator();
			List urlCacheRemoveKeys = new ArrayList();

			while (iterator.hasNext()) {
				Map.Entry entry = (Map.Entry) iterator.next();
				Object key = entry.getKey();

				if (key instanceof ZipFile) {
					ZipFile zf = (ZipFile) key;
					File file = new File(zf.getName()); // getName returns
														// File.getPath()
					if (Arrays.asList(classLoader.getURLs()).contains(
							file.toURL())) {
						// Flag for removal
						urlCacheRemoveKeys.add(key);
					}
				} else {
					logger.warn("Unexpected key type: " + key);
				}
			}

			iterator = fileCacheCopy.entrySet().iterator();
			List fileCacheRemoveKeys = new ArrayList();

			while (iterator.hasNext()) {
				Map.Entry entry = (Map.Entry) iterator.next();
				Object value = entry.getValue();

				if (urlCacheRemoveKeys.contains(value)) {
					fileCacheRemoveKeys.add(entry.getKey());
				}
			}

			// Use these unstable values as the keys for the fileCache values.
			iterator = fileCacheRemoveKeys.iterator();
			while (iterator.hasNext()) {

				Object next = iterator.next();

				try {
					Object remove = fileCache.remove(next);
					if (null != remove) {
						logger.debug("Removed item from fileCache: " + remove);
					}
				} catch (Throwable e) {
					logger.warn("Failed to remove item from fileCache: " + next);
				}
			}

			iterator = urlCacheRemoveKeys.iterator();
			while (iterator.hasNext()) {

				Object next = iterator.next();

				try {
					Object remove = urlCache.remove(next);

					try {
						((ZipFile) next).close();
					} catch (Throwable e) {
						// Ignore
					}

					if (null != remove) {
						logger.debug("Removed item from urlCache: " + remove);
					}
				} catch (Throwable e) {
					logger.warn("Failed to remove item from urlCache: " + next);
				}

			}

		} catch (ConcurrentModificationException e) {
			if (attempt > 0) {
				clearSunJarFileFactoryCache(classLoader, attempt - 1);
			} else {
				logger.error("Unable to clear Sun JarFileFactory cache", e);
			}
		} catch (ClassNotFoundException e) {
			// not a sun vm
		} catch (NoSuchFieldException e) {
			// not a sun vm
		} catch (Throwable e) {
			logger.error("Unable to clear Sun JarFileFactory cache", e);
		}
	}
}
