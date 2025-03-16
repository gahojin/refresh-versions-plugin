/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions

import jp.co.gahojin.refreshVersions.extension.contentFilter
import jp.co.gahojin.refreshVersions.extension.defaultVersionCatalog
import jp.co.gahojin.refreshVersions.extension.register
import jp.co.gahojin.refreshVersions.extension.repositoriesWithGlobal
import jp.co.gahojin.refreshVersions.extension.repositoriesWithPlugin
import jp.co.gahojin.refreshVersions.model.Dependency
import jp.co.gahojin.refreshVersions.model.Repository
import jp.co.gahojin.refreshVersions.model.maven
import jp.co.gahojin.refreshVersions.toml.TomlFile
import kotlinx.coroutines.runBlocking
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.repositories.ArtifactRepository
import org.gradle.api.internal.artifacts.repositories.DefaultMavenArtifactRepository
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

    private val repositoryWithGlobal = project.repositoriesWithGlobal.mapNotNull { Repository.from(it) }

    private val pluginRepository by lazy {
        ConfigHolder.pluginRepositories.mapNotNull { Repository.from(it) }
    }

    private val dependencies = recordBuildscriptAndRegularDependencies(project)

    @TaskAction
    fun refreshVersions() {
        ConfigHolder.initialize(this)

        val versionCatalog = versionCatalog ?: run {
            logger.lifecycle("versionsCatalog disabled.")
            return
        }
        val file = requireNotNull(versionsTomlFile.orNull)
        val toml = if (file.exists()) {
            TomlFile.parseToml(file.readText())
        } else {
            logger.lifecycle("versionsCatalog file not found.")
            return
        }
        logger.lifecycle("toml loaded. ${toml.sections.values.flatten().joinToString("\n")}")

        // configuration構文で定義した依存を抽出
        runBlocking {
            // 実際に使用されているライブラリやプラグインを抽出
            dependencies
                .forEach {
                    logger.lifecycle("${it.first} : ${it.second.joinToString()}")
                }

            // ライブラリとプラグインの最新のバージョン情報をダウンロード
            LookupVersionsCandidates(cacheDurationMinutes.get(), logger)
                .executeLibrary(repositoryWithGlobal.maven(), versionCatalog.libraries())
                .forEach {
                    logger.lifecycle("fetch versions: ${it.dependency} > ${it.updatableVersions.joinToString()}")
                }

            LookupVersionsCandidates(cacheDurationMinutes.get(), logger)
                .executePlugin(pluginRepository.maven(), versionCatalog.plugins())
                .forEach {
                    logger.lifecycle("fetch versions: ${it.dependency} > ${it.updatableVersions.joinToString()}")
                }
        }

        // バージョンカタログに定義されている情報を取得
        logger.lifecycle("versions ${versionCatalog.versions().entries.joinToString { "${it.key}:${it.value}" }}")
        logger.lifecycle("libraries ${versionCatalog.libraries().joinToString { "${it.group}:${it.name}:${it.versionConstraint}" }}")
        logger.lifecycle("plugins ${versionCatalog.plugins().joinToString { "${it.pluginId}:${it.version}" }}")

        // TODO
        // バージョンカタログに定義されているうち、実際に使用されているものに絞り込む
        // 絞り込んだライブラリ・プラグインのバージョン情報を取得する
        // 現在のバージョンより新しいバージョンを抽出
        // バージョンカタログファイルから以前のバージョン一覧を削除 (cleanタスクと同じ動作)
        // バージョンカタログファイルに、バージョンの候補コメントを追加
    }

    fun recordBuildscriptAndRegularDependencies(rootProject: Project): List<Pair<Dependency, List<Repository>>> {
        val allDependencies = mutableListOf<Pair<Dependency, List<Repository>>>()
        rootProject.allprojects {
            var dependencies = extractDependencies(it.configurations, it.repositoriesWithGlobal)
            allDependencies.addAll(dependencies)

            dependencies = extractDependencies(it.buildscript.configurations, it.repositoriesWithPlugin)
            allDependencies.addAll(dependencies)
        }
        return allDependencies
    }

    private fun extractDependencies(
        configurations: ConfigurationContainer,
        repositories: List<ArtifactRepository>,
    ): Sequence<Pair<Dependency, List<Repository>>> {
        // TODO 仮実装
        return configurations
            .asSequence()
            .flatMap {
                it.dependencies.asSequence()
            }.mapNotNull { rawDependency ->
                val dependency = Dependency.from(rawDependency) ?: return@mapNotNull null

                // リポジトリの制約を適用する
                val filteredRepositories = repositories.filterIsInstance<DefaultMavenArtifactRepository>().filter {
                    val details = dependency.asArtifactResolutionDetails()
                    it.contentFilter(details)
                    details.found
                }
                dependency to filteredRepositories.mapNotNull { Repository.from(it) }
            }
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
