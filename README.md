# Gradleバージョン更新プラグイン

[![Build checks](https://github.com/gahojin/refresh-versions-plugin/actions/workflows/build.yml/badge.svg)](https://github.com/gahojin/refresh-versions-plugin/actions/workflows/build.yml)
[![Maven Central Version](https://img.shields.io/maven-central/v/jp.co.gahojin.refreshVersions/jp.co.gahojin.refreshVersions.gradle.plugin)](https://central.sonatype.com/artifact/jp.co.gahojin.refreshVersions/jp.co.gahojin.refreshVersions.gradle.plugin)
[![GitHub License](https://img.shields.io/github/license/gahojin/refresh-versions-plugin)](LICENSE)

## Setup

```groovy
// settings.gradle(.kts)
plugins {
    id("jp.co.gahojin.refreshVersions") version "0.3.0"
}
```

## Usage

Currently, Gradle 8.2 or later is supported.

### Find available updates in versions catalog

```shell
./gradlew refreshVersions
```

### Cleanup versions availability comments

```shell
./gradlew refreshVersionsCleanup
```

## License

```
Copyright 2025, GAHOJIN, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
