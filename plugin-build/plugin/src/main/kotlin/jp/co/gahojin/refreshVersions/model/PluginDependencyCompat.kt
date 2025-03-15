/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions.model

import org.gradle.api.artifacts.VersionConstraint
import org.gradle.plugin.use.PluginDependency

data class PluginDependencyCompat(
    val pluginId: String,
    val version: VersionConstraint,
) : DependencyProvider {
    constructor(dependency: PluginDependency) : this(
        pluginId = dependency.pluginId,
        version = dependency.version,
    )

    override fun getDependency() = Dependency.Plugin(this)
}
