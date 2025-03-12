/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions

import jp.co.gahojin.refreshVersions.dependency.MavenDependencyVersionsFetcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okio.FileSystem
import okio.Path
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.repositories.ArtifactRepository
import org.gradle.api.artifacts.repositories.UrlArtifactRepository
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.minutes

/**
 * バージョン候補を調べる.
 */
internal class LookupVersionsCandidates(
    private val cacheDurationMinutes: Int,
    logLevel: HttpLoggingInterceptor.Level = HttpLoggingInterceptor.Level.BASIC,
    private val context: CoroutineContext = Dispatchers.IO,
) {
    private val loggingInterceptor = HttpLoggingInterceptor { println(it) }
        .setLevel(logLevel)

    suspend fun execute(
        repositories: List<ArtifactRepository>,
        versionsCatalogLibraries: Set<MinimalExternalModuleDependency>,
        versionsCatalogPlugins: Set<PluginDependencyCompat>,
    ): List<String?> {
        return withContext(context) {
            withClient {
                versionsCatalogLibraries.flatMap { library ->
                    repositories.filterIsInstance<UrlArtifactRepository>().map { repository ->
                        MavenDependencyVersionsFetcher.ForHttp(
                            client = it,
                            repositoryUrl = repository.url.toString(),
                            group = library.group.orEmpty(),
                            name = library.name,
                            authorization = null,
                            cacheDuration = cacheDurationMinutes.minutes,
                        ).fetchXmlMetadata().getOrNull()
                    }
                }
            }
        }
    }

    private suspend inline fun <R> withClient(block: suspend (client: OkHttpClient) -> R): R {
        val client = OkHttpClient.Builder()
            .cache(ConfigHolder.cache)
            .addNetworkInterceptor(loggingInterceptor)
            .followSslRedirects(true)
            .build()
        return try {
            block(client)
        } finally {
            client.dispatcher.executorService.shutdown()
        }
    }
}
