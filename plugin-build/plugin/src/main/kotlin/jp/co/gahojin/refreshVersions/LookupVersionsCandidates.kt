/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions

import jp.co.gahojin.refreshVersions.dependency.MavenDependencyVersionsFetcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.repositories.ArtifactRepository
import org.gradle.plugin.use.PluginDependency
import kotlin.coroutines.CoroutineContext

/**
 * バージョン候補を調べる.
 */
internal class LookupVersionsCandidates(
    logLevel: HttpLoggingInterceptor.Level = HttpLoggingInterceptor.Level.BASIC,
    private val context: CoroutineContext = Dispatchers.IO,
) {
    private val loggingInterceptor = HttpLoggingInterceptor { println(it) }
        .setLevel(logLevel)

    suspend fun execute(
        repositories: List<ArtifactRepository>,
        versionsCatalogLibraries: Set<MinimalExternalModuleDependency>,
        versionsCatalogPlugins: Set<PluginDependencyCompat>,
    ) {
        return withContext(context) {
            withClient {
                versionsCatalogLibraries.forEach { library ->
                    repositories.forEach { repository ->
//                        MavenDependencyVersionsFetcher.ForHttp(it, repository.toString())
                    }
                }
            }
        }
    }

    private suspend inline fun <R> withClient(block: suspend (client: OkHttpClient) -> R): R {
        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .followSslRedirects(true)
            .build()
        return try {
            block(client)
        } finally {
            client.dispatcher.executorService.shutdown()
        }
    }
}
