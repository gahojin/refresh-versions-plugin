/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions

import jp.co.gahojin.refreshVersions.dependency.ExtractorDependency
import jp.co.gahojin.refreshVersions.extension.defaultVersionCatalog
import jp.co.gahojin.refreshVersions.extension.register
import jp.co.gahojin.refreshVersions.toml.TomlFile
import kotlinx.coroutines.runBlocking
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class RefreshVersionsTask : DefaultTask() {
    init {
        group = Constants.GROUP
    }

    @get:InputFile
    abstract val versionsTomlFile: Property<File>

    @get:Internal
    abstract val sortSection: Property<Boolean>

    @get:Internal
    abstract val cacheDurationMinutes: Property<Int>

    private val versionCatalog = project.defaultVersionCatalog

    private val dependencies by lazy {
        // 実際に使用されているライブラリやプラグインを抽出
        ExtractorDependency().extract(project)
    }

    @TaskAction
    fun refreshVersions() {
        ConfigHolder.initialize(this)

        // バージョンカタログに定義されている情報を取得
        val versionCatalog = versionCatalog ?: run {
            logger.lifecycle("versionsCatalog disabled.")
            return
        }

        // TOMLファイルに定義されている情報を取得
        val file = requireNotNull(versionsTomlFile.orNull)
        val toml = if (file.exists()) {
            TomlFile.parseToml(file.readText())
        } else {
            logger.lifecycle("versionsCatalog file not found.")
            return
        }
        logger.lifecycle("toml loaded. $toml")

        // configuration構文で定義した依存を抽出
        runBlocking {
            // バージョンカタログにあり、実際に使用されているものに絞り込む
            val libraryDependencies = versionCatalog.libraries().withDependencies(dependencies)
            val pluginDependencies = versionCatalog.plugins().withDependencies(dependencies)

            // ライブラリとプラグインの最新のバージョン情報をダウンロード
            val lookupVersionsCandidates = LookupVersionsCandidates(cacheDurationMinutes.get(), logger)
            lookupVersionsCandidates.execute(libraryDependencies)
                .forEach {
                    logger.lifecycle("fetch versions: ${it.dependency} > ${it.updatableVersions.joinToString()}")
                }
            lookupVersionsCandidates.execute(pluginDependencies)
                .forEach {
                    logger.lifecycle("fetch versions: ${it.dependency} > ${it.updatableVersions.joinToString()}")
                }
        }

        // TODO
        // バージョンカタログファイルから以前のバージョン一覧を削除 (cleanタスクと同じ動作)
        // バージョンカタログファイルに、バージョンの候補コメントを追加
    }

    companion object {
        private const val TASK_NAME = "refreshVersions"

        fun register(project: Project, extensions: RefreshVersionsExtension) {
            project.tasks.register<RefreshVersionsTask>(TASK_NAME) {
                it.versionsTomlFile.set(extensions.getVersionsTomlFile())
                it.sortSection.set(extensions.sortSection)
                it.cacheDurationMinutes.set(extensions.cacheDurationMinutes)
            }
        }
    }
}
