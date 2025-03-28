/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions.extension

import jp.co.gahojin.refreshVersions.ConfigHolder
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.artifacts.repositories.ArtifactRepository
import java.io.File

val Project.defaultVersionCatalog: VersionCatalog?
    get() {
        val versionCatalogs = project.extensions.findByType<VersionCatalogsExtension>()
        val settings = ConfigHolder.settings
        return versionCatalogs?.find(settings.defaultCatalogName)?.orElse(null)
    }

val Project.repositoriesWithGlobal: List<ArtifactRepository>
    get() {
        val globalRepositories = ConfigHolder.globalRepositories
        val repositories = when {
            globalRepositories.isEmpty() -> repositories
            repositories.isEmpty() -> globalRepositories
            else -> globalRepositories + repositories
        }
        return repositories
    }

val Project.repositoriesWithPlugin: List<ArtifactRepository>
    get() {
        val pluginRepositories = ConfigHolder.pluginRepositories
        val repositories = when {
            pluginRepositories.isEmpty() -> repositories
            repositories.isEmpty() -> pluginRepositories
            else -> pluginRepositories + repositories
        }
        return repositories
    }


fun Project.getSettingsFile(prefix: String = ""): File? {
    return file("${prefix}settings.gradle.kts").takeIf { it.exists() } ?: run {
        file("${prefix}settings.gradle").takeIf { it.exists() }
    }
}
