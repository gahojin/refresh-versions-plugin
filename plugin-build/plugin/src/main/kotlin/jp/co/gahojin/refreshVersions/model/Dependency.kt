/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions.model

import org.gradle.api.artifacts.MinimalExternalModuleDependency
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
                else -> Module(dependency)
            }
        }

        @JvmStatic
        fun from(dependency: PluginDependency): Dependency {
            return Plugin(dependency)
        }

        @JvmStatic
        fun from(dependency: ModuleVersionSelector): Dependency {
            return Module(dependency)
        }

        @JvmStatic
        fun from(dependency: MinimalExternalModuleDependency): Dependency {
            return Module(dependency as ModuleVersionSelector)
        }
    }

    private data class Module(
        val group: String,
        val name: String,
        override val version: String,
    ) : Dependency {
        override val moduleId = ModuleId(
            group = group,
            name = name,
        )

        constructor(dependency: org.gradle.api.artifacts.Dependency) : this(
            group = dependency.group.orEmpty(),
            name = dependency.name,
            version = dependency.version.orEmpty(),
        )

        constructor(moduleSelector: ModuleVersionSelector) : this(
            group = moduleSelector.group,
            name = moduleSelector.name,
            version = moduleSelector.version.orEmpty(),
        )
    }

    private data class Plugin(
        val pluginId: String,
        override val version: String,
    ) : Dependency {
        override val moduleId = ModuleId(
            group = pluginId,
            name = "${pluginId}.gradle.plugin",
        )

        constructor(dependency: PluginDependency) : this(
            pluginId = dependency.pluginId,
            version = dependency.version.requiredVersion,
        )
    }
}

fun Set<Dependency>.withDependencies(
    dependencies: List<DependencyWithRepository>,
): List<DependencyWithRepository> {
    val modules = map { it.moduleId to it.version }.toSet()
    return dependencies.sortedBy { it.dependency }.filter { (dependency, _) ->
        modules.contains(dependency.moduleId to dependency.version)
    }
}
