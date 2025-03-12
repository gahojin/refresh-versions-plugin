/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions

import okhttp3.Cache
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import org.gradle.api.Task
import org.gradle.api.initialization.Settings

object ConfigHolder {
    // キャッシュ最大サイズ 1MB
    private const val CACHE_MAX_SIZE = 1024L * 1024L

    internal lateinit var settings: Settings
        private set

    internal lateinit var cache: Cache

    internal fun initialize(settings: Settings) {
        this.settings = settings
    }

    internal fun initialize(task: Task) {
        this.cache = Cache(FileSystem.SYSTEM, task.temporaryDir.toOkioPath(), CACHE_MAX_SIZE)
    }
}
