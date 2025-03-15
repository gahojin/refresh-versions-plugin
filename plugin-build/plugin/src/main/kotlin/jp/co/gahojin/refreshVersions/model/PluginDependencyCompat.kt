/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions.model

import org.gradle.api.artifacts.VersionConstraint
import org.gradle.plugin.use.PluginDependency

internal data class PluginDependencyCompat(
    val pluginId: String,
    val version: VersionConstraint,
) {
    constructor(dependency: PluginDependency) : this(
        pluginId = dependency.pluginId,
        version = dependency.version,
    )
}
