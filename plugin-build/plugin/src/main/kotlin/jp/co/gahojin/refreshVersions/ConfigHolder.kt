/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions

import jp.co.gahojin.refreshVersions.extension.globalRepositories
import okhttp3.Cache
import org.gradle.api.Task
import org.gradle.api.artifacts.repositories.ArtifactRepository
import org.gradle.api.initialization.Settings

object ConfigHolder {
    // キャッシュ最大サイズ 1MB
    private const val CACHE_MAX_SIZE = 1024L * 1024L

    internal lateinit var settings: Settings
        private set

    internal lateinit var cache: Cache

    val globalRepositories: List<ArtifactRepository>
        get() = settings.globalRepositories

    val pluginRepositories: List<ArtifactRepository>
        get() = settings.pluginManagement.repositories.asMap.values.toList()

    internal fun initialize(settings: Settings) {
        this.settings = settings
    }

    internal fun initialize(task: Task) {
        this.cache = Cache(task.temporaryDir, CACHE_MAX_SIZE)
    }
}
