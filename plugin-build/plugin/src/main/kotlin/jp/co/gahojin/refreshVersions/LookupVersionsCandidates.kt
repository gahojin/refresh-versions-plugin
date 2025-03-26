/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions

import jp.co.gahojin.refreshVersions.model.DependencyWithRepository
import jp.co.gahojin.refreshVersions.model.UpdatableDependency
import jp.co.gahojin.refreshVersions.model.filterAfter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.gradle.api.logging.Logger
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.minutes

/**
 * バージョン候補を調べる.
 */
internal class LookupVersionsCandidates(
    private val cacheDurationMinutes: Int,
    logger: Logger,
    logLevel: HttpLoggingInterceptor.Level = HttpLoggingInterceptor.Level.BASIC,
    private val context: CoroutineContext = Dispatchers.IO,
) {
    private val loggingInterceptor = HttpLoggingInterceptor {
        logger.info(it)
    }.setLevel(logLevel)

    suspend fun execute(dependencies: List<DependencyWithRepository>): List<UpdatableDependency> {
        return withContext(context) {
            withClient { client ->
                dependencies.map { (dependency, repositories) ->
                    val versions = repositories.flatMap { repository ->
                        repository.createFetcher(
                            client = client,
                            library = dependency,
                            cacheDuration = cacheDurationMinutes.minutes,
                        ).fetchVersions()
                    }.toSortedSet()

                    UpdatableDependency(
                        dependency = dependency,
                        updatableVersions = versions.filterAfter(dependency.version),
                    )
                }
            }
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
