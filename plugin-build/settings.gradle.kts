plugins {
    id("jp.co.gahojin.refreshVersions") version "0.0.4"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

refreshVersions {
    sortSection = true
}

rootProject.name = "jp.co.gahojin.refreshVersions"
include(":plugin")
