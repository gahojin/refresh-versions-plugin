/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions.model

import org.gradle.api.artifacts.ExternalDependency
import org.gradle.api.artifacts.ProjectDependency

/**
 * 依存情報.
 */
sealed class Dependency {
    abstract val group: String
    abstract val name: String
    abstract val version: String?
    abstract val versionConstraint: VersionConstraint?

    fun asArtifactResolutionDetails(): ArtifactResolutionDetailsDelegate {
        return ArtifactResolutionDetailsDelegate(
            group = group,
            name = name,
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
    }

    @ConsistentCopyVisibility
    data class General private constructor(
        override val group: String,
        override val name: String,
        override val version: String?,
    ) : Dependency() {
        override val versionConstraint = null

        constructor(dependency: org.gradle.api.artifacts.Dependency) : this(
            group = dependency.group.orEmpty(),
            name = dependency.name,
            version = dependency.version,
        )
    }

    @ConsistentCopyVisibility
    data class External private constructor(
        override val group: String,
        override val name: String,
        override val version: String?,
        override val versionConstraint: VersionConstraint?,
    ) : Dependency() {
        constructor(dependency: ExternalDependency) : this(
            group = dependency.group.orEmpty(),
            name = dependency.name,
            version = dependency.version,
            versionConstraint = VersionConstraint(dependency.versionConstraint),
        )
    }
}

