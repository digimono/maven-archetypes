# Maven archetypes

[![License](https://img.shields.io/badge/license-Apache%202-blue.svg?style=flat-square)](https://www.apache.org/licenses/LICENSE-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.digimono/maven-super-pom.svg?color=blue&style=flat-square)](https://search.maven.org/search?q=g:io.github.digimono%20AND%20a:maven-super-pom)

## Maven Super POM

## Installation

```bash
mvn clean install
```

## Application archetype

```bash
mvn archetype:generate \
    -DarchetypeGroupId=io.github.digimono \
    -DarchetypeArtifactId=maven-application-archetype \
    -DarchetypeVersion=${version} \
    -DarchetypeCatalog=local \
    -DinteractiveMode=false \
    -DgroupId=io.github.digimono \
    -DartifactId=spring-boot-scaffold
```

## Multi-module archetype

```bash
mvn archetype:generate \
    -DarchetypeGroupId=io.github.digimono \
    -DarchetypeArtifactId=maven-multi-module-archetype \
    -DarchetypeVersion=${version} \
    -DarchetypeCatalog=local \
    -DinteractiveMode=false \
    -DgroupId=io.github.digimono \
    -DartifactId=spring-boot-scaffold
```
