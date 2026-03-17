/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions

import jp.co.gahojin.refreshVersions.model.DependencyWithRepository
import jp.co.gahojin.refreshVersions.model.UpdatableDependency
import jp.co.gahojin.refreshVersions.model.filterAfter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import org.gradle.api.logging.Logger
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration

/**
 * バージョン候補を調べる.
 */
class LookupVersionsCandidates(
    logger: Logger,
    logLevel: HttpLoggingInterceptor.Level = HttpLoggingInterceptor.Level.BASIC,
    private val cacheDuration: Duration,
    private val context: CoroutineContext = Dispatchers.IO,
) {
    private val loggingInterceptor = HttpLoggingInterceptor {
        if (logger.isInfoEnabled) {
            logger.info(it)
        }
    }.setLevel(logLevel)

    suspend fun execute(dependencies: List<DependencyWithRepository>): List<UpdatableDependency> {
        return withContext(context) {
            withClient(cacheDuration) { client ->
                dependencies.map { (dependency, repositories) ->
                    val versions = repositories.flatMap { repository ->
                        repository.createFetcher(
                            client = client,
                            library = dependency,
                            cacheDuration = cacheDuration,
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

    private inline fun <R> withClient(cacheDuration: Duration, block: (client: OkHttpClient) -> R): R {
        val client = OkHttpClient.Builder()
            .cache(ConfigHolder.cache)
            .addNetworkInterceptor(loggingInterceptor)
            // キャッシュ期間を上書きする
            .addNetworkInterceptor(CacheInterceptor(cacheDuration))
            .followSslRedirects(true)
            .build()
        return try {
            block(client)
        } finally {
            client.dispatcher.executorService.shutdown()
        }
    }
}

private class CacheInterceptor(
    private val cacheDuration: Duration,
): Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        val serverCacheControl = CacheControl.parse(response.headers)
        val cacheControl = when {
            // キャッシュを保存しないようサーバが応答している場合、保存しないようヘッダーを書き換える
            cacheDuration == Duration.ZERO || serverCacheControl.isPrivate || serverCacheControl.noStore || serverCacheControl.noCache -> {
                CacheControl.Builder().noStore().build()
            }
            // それ以外の場合は、設定した期間キャッシュさせる
            else -> CacheControl.Builder().maxAge(cacheDuration).build()
        }

        return response.newBuilder()
            .header("cache-control", cacheControl.toString())
            .removeHeader("age")
            .removeHeader("pragma")
            .removeHeader("vary")
            .build()
    }
}
