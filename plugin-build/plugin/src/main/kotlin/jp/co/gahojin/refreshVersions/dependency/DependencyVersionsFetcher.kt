/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions.dependency

import jp.co.gahojin.refreshVersions.model.Version

/**
 * バージョン取得処理.
 */
interface DependencyVersionsFetcher {
    suspend fun fetchVersions(): List<Version>

    object Empty : DependencyVersionsFetcher {
        override suspend fun fetchVersions(): List<Version> = emptyList()
    }
}
