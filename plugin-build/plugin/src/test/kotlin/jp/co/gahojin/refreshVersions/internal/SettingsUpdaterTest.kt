/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions.internal

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import jp.co.gahojin.refreshVersions.model.Dependency
import jp.co.gahojin.refreshVersions.model.UpdatableDependency
import jp.co.gahojin.refreshVersions.model.Version

class SettingsUpdaterTest : StringSpec({
    "空" {
        SettingsUpdater.execute("".reader(), emptyList()) shouldBe ""
    }

    "バージョン情報が更新されること" {
        val dependencies = listOf(
            UpdatableDependency(Dependency.plugin("jp.co.gahojin.refreshVersions", "x.y.z"), listOf(Version("0.1.0"), Version("0.1.1"))),
        )
        SettingsUpdater.execute("""
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
            |/* aa */id("jp.co.gahojin.refreshVersions") version '0.0.5' /* a */
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
            |""".trimMargin().replace("\\\"\\\"\\\"", "\"\"\"").reader(), dependencies) shouldBe """
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
                |//                                            ^ '0.1.0'
                |//                                            ^ '0.1.1'
                |/* aa */id("jp.co.gahojin.refreshVersions") version '0.0.5' /* a */
                |//                                                ^ '0.1.0'
                |//                                                ^ '0.1.1'
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
