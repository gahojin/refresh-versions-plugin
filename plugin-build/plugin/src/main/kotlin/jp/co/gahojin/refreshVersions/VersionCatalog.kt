/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions

import jp.co.gahojin.refreshVersions.model.Dependency
import jp.co.gahojin.refreshVersions.model.PluginDependencyCompat
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionConstraint
import kotlin.jvm.optionals.getOrNull

internal fun VersionCatalog.versions(): Map<String, VersionConstraint> {
    return versionAliases.mapNotNull {
        val version = findVersion(it).getOrNull() ?: return@mapNotNull null
        it to version
    }.associate { it }
}

internal fun VersionCatalog.plugins(): Set<PluginDependencyCompat> {
    return pluginAliases.asSequence()
        .mapNotNull { findPlugin(it).getOrNull()?.orNull }
        .map { PluginDependencyCompat(it) }
        .toSet()

}

internal fun VersionCatalog.libraries(): Set<Dependency> {
    return libraryAliases.asSequence()
        .mapNotNull { findLibrary(it).getOrNull()?.orNull }
        .filter { it.version != null }
        .mapNotNull { Dependency.from(it) }
        .toSet()
}
