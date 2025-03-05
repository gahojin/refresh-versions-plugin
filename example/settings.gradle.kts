pluginManagement {
//    repositories {
//        mavenLocal()
//        gradlePluginPortal()
//    }
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
    }
}

refreshVersions {
//    versionsTomlFile = file("gradle/lib.versions.toml")
}
