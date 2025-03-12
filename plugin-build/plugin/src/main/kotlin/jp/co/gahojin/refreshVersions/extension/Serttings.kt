/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions.extension

import org.gradle.api.artifacts.repositories.ArtifactRepository
import org.gradle.api.initialization.Settings

val Settings.globalRepositories: List<ArtifactRepository>
    get() = runCatching {
        dependencyResolutionManagement.repositories
    }.getOrDefault(emptyList())

val Settings.defaultCatalogName: String
    get() = runCatching {
        dependencyResolutionManagement.defaultLibrariesExtensionName.get()
    }.getOrDefault("libs")
