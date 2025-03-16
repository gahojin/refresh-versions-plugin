/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions.model

import org.gradle.api.artifacts.ExternalDependency
import org.gradle.api.artifacts.ModuleIdentifier
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.plugin.use.PluginDependency

/**
 * 依存情報.
 */
sealed interface Dependency {
    val moduleId: ModuleIdentifier
    val version: String
    val versionConstraint: VersionConstraint?

    fun asArtifactResolutionDetails(): ArtifactResolutionDetailsDelegate {
        return ArtifactResolutionDetailsDelegate(
            moduleId = moduleId,
            version = version,
        )
    }

    companion object {
        fun from(dependency: org.gradle.api.artifacts.Dependency): Dependency? {
            return when (dependency) {
                // project参照の依存は無視する
                is ProjectDependency -> null
                is ExternalDependency -> External(dependency)
                else -> General(dependency)
            }
        }

        fun from(dependency: PluginDependency): Dependency? {
            return Plugin(dependency)
        }
    }

    @ConsistentCopyVisibility
    data class General private constructor(
        override val moduleId: ModuleIdentifier,
        override val version: String,
    ) : Dependency {
        override val versionConstraint = null

        constructor(dependency: org.gradle.api.artifacts.Dependency) : this(
            moduleId = ModuleId(
                group = dependency.group.orEmpty(),
                name = dependency.name,
            ),
            version = dependency.version.orEmpty(),
        )
    }

    @ConsistentCopyVisibility
    data class External private constructor(
        override val moduleId: ModuleIdentifier,
        override val version: String,
        override val versionConstraint: VersionConstraint?,
    ) : Dependency {
        constructor(dependency: ExternalDependency) : this(
            moduleId = ModuleId(dependency.module),
            version = dependency.version.orEmpty(),
            versionConstraint = VersionConstraint(dependency.versionConstraint),
        )
    }

    @ConsistentCopyVisibility
    data class Plugin private constructor(
        val pluginId: String,
        override val moduleId: ModuleIdentifier,
        override val version: String,
        override val versionConstraint: VersionConstraint?,
    ) : Dependency {
        constructor(dependency: PluginDependency) : this(
            pluginId = dependency.pluginId,
            moduleId = ModuleId(
                group = dependency.pluginId,
                name = "${dependency.pluginId}.gradle.plugin",
            ),
            version = dependency.version.requiredVersion,
            versionConstraint = VersionConstraint(dependency.version),
        )
    }
}

