/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions

import jp.co.gahojin.refreshVersions.dependency.MavenDependencyVersionsFetcher
import jp.co.gahojin.refreshVersions.dependency.MavenMetadataParser
import jp.co.gahojin.refreshVersions.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
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

    suspend fun executeLibrary(
        repositories: List<Repository.Maven>,
        versionsCatalogLibraries: Set<Dependency>,
    ) = execute(repositories, versionsCatalogLibraries) { dependency, versions ->
        DependencyContainer.Library(
            dependency = dependency,
            updatableVersions = versions,
        )
    }

    suspend fun executePlugin(
        repositories: List<Repository.Maven>,
        versionsCatalogPlugins: Set<PluginDependencyCompat>,
    ) = execute(repositories, versionsCatalogPlugins) { dependency, versions ->
        DependencyContainer.Plugin(
            dependency = dependency,
            updatableVersions = versions,
        )
    }

    private suspend inline fun <T : DependencyProvider> execute(
        repositories: List<Repository.Maven>,
        dependencies: Collection<T>,
        crossinline block: (T, List<Version>) -> DependencyContainer<T>,
    ): List<DependencyContainer<T>> {
        return withContext(context) {
            withClient { client ->
                dependencies.map {
                    val dependency = it.getDependency()
                    val versions = repositories.mapNotNull { repository ->
                        repository.createFetcher(
                            client = client,
                            library = dependency,
                        )?.fetchXmlMetadata()?.getOrNull()
                    }.flatMap(MavenMetadataParser::parse)

                    block(it, versions.filterAfter(dependency.version))
                }
            }
        }
    }

    private fun Repository.Maven.createFetcher(
        client: OkHttpClient,
        library: Dependency,
    ): MavenDependencyVersionsFetcher? {
        val group = library.group
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
