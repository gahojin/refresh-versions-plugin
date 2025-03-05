/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions.ext

import jp.co.gahojin.refreshVersions.ConfigHolder
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension

fun Project.getDefaultVersionCatalog(): VersionCatalog? {
    val versionCatalogs = project.extensions.findByType<VersionCatalogsExtension>()
    val settings = ConfigHolder.settings
    return versionCatalogs?.find(settings.defaultCatalogName)?.orElse(null)
}
