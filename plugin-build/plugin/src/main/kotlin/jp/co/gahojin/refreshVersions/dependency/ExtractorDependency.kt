/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions.dependency

import jp.co.gahojin.refreshVersions.extension.contentFilter
import jp.co.gahojin.refreshVersions.extension.debug
import jp.co.gahojin.refreshVersions.extension.dependencies
import jp.co.gahojin.refreshVersions.extension.pluginRepositories
import jp.co.gahojin.refreshVersions.extension.repositoriesWithGlobal
import jp.co.gahojin.refreshVersions.extension.repositoriesWithPlugin
import jp.co.gahojin.refreshVersions.model.Dependency
import jp.co.gahojin.refreshVersions.model.DependencyWithRepository
import jp.co.gahojin.refreshVersions.model.Repository
import org.gradle.api.Project
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.repositories.ArtifactRepository
import org.gradle.api.initialization.Settings
import org.gradle.api.logging.Logger

class ExtractorDependency {
    fun extract(rootProject: Project, logger: Logger): List<DependencyWithRepository> {
        logger.debug {
            buildString {
                appendLine("extracting dependencies")
                appendLine("repositories:")
                rootProject.allprojects {
                    append("  ").append(it.displayName).append(" > ").appendLine(it.repositoriesWithGlobal.joinToString(", ") { it.name })
                }
                appendLine()
            }
        }

        val allDependencies = mutableMapOf<Dependency, MutableSet<Repository>>()
        rootProject.allprojects {
            extractDependencies(it, allDependencies, it.configurations, it.repositoriesWithGlobal, logger)
            extractDependencies(it, allDependencies, it.buildscript.configurations, it.repositoriesWithPlugin, logger)
        }

        return allDependencies.entries.map { (dependency, repositories) ->
            DependencyWithRepository(dependency, repositories.toList())
        }
    }

    fun extract(settings: Settings, logger: Logger): List<DependencyWithRepository> {
        val allDependencies = mutableMapOf<Dependency, MutableSet<Repository>>()
        extractDependencies(allDependencies, settings, settings.pluginRepositories, logger)

        return allDependencies.entries.map { (dependency, repositories) ->
            DependencyWithRepository(dependency, repositories.toList())
        }
    }

    private fun extractDependencies(
        project: Project,
        destination: MutableMap<Dependency, MutableSet<Repository>>,
        configurations: ConfigurationContainer,
        repositories: List<ArtifactRepository>,
        logger: Logger,
    ) {
        logger.debug {
            buildString {
                append("project(").append(project.name).appendLine(')')
                append("  configurations: ").appendLine(configurations.joinToString(","))
                append("  repositories: ").appendLine(repositories.joinToString(",") { it.name })
                appendLine()
            }
        }

        // dependenciesの処理
        configurations
            .asSequence()
            .flatMap { it.dependencies.asSequence() }
            // 処理出来ない依存は無視する
            .mapNotNull { Dependency.from(it) }
            // リポジトリの制約を適用する
            .resolutionRepositories(destination, repositories)

        // resolutionStrategyの処理
        configurations
            .asSequence()
            .flatMap { it.resolutionStrategy.forcedModules.asSequence() }
            .mapNotNull { Dependency.from(it) }
            // リポジトリの制約を適用する
            .resolutionRepositories(destination, repositories)
    }

    private fun extractDependencies(
        destination: MutableMap<Dependency, MutableSet<Repository>>,
        settings: Settings,
        repositories: List<ArtifactRepository>,
        logger: Logger,
    ) {
        logger.debug {
            buildString {
                appendLine("settings:")
                append("  dependencies: ").appendLine(settings.dependencies().joinToString(","))
                append("  repositories: ").appendLine(repositories.joinToString(",") { it.name })
                appendLine()
            }
        }

        // dependenciesの処理
        settings.dependencies()
            // 処理出来ない依存は無視する
            .mapNotNull { Dependency.from(it) }
            // リポジトリの制約を適用する
            .resolutionRepositories(destination, repositories)
    }

    private fun Sequence<Dependency>.resolutionRepositories(
        destination: MutableMap<Dependency, MutableSet<Repository>>,
        repositories: List<ArtifactRepository>,
    ) {
        map { dependency ->
            // リポジトリの制約を適用する
            val filteredRepositories = repositories.filter {
                val details = dependency.asArtifactResolutionDetails()
                it.contentFilter(details)
                details.found
            }
            dependency to filteredRepositories.mapNotNull { Repository.from(it) }
        }.forEach { (dependency, repositories) ->
            destination.getOrPut(dependency) {
                mutableSetOf()
            }.addAll(repositories)
        }
    }
}
