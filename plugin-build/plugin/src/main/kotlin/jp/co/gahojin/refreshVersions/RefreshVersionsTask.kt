/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions

import jp.co.gahojin.refreshVersions.dependency.MavenMetadataParser
import jp.co.gahojin.refreshVersions.extension.defaultVersionCatalog
import jp.co.gahojin.refreshVersions.extension.register
import jp.co.gahojin.refreshVersions.extension.repositoriesWithGlobal
import jp.co.gahojin.refreshVersions.model.Dependency
import jp.co.gahojin.refreshVersions.model.Repository
import jp.co.gahojin.refreshVersions.model.maven
import jp.co.gahojin.refreshVersions.toml.TomlFile
import kotlinx.coroutines.runBlocking
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.ConfigurationContainer
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

    private val repositoryWithGlobal = project.repositoriesWithGlobal

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
            dependencies
                .forEach {
                    logger.lifecycle("${it.first} : ${it.second.joinToString()}")
                }

            LookupVersionsCandidates(cacheDurationMinutes.get())
                .execute(repositoryWithGlobal.maven(), versionCatalog.libraries(), emptySet())
                .filterNotNull()
                .forEach {
                    logger.lifecycle("versions: ${MavenMetadataParser.parse(it).joinToString()}")
                }
        }
        logger.lifecycle("versions ${versionCatalog.versions().entries.joinToString { "${it.key}:${it.value}" }}")
        logger.lifecycle("libraries ${versionCatalog.libraries().joinToString { "${it.group}:${it.name}:${it.versionConstraint}, ${versionCatalog.versions().values.find {v -> it.versionConstraint == v }} " }}")
        logger.lifecycle("plugins ${versionCatalog.plugins().joinToString { "${it.pluginId}:${it.version}" }}")
    }

    fun recordBuildscriptAndRegularDependencies(rootProject: Project): List<Pair<Dependency, List<Repository>>> {
        val allDependencies = mutableListOf<Pair<Dependency, List<Repository>>>()
        rootProject.allprojects {
            val repositories = it.repositoriesWithGlobal
            val dependencies = extractDependencies(it.configurations, repositories)
            allDependencies.addAll(dependencies)
        }
        return allDependencies
    }

    private fun extractDependencies(
        configurations: ConfigurationContainer,
        repositories: List<Repository>,
    ): Sequence<Pair<Dependency, List<Repository>>> {
        // TODO 仮実装
        return configurations
            .asSequence()
            .flatMap {
                it.dependencies.asSequence()
            }.mapNotNull { rawDependency ->
                val dependency = Dependency.from(rawDependency) ?: return@mapNotNull null

                // TODO リポジトリの制約を使用する
                dependency to repositories
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
