xtend-gradle-plugin
===================

[![Build Status](https://oehme.ci.cloudbees.com/buildStatus/icon?job=xtend-gradle-plugin)](https://oehme.ci.cloudbees.com/job/xtend-gradle-plugin/)

A gradle plugin for building Xtend projects, **even with the new Android build system!**

Features
--------

- Compiles Xtend sources to Java
- Enhances Java classes with Xtend debug information
- A compiler daemon to speed up builds
- Automatically downloads the correct Xtend compiler based on which version of xtend.lib you use
- Supports both normal Java projects and the new Android build system
- Hooks into 'gradle eclipse', so the Xtend compiler is configured for your project when you import it into Eclipse

Usage
------

Add the plugins to your build classpath

```groovy
buildscript {
  repositories {
    mavenCentral()
  }
  dependencies {
    classpath 'org.xtend:xtend-gradle-plugin:0.1.+'
  }
}
```

For normal Java projects add 

```groovy
apply plugin: 'xtend'
```

This will automatically apply the 'java' and 'eclipse' plugins, too.
  
If you are using the new Android build system, add

```groovy
apply plugin: 'xtend-android'
```

This will not apply anything else, because there are different android plugins for apps and libraries. Just choose yourself. Also this will not generate Eclipse metadata, since the new Android build system is currently only supported by Android Studio.
    
Now you just need xtend.lib and start coding.

```groovy
repositories {
  mavenCentral()
}

dependencies {
  compile 'org.eclipse.xtend:org.eclipse.xtend.lib:2.6.+'
}
```
    
You can change compiler options through the Xtend DSL object

```groovy
xtend {
  fork = true
  useDaemon = true
  xtendAsPrimaryDebugSource = true
  hideSyntheticVariables = false
  encoding = "UTF-16"
}
```

And the target folder per source set. The default is ```build/xtend-gen/${source set name}```

```groovy
sourceSets {
  main.xtendOutputDir = 'xtend-gen'
  test.xtendOutputDir = 'test/xtend-gen'
}
```

Reducing the runtime footprint
------------------------------

If you develop for Android or some other restrictive platform where the number of classes or methods matter a lot, then there are several ways to reduce the runtime footprint of your Xtend projects.

If your project does not define new active annotations, then you don't need the macro API from xtend.lib. You can use the smaller xbase.lib instead:

```groovy
dependencies {
  compile 'org.eclipse.xtext:org.eclipse.xtext.xbase.lib:2.6.+'
}
```

If your code and all of its dependencies work without Google Guava, then you can use xbase.lib.slim, which comes with an inlined and stripped down version of Guava.

```groovy
dependencies {
  compile 'org.eclipse.xtext:org.eclipse.xtext.xbase.lib.slim:2.6.+'
}
```

If you have dependencies which only contain active annotations, you can put them on the xtendCompileOnly classpath. Active annotations are expanded at compile time, so they are no longer needed at runtime.

```groovy
dependencies {
  xtendCompileOnly 'com.github.oehme.xtend:xtend-contrib:0.4.5'
}
```

Limitations
-----------

This plugin supports Xtend 2.5.4 and higher. For the android plugin, version 0.10 or higher is supported.

Also, the behaviour and API are still subject to change. Please file issues here if anything doesn't work as you would expect.
