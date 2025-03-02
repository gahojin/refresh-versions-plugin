pluginManagement {
    includeBuild("../plugin-build")
}

plugins {
    id("jp.co.gahojin.refreshVersions")
}

refreshVersions {
//    versionsTomlFile = file("gradle/lib.versions.toml")
}
