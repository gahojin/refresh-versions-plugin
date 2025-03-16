/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions.model

/**
 * ライブラリ/プラグイン情報と、アップデート可能なバージョン一覧.
 */
data class UpdatableDependency(
    val dependency: Dependency,
    val updatableVersions: List<Version>,
)
