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
    classpath 'org.xtend:xtend-gradle-plugin:0.0.5'
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
  //or testCompile if you only want to use Xtend for some tests
  compile 'org.eclipse.xtend:org.eclipse.xtend.lib:2.5.4'
}
```
    
You can change compiler options through the Xtend DSL object

```groovy
xtend {
  useDaemon = true
  xtendAsPrimaryDebugSource = true
  hideSyntheticVariables = false
  encoding = "UTF-16"
}
```

Limitations
-----------

This is a very early version which only works with the [2.5.4, 2.6.0) versions of Xtend. Also, the behaviour and API are all subject to change. Please file issues here if anything doesn't work as you would expect.
