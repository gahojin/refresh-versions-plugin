/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions

import jp.co.gahojin.refreshVersions.model.Dependency
import jp.co.gahojin.refreshVersions.model.DependencyWithRepository
import org.gradle.api.artifacts.VersionCatalog
import kotlin.jvm.optionals.getOrNull

fun VersionCatalog.versions() = versionAliases.mapNotNull {
    val version = findVersion(it).getOrNull() ?: return@mapNotNull null
    it to version
}.associate { it }

fun VersionCatalog.plugins() = pluginAliases.asSequence()
    .mapNotNull { findPlugin(it).getOrNull()?.orNull }
    .map { Dependency.from(it) }
    .toSet()

fun VersionCatalog.libraries() = libraryAliases.asSequence()
    .mapNotNull { findLibrary(it).getOrNull()?.orNull }
    .mapNotNull { Dependency.from(it as org.gradle.api.artifacts.Dependency) }
    .toSet()

fun Set<Dependency>.withDependencies(
    dependencies: List<DependencyWithRepository>,
): List<DependencyWithRepository> {
    val modules = map { it.moduleId to it.version }.toSet()
    return dependencies.sortedBy { it.dependency }.filter { (dependency, _) ->
        modules.contains(dependency.moduleId to dependency.version)
    }
}
