/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions.model

import jp.co.gahojin.refreshVersions.Version

data class DependencyContainer(
    val dependency: Dependency,
    val updatableVersions: List<Version>,
)
