semversa
========

Gradle plug-in to generate version information from the SCM branch.

## Applying the plug-in

```kotlin
plugins {
   id("org.emergent.semversa") version "1.0.0"
}
```

## Using the versioning info

```kotlin
version = project.extra["version"]
```

For a multi-module project, you will probably do:

```kotlin
allprojects {
    version = project.extra["version"]
}
```