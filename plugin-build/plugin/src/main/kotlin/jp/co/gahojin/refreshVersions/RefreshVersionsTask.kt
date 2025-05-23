/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions

import jp.co.gahojin.refreshVersions.dependency.ExtractorDependency
import jp.co.gahojin.refreshVersions.extension.debug
import jp.co.gahojin.refreshVersions.extension.defaultVersionCatalog
import jp.co.gahojin.refreshVersions.extension.getSettingsFile
import jp.co.gahojin.refreshVersions.extension.libraries
import jp.co.gahojin.refreshVersions.extension.plugins
import jp.co.gahojin.refreshVersions.extension.register
import jp.co.gahojin.refreshVersions.internal.SettingsUpdater
import jp.co.gahojin.refreshVersions.internal.VersionCatalogUpdater
import jp.co.gahojin.refreshVersions.model.withDependencies
import kotlinx.coroutines.runBlocking
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class RefreshVersionsTask : DefaultTask() {
    init {
        group = Constants.GROUP
    }

    @get:InputFile
    abstract val versionsTomlFile: Property<File>

    @get:InputFile
    @get:Optional
    abstract val rootSettingsFile: Property<File>

    @get:InputFile
    @get:Optional
    abstract val buildSrcSettingsFile: Property<File>

    @get:Internal
    abstract val sortSection: Property<Boolean>

    @get:Internal
    abstract val cacheDurationMinutes: Property<Int>

    private val versionCatalog = project.defaultVersionCatalog

    private val dependencies by lazy {
        // 実際に使用されているライブラリやプラグインを抽出
        ExtractorDependency().extract(project, logger)
    }

    private val settingsPluginDependencies by lazy {
        ExtractorDependency().extract(ConfigHolder.settings, logger)
    }

    @TaskAction
    fun refreshVersions() {
        logger.debug {
            buildString {
                appendLine("initialize task...")
                appendLine("  versionsTomlFile: ${versionsTomlFile.orNull}")
                appendLine("  rootSettingsFile: ${rootSettingsFile.orNull}")
                appendLine("  buildSrcSettingsFile: ${buildSrcSettingsFile.orNull}")
                appendLine("  sortSection: ${sortSection.orNull}")
                appendLine("  cacheDurationMinutes: ${cacheDurationMinutes.orNull}")
            }
        }
        ConfigHolder.initialize(this)

        // バージョンカタログに定義されている情報を取得
        val versionCatalog = versionCatalog ?: run {
            logger.warn("versionsCatalog disabled.")
            return
        }

        // configuration構文で定義した依存を抽出
        runBlocking {
            logger.debug {
                buildString {
                    appendLine("fetch dependencies from gradle.")
                    append("dependencies: ").appendLine(dependencies.joinToString(separator = ", "))
                    append("settingsPluginDependencies: ").appendLine(settingsPluginDependencies.joinToString(separator = ", "))
                    append("versionCatalog(libraries): ").appendLine(versionCatalog.libraries().joinToString(separator = ", "))
                    append("versionCatalog(plugins): ").appendLine(versionCatalog.plugins().joinToString(separator = ", "))
                }
            }

            // バージョンカタログにあり、実際に使用されているものに絞り込む
            val libraryDependencies = versionCatalog.libraries().withDependencies(dependencies)
            val pluginDependencies = versionCatalog.plugins().withDependencies(dependencies)

            // ライブラリとプラグインの最新のバージョン情報をダウンロード
            val lookupVersionsCandidates = LookupVersionsCandidates(
                cacheDurationMinutes = cacheDurationMinutes.get(),
                logger = logger,
            )
            val libraryUpdatableDependencies = lookupVersionsCandidates.execute(libraryDependencies)
            val pluginUpdatableDependencies = lookupVersionsCandidates.execute(pluginDependencies)
            val settingsPluginUpdatableDependencies = lookupVersionsCandidates.execute(settingsPluginDependencies)

            logger.debug {
                buildString {
                    appendLine("after lookup versions.")
                    append("library: ").appendLine(libraryUpdatableDependencies.joinToString(separator = ", "))
                    append("plugin: ").appendLine(pluginUpdatableDependencies.joinToString(separator = ", "))
                    append("settingPlugin: ").appendLine(settingsPluginUpdatableDependencies.joinToString(separator = ", "))
                }
            }

            VersionCatalogUpdater.execute(
                file = versionsTomlFile.get(),
                libraryDependencies = libraryUpdatableDependencies,
                pluginDependencies = pluginUpdatableDependencies,
                sortSection = sortSection.getOrElse(false),
            )

            // settings.gradle(.kts)ファイルを更新
            SettingsUpdater.execute(
                files = listOf(rootSettingsFile, buildSrcSettingsFile).mapNotNull { it.orNull },
                dependencies = settingsPluginUpdatableDependencies,
            )
        }
    }

    companion object {
        private const val TASK_NAME = "refreshVersions"

        @JvmStatic
        fun register(project: Project, extensions: RefreshVersionsExtension) {
            project.tasks.register<RefreshVersionsTask>(TASK_NAME) {
                it.versionsTomlFile.set(extensions.getVersionsTomlFile(project))
                it.sortSection.set(extensions.sortSection)
                it.cacheDurationMinutes.set(extensions.cacheDurationMinutes)
                it.rootSettingsFile.set(project.getSettingsFile())
                it.buildSrcSettingsFile.set(project.getSettingsFile("buildSrc/"))
            }
        }
    }
}
