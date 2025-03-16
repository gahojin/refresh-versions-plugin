/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions.model

/**
 * ライブラリ/プラグイン情報と、リポジトリ情報.
 */
data class DependencyWithRepository(
    val dependency: Dependency,
    val repositories: List<Repository>,
)
