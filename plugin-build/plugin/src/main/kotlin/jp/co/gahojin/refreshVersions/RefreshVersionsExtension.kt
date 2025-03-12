/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions

import jp.co.gahojin.refreshVersions.extension.create
import org.gradle.api.initialization.Settings
import java.io.File

abstract class RefreshVersionsExtension {
    /** 対象バージョンカタログファイル */
    var versionsTomlFile: File? = null

    /** セクション並び替えフラグ */
    var sortSection: Boolean = false

    /** キャッシュ有効期間 (分) */
    var cacheDurationMinutes: Int = DEFAULT_CACHE_DURATION

    internal fun getVersionsTomlFile(): File {
        val settings = ConfigHolder.settings
        return versionsTomlFile ?: settings.settingsDir.resolve(Constants.LIBS_VERSIONS_TOML)
    }

    companion object {
        private const val EXTENSION_NAME = "refreshVersions"

        private const val DEFAULT_CACHE_DURATION = 60

        fun create(settings: Settings): RefreshVersionsExtension {
            return settings.extensions.create<RefreshVersionsExtension>(EXTENSION_NAME)
        }
    }
}
