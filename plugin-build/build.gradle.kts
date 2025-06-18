// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.dokka.javadoc) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlinx.kover) apply false
    alias(libs.plugins.maven.publish) apply false
}

// FIXME Dokkaにより、脆弱な依存関係が取り込まれている
// https://github.com/Kotlin/dokka/issues/3194
allprojects {
    configurations.all {
        resolutionStrategy {
            force(libs.fastxml.jackson.databind)
            force(libs.fastxml.woodstox)
        }
    }
}
