/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions.model

import org.gradle.api.artifacts.ExternalDependency
import org.gradle.api.artifacts.ModuleVersionSelector
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.plugin.use.PluginDependency

/**
 * 依存情報.
 */
sealed interface Dependency : Comparable<Dependency> {
    val moduleId: ModuleId
    val version: String

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
        @JvmStatic
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

        @JvmStatic
        fun from(dependency: PluginDependency): Dependency {
            return Plugin(dependency)
        }

        @JvmStatic
        fun from(dependency: ModuleVersionSelector): Dependency {
            return ForceModule(dependency)
        }
    }

    data class General(
        override val moduleId: ModuleId,
        override val version: String,
    ) : Dependency {
        constructor(dependency: org.gradle.api.artifacts.Dependency) : this(
            moduleId = ModuleId(
                group = dependency.group.orEmpty(),
                name = dependency.name,
            ),
            version = dependency.version.orEmpty(),
        )
    }

    data class External(
        override val moduleId: ModuleId,
        override val version: String,
    ) : Dependency {
        constructor(dependency: ExternalDependency) : this(
            moduleId = ModuleId(dependency.module),
            version = dependency.version.orEmpty(),
        )
    }

    data class Plugin(
        val pluginId: String,
        override val moduleId: ModuleId,
        override val version: String,
    ) : Dependency {
        constructor(dependency: PluginDependency) : this(
            pluginId = dependency.pluginId,
            moduleId = ModuleId(
                group = dependency.pluginId,
                name = "${dependency.pluginId}.gradle.plugin",
            ),
            version = dependency.version.requiredVersion,
        )
    }

    data class ForceModule(
        override val moduleId: ModuleId,
        override val version: String,
    ) : Dependency {
        constructor(moduleSelector: ModuleVersionSelector) : this(
            moduleId = ModuleId(
                group = moduleSelector.group,
                name = moduleSelector.name,
            ),
            version = moduleSelector.version.orEmpty(),
        )
    }
}

