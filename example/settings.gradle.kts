pluginManagement {
    repositories {
        mavenCentral()
    }
//    includeBuild("../plugin-build")
}

plugins {
//    id("jp.co.gahojin.refreshVersions")
    id("jp.co.gahojin.refreshVersions") version "0.1.0"
}

dependencyResolutionManagement {
    repositories {
        google {
// can be used with gradle 8.1 or later
//            content {
//                includeGroupAndSubgroups("com.android")
//                includeGroupAndSubgroups("com.google")
//                includeGroupAndSubgroups("androidx")
//            }
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

configure<jp.co.gahojin.refreshVersions.RefreshVersionsExtension> {
    cacheDurationMinutes = 120
    versionsTomlFile = file("gradle/custom.versions.toml")
}

// For gradle 8.8 or later
//refreshVersions {
//    cacheDurationMinutes = 120
//    versionsTomlFile = file("gradle/custom.versions.toml")
//}
