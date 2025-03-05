/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions

import jp.co.gahojin.refreshVersions.toml.TomlSection

object Constants {
    // プラグイン情報
    const val GROUP = "refreshVersions"
    const val PLUGIN_ID = "jp.co.gahojin.refreshVersions"

    // バージョンカタログファイル名
    const val LIBS_VERSIONS_TOML = "gradle/libs.versions.toml"

    // バージョン更新箇所シンボル
    const val VERSION_SYMBOL = "^"

    // カタログファイルセクション並び順
    val orderTomlSections = listOf(
        TomlSection.Root,
        TomlSection.Versions,
        TomlSection.Libraries,
        TomlSection.Bundles,
        TomlSection.Plugins,
    )
}
