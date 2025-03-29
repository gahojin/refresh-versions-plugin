/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions.extension

import jp.co.gahojin.refreshVersions.Constants.PLUGIN_NAME_SUFFIX
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.repositories.ArtifactRepository
import org.gradle.api.initialization.Settings

val Settings.globalRepositories: List<ArtifactRepository>
    get() = runCatching {
        dependencyResolutionManagement.repositories
    }.getOrDefault(emptyList())

val Settings.pluginRepositories: List<ArtifactRepository>
    get() = pluginManagement.repositories.asMap.values.toList()

val Settings.defaultCatalogName: String
    get() = runCatching {
        dependencyResolutionManagement.defaultLibrariesExtensionName.get()
    }.getOrDefault("libs")

fun Settings.dependencies(): Sequence<Dependency> {
    // settingsに定義されているプラグイン情報が直接取得出来ないため、classpathから抽出する
    return buildscript.configurations.getByName("classpath").dependencies.asSequence()
        .filter {
            // 依存名が、pluginId:pluginId.gradle.pluginとなっているものに絞り込む
            val group = it.group ?: return@filter false
            return@filter it.name.endsWith(PLUGIN_NAME_SUFFIX) &&
                group == it.name.substringBefore(PLUGIN_NAME_SUFFIX) &&
                pluginManager.hasPlugin(group)
        }
}
