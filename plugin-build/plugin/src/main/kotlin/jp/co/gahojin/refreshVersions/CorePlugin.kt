/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import org.gradle.api.invocation.Gradle

/**
 * プラグインエンドポイント.
 */
@Suppress("unused")
abstract class CorePlugin : Plugin<Any> {
    override fun apply(target: Any) {
        // settings.gradle(.kts)以外では使用不可とする
        require(target is Settings) {
            val notInExtraClause = when (target) {
                is Project -> when (target) {
                    target.rootProject -> ", not in build.gradle(.kts)"
                    else -> ", not in a build.gradle(.kts) file."
                }
                is Gradle -> ", not in an initialization script."
                else -> ""
            }
            """
            plugins.id(${Constants.PLUGIN_ID}") must be configured in settings.gradle(.kts)$notInExtraClause.
            """.trimIndent()
        }

        val extensions = RefreshVersionsExtension.create(target)

        bootstrap(target, extensions)
    }

    private fun bootstrap(settings: Settings, extensions: RefreshVersionsExtension) {
        ConfigHolder.initialize(settings)

        settings.gradle.settingsEvaluated {
            settings.gradle.rootProject {
                RefreshVersionsTask.register(it, extensions)
                RefreshVersionsCleanupTask.register(it, extensions)
            }
        }
    }
}
