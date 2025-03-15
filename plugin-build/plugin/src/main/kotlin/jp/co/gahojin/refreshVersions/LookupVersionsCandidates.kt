/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions

import jp.co.gahojin.refreshVersions.dependency.MavenDependencyVersionsFetcher
import jp.co.gahojin.refreshVersions.model.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.gradle.api.artifacts.MinimalExternalModuleDependency
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
        repositories: List<Repository.Maven>,
        versionsCatalogLibraries: Set<MinimalExternalModuleDependency>,
        versionsCatalogPlugins: Set<PluginDependencyCompat>,
    ): List<String> {
        return withContext(context) {
            withClient {
                versionsCatalogLibraries.flatMap { library ->
                    repositories.mapNotNull { repository ->
                        repository.createFetcher(
                            client = it,
                            library = library,
                        )?.fetchXmlMetadata()?.getOrNull()
                    }
                }
            }
        }
    }

    private fun Repository.Maven.createFetcher(
        client: OkHttpClient,
        library: MinimalExternalModuleDependency,
    ): MavenDependencyVersionsFetcher? {
        val group = library.group.orEmpty()
        val name = library.name

        return when (url.scheme) {
            "http", "https" -> MavenDependencyVersionsFetcher.ForHttp(
                group = group,
                name = name,
                client = client,
                repositoryUrl = url.toString(),
                authorization = credentials?.let {
                    Credentials.basic(it.username, it.password)
                },
                cacheDuration = cacheDurationMinutes.minutes,
            )
            "file" -> MavenDependencyVersionsFetcher.ForFile(
                group = group,
                name = name,
                repositoryUrl = url.toString(),
            )
            else -> null
        }
    }

    private inline fun <R> withClient(block: (client: OkHttpClient) -> R): R {
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
