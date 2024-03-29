JDrupes JSON library
==================

[![Build Status](https://travis-ci.org/mnlipp/jdrupes-json.svg?branch=master)](https://travis-ci.org/mnlipp/jdrupes-json) 
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/0d9e648d1d904ec6a1f0ca713ca30c5c)](https://www.codacy.com/app/mnlipp/jdrupes-json?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=mnlipp/jdrupes-json&amp;utm_campaign=Badge_Grade)
[![Code Climate](https://lima.codeclimate.com/github/mnlipp/jdrupes-json/badges/gpa.svg)](https://lima.codeclimate.com/github/mnlipp/jdrupes-json)
[![Codebeat Badge](https://codebeat.co/badges/1f6b6ece-4436-400e-b457-59331bb758e5)](https://codebeat.co/projects/github-com-mnlipp-jdrupes-json-master)
[![Release](https://jitpack.io/v/mnlipp/jdrupes-json.svg)](https://jitpack.io/#mnlipp/jdrupes-json)
[![Maven Central](https://img.shields.io/maven-central/v/org.jdrupes.json/json.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.jdrupes.json%22)
[![Javadocs](https://www.javadoc.io/badge/org.jdrupes.json/json.svg)](https://www.javadoc.io/doc/org.jdrupes.json/json)

The goal of this package is to provide a JSON encoder and decoder for JavaBeans
and maps or collections of primitive types (and JavaBeans) as an OSGi 
compliant utility library using the 
[Jackson core library](https://github.com/FasterXML/jackson-core).

This library requires Java 11 SE. Binaries are currently made
available at maven central.

```gradle
repositories {
	mavenCentral()
}

dependencies {
	compile 'org.jdrupes.json:json:X.Y.Z'
}
```

(See badge above for the latest version.) 

The best starting point for using the library is to have a look at the Javadoc (either 
[properly versioned](https://www.javadoc.io/doc/org.jdrupes.json/json/)
or [close to master](https://mnlipp.github.io/jdrupes-json/javadoc/index.html)).

Contributions and bug reports are welcome. Please provide them as
GitHub issues.
