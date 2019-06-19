# Maven archetypes

[![License](https://img.shields.io/badge/license-Apache%202-blue.svg?style=flat-square)](https://www.apache.org/licenses/LICENSE-2.0)
[![License](https://img.shields.io/maven-central/v/io.github.openfrog/maven-super-pom.svg?color=blue&style=flat-square)](https://search.maven.org/search?q=g:io.github.openfrog%20AND%20a:maven-super-pom)

## Maven Super POM

## Application archetype

```bash
mvn archetype:generate \
    -DarchetypeGroupId=io.github.openfrog \
    -DarchetypeArtifactId=maven-application-archetype \
    -DarchetypeVersion=${version} \
    -DarchetypeCatalog=local \
    -DinteractiveMode=false \    
    -DgroupId=io.github.openfrog \
    -DartifactId=sample
```

## Multi-module archetype

```bash
mvn archetype:generate \
    -DarchetypeGroupId=io.github.openfrog \
    -DarchetypeArtifactId=maven-multi-module-archetype \
    -DarchetypeVersion=${version} \
    -DarchetypeCatalog=local \
    -DinteractiveMode=false \    
    -DgroupId=io.github.openfrog \
    -DartifactId=multi-module-sample
```
