/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions.model

import org.gradle.api.artifacts.ExternalDependency
import org.gradle.api.artifacts.ModuleIdentifier
import org.gradle.api.artifacts.ModuleVersionSelector
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.plugin.use.PluginDependency

/**
 * 依存情報.
 */
sealed interface Dependency : Comparable<Dependency> {
    val moduleId: ModuleId
    val version: String
    val versionConstraint: VersionConstraint?

    override fun compareTo(other: Dependency): Int {
        var ret = moduleId.compareTo(other.moduleId)
        if (ret == 0) {
            ret = version.compareTo(other.version)
        }
        return ret
    }

    fun asArtifactResolutionDetails(): ArtifactResolutionDetailsDelegate {
        return ArtifactResolutionDetailsDelegate(
            moduleId = moduleId,
            version = version,
        )
    }

    companion object {
        fun from(dependency: org.gradle.api.artifacts.Dependency): Dependency? {
            // バージョンが未定義の場合、処理対象外
            if (dependency.version == null) {
                return null
            }
            return when (dependency) {
                // project参照の依存は無視する
                is ProjectDependency -> null
                is ExternalDependency -> External(dependency)
                else -> General(dependency)
            }
        }

        fun from(dependency: PluginDependency): Dependency {
            return Plugin(dependency)
        }

        fun from(dependency: ModuleVersionSelector): Dependency {
            return ForceModule(dependency)
        }
    }

    @ConsistentCopyVisibility
    data class General private constructor(
        override val moduleId: ModuleId,
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
        override val moduleId: ModuleId,
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
        override val moduleId: ModuleId,
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

    @ConsistentCopyVisibility
    data class ForceModule private constructor(
        override val moduleId: ModuleId,
        override val version: String,
    ) : Dependency {
        override val versionConstraint = null

        constructor(moduleSelector: ModuleVersionSelector) : this(
            moduleId = ModuleId(
                group = moduleSelector.group,
                name = moduleSelector.name,
            ),
            version = moduleSelector.version.orEmpty(),
        )
    }
}

