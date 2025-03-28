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

    // プラグインのパッケージ名接尾語
    const val PLUGIN_NAME_SUFFIX = ".gradle.plugin"

    // カタログファイルセクション並び順
    val orderTomlSections = listOf(
        TomlSection.Root,
        TomlSection.Versions,
        TomlSection.Libraries,
        TomlSection.Bundles,
        TomlSection.Plugins,
    )

    // id("plugin") version "version" 文字列検知用正規表現
    val pluginDslRegex = """id\s*\(?\s*["']([0-9a-zA-Z\-_.]*)["']\s*\)?\s*\.?\s*version\s*\(?\s*["']([0-9a-zA-Z\-_.]*)["']\s*\)?""".toRegex()

    // //   ^ "x.y.z" 文字列検知用正規表現
    val addCommentRegex = """^//\s*\^\s*["']([0-9a-zA-Z\-_.]*)["']""".toRegex()
}
