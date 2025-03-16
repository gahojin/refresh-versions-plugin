/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions.dependency

import jp.co.gahojin.refreshVersions.model.Version

class IvyDependencyVersionsFetcher : DependencyVersionsFetcher {
    override suspend fun fetchVersions(): List<Version> = emptyList()
}
