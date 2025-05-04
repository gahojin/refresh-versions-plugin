/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions

import jp.co.gahojin.refreshVersions.extension.create
import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import java.io.File

abstract class RefreshVersionsExtension {
    /** 対象バージョンカタログファイル */
    @get:InputFile
    var versionsTomlFile: File? = null

    /** セクション並び替えフラグ */
    @get:Input
    var sortSection: Boolean = false

    /** キャッシュ有効期間 (分) */
    @get:Input
    var cacheDurationMinutes: Int = DEFAULT_CACHE_DURATION

    internal fun getVersionsTomlFile(project: Project): File {
        return versionsTomlFile ?: project.file(Constants.LIBS_VERSIONS_TOML)
    }

    companion object {
        private const val EXTENSION_NAME = "refreshVersions"

        private const val DEFAULT_CACHE_DURATION = 60

        @JvmStatic
        fun create(settings: Settings): RefreshVersionsExtension {
            return settings.extensions.create<RefreshVersionsExtension>(EXTENSION_NAME)
        }
    }
}
