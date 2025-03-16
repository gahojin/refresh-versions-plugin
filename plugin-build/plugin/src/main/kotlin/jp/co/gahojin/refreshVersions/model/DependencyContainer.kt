/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions.model

sealed interface DependencyContainer<T> {
    val dependency: T
    val updatableVersions: List<Version>

    data class Library(
        override val dependency: Dependency,
        override val updatableVersions: List<Version>,
    ) : DependencyContainer<Dependency>

    data class Plugin(
        override val dependency: PluginDependencyCompat,
        override val updatableVersions: List<Version>,
    ) : DependencyContainer<PluginDependencyCompat>
}
