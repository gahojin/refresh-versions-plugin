/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions.dependency

import jp.co.gahojin.refreshVersions.extension.contentFilter
import jp.co.gahojin.refreshVersions.extension.repositoriesWithGlobal
import jp.co.gahojin.refreshVersions.extension.repositoriesWithPlugin
import jp.co.gahojin.refreshVersions.model.Dependency
import jp.co.gahojin.refreshVersions.model.DependencyWithRepository
import jp.co.gahojin.refreshVersions.model.Repository
import org.gradle.api.Project
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.repositories.ArtifactRepository

class ExtractorDependency {
    fun extract(rootProject: Project): List<DependencyWithRepository> {
        val allDependencies = mutableMapOf<Dependency, MutableSet<Repository>>()
        rootProject.allprojects {
            extractDependencies(allDependencies, it.configurations, it.repositoriesWithGlobal + it.repositoriesWithPlugin)
            extractDependencies(allDependencies, it.buildscript.configurations, it.repositoriesWithPlugin)
        }

        return allDependencies.entries.map { (dependency, repositories) ->
            DependencyWithRepository(dependency, repositories.toList())
        }
    }

    private fun extractDependencies(
        destination: MutableMap<Dependency, MutableSet<Repository>>,
        configurations: ConfigurationContainer,
        repositories: List<ArtifactRepository>,
    ) {
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
            .map { Dependency.from(it) }
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
