pluginManagement {
    repositories {
//        mavenLocal()
        gradlePluginPortal()
    }
    includeBuild("../plugin-build")
}

plugins {
    id("jp.co.gahojin.refreshVersions")
//    id("jp.co.gahojin.refreshVersions") version "0.0.1"
}

dependencyResolutionManagement {
    repositories {
        google {
            content {
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
                includeGroupAndSubgroups("androidx")
            }
        }
        mavenCentral()
        mavenLocal()
    }
    versionCatalogs {
        register("libs") {
            from(files("gradle/custom.versions.toml"))
        }
    }
}

refreshVersions {
    cacheDurationMinutes = 120
    versionsTomlFile = file("gradle/custom.versions.toml")
}
