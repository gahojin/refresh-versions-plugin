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
        val allDependencies = mutableListOf<DependencyWithRepository>()
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
    ): Sequence<DependencyWithRepository> = sequence {
        // dependenciesの処理
        configurations
            .asSequence()
            .flatMap { it.dependencies.asSequence() }
            // 処理出来ない依存は無視する
            .mapNotNull { Dependency.from(it) }
            // リポジトリの制約を適用する
            .resolutionRepositories(repositories)
            .forEach {
                yield(it)
            }

        // resolutionStrategyの処理
        configurations
            .asSequence()
            .flatMap { it.resolutionStrategy.forcedModules.asSequence() }
            .map { Dependency.from(it) }
            // リポジトリの制約を適用する
            .resolutionRepositories(repositories)
            .forEach {
                yield(it)
            }
    }

    private fun Sequence<Dependency>.resolutionRepositories(
        repositories: List<ArtifactRepository>,
    ): Sequence<DependencyWithRepository> = sorted().distinct().map { dependency ->
        // リポジトリの制約を適用する
        val filteredRepositories = repositories.filter {
            val details = dependency.asArtifactResolutionDetails()
            it.contentFilter(details)
            details.found
        }
        DependencyWithRepository(
            dependency = dependency,
            repositories = filteredRepositories.mapNotNull { Repository.from(it) },
        )
    }
}
