/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions.dependency

import jp.co.gahojin.refreshVersions.model.Version

/**
 * FlatDirバージョン取得.
 *
 * FlatDirにバージョン情報がない
 */
object FlatDirDependencyVersionsFetcher : DependencyVersionsFetcher {
    override suspend fun fetchVersions(): List<Version> = emptyList()
}
