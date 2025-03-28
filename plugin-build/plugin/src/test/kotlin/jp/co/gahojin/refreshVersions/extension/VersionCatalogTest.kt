/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions.extension

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import jp.co.gahojin.refreshVersions.model.Dependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.testfixtures.ProjectBuilder
import java.util.Optional

class VersionCatalogTest : StringSpec({
    "plugins" {
        val project = ProjectBuilder.builder().build()
        val aliasCapture = slot<String>()
        val sut = mockk<VersionCatalog> {
            every { pluginAliases } returns listOf(
                "plugin.name",
                "plugin.name2",
                "plugin2.name",
            )
            every { findPlugin(capture(aliasCapture)) } answers {
                val answerAlias = aliasCapture.captured
                val answerVersion = when (answerAlias) {
                    "plugin.name" -> "0.0.1"
                    "plugin.name2" -> "0.1.0"
                    else -> null
                }

                Optional.ofNullable(answerVersion?.let {
                    project.provider {
                        mockk {
                            every { pluginId } returns answerAlias
                            every { version } returns mockk {
                                every { requiredVersion } returns it
                            }
                        }
                    }
                })
            }
        }

        sut.plugins() shouldBe setOf(
            Dependency.plugin("plugin.name", version = "0.0.1"),
            Dependency.plugin("plugin.name2", version = "0.1.0"),
        )
    }

    "libraries" {
        val project = ProjectBuilder.builder().build()
        val aliasCapture = slot<String>()
        val sut = mockk<VersionCatalog> {
            every { libraryAliases } returns listOf(
                "group:name",
                "group:name2",
                "group2:name",
            )
            every { findLibrary(capture(aliasCapture)) } answers {
                val answerAlias = aliasCapture.captured
                val (answerGroup, answerName, answerVersion) = when (answerAlias) {
                    "group:name" -> Triple("group", "name", "0.0.1")
                    "group:name2" -> Triple("group", "name2", "0.1.0")
                    else -> Triple("", "", null)
                }

                Optional.ofNullable(answerVersion?.let {
                    project.provider {
                        mockk {
                            every { group } returns answerGroup
                            every { name } returns answerName
                            every { version } returns answerVersion
                        }
                    }
                })
            }
        }

        sut.libraries() shouldBe setOf(
            Dependency.module("group", "name", version = "0.0.1"),
            Dependency.module("group", "name2", version = "0.1.0"),
        )
    }
})
