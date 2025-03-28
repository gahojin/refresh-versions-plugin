/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions.internal

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class SettingsCleanerTest : StringSpec({
    "空" {
        SettingsCleaner.execute("".reader()) shouldBe ""
    }

    "バージョンコメントが削除されること" {
        SettingsCleaner.execute("""
            |pluginManagement {
            |    repositories {
            |       mavenCentral()
            |       gradlePluginPortal()
            |    }
            |}
            |
            |plugins {
            |//                                            ^ "1.0.0" // 残る
            |print("/* id(\"jp.co.gahojin.refreshVersions\") version '0.0.5' */") // 処理されないこと
            |print(\"\"\"
            |    id(\"jp.co.gahojin.refreshVersions\") version '0.0.5' */")
            |\"\"\") // 処理されないこと
            |    id("jp.co.gahojin.refreshVersions") version '0.0.5' // comment
            |/* aa */id("jp.co.gahojin.refreshVersions") version '0.0.5'
            |//                                            ^ "0.0.6" // comment
            |//                                            ^ "0.0.7"
            |//                                            ^ "0.0.8"
            |}
            |
            |dependencyResolutionManagement {
            |    repositories {
            |       google()
            |       mavenCentral()
            |    }
            |}
            |""".trimMargin().replace("\\\"\\\"\\\"", "\"\"\"").reader()) shouldBe """
                |pluginManagement {
                |    repositories {
                |       mavenCentral()
                |       gradlePluginPortal()
                |    }
                |}
                |
                |plugins {
                |//                                            ^ "1.0.0" // 残る
                |print("/* id(\"jp.co.gahojin.refreshVersions\") version '0.0.5' */") // 処理されないこと
                |print(\"\"\"
                |    id(\"jp.co.gahojin.refreshVersions\") version '0.0.5' */")
                |\"\"\") // 処理されないこと
                |    id("jp.co.gahojin.refreshVersions") version '0.0.5' // comment
                |/* aa */id("jp.co.gahojin.refreshVersions") version '0.0.5'
                |}
                |
                |dependencyResolutionManagement {
                |    repositories {
                |       google()
                |       mavenCentral()
                |    }
                |}
                |""".trimMargin().replace("\\\"\\\"\\\"", "\"\"\"")
    }
})
