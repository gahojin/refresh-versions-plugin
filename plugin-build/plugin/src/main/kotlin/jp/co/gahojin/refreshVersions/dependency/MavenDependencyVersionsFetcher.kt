/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions.dependency

import jp.co.gahojin.refreshVersions.HttpException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.coroutines.executeAsync
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

internal sealed class MavenDependencyVersionsFetcher(
    protected val repositoryUrl: String,
    protected val group: String,
    protected val name: String,
) {
    abstract suspend fun fetchXmlMetadata(): Result<String?>

    class ForHttp(
        private val client: OkHttpClient,
        repositoryUrl: String,
        group: String,
        name: String,
        private val authorization: String?,
        private val cacheDuration: Duration,
    ) : MavenDependencyVersionsFetcher(repositoryUrl, group, name) {
        private val metadataUrl = "${repositoryUrl.removeSuffix("/")}/${group.replace('.', '/')}/${name}/maven-metadata.xml"
        private val request = Request.Builder().apply {
            // 一定期間キャッシュする
            cacheControl(CacheControl.Builder().maxStale(cacheDuration).build())
            url(metadataUrl)
            authorization?.also { header("Authorization", it) }
        }.build()

        @OptIn(ExperimentalCoroutinesApi::class)
        override suspend fun fetchXmlMetadata() = runCatching {
            client.newCall(request).executeAsync().use { response ->
                if (response.isSuccessful) {
                    response.use { it.body.string() }
                } else when (response.code) {
                    401, 404 -> null
                    else -> throw HttpException(response.code, response.body)
                }
            }
        }
    }

    class ForFile(
        repositoryUrl: String,
        group: String,
        name: String,
    ) : MavenDependencyVersionsFetcher(repositoryUrl, group, name) {
        private val repositoryDir = File(repositoryUrl.substringAfter("file:").removeSuffix("/"))

        override suspend fun fetchXmlMetadata() = runCatching {
            val targetDir = repositoryDir.resolve("${group.replace('.', File.pathSeparatorChar)}/${name}")
            // メタデータのXMLファイルを抽出
            targetDir.walkTopDown()
                .filter { it.startsWith("maven-metadata") && it.endsWith(".xml") }
                .singleOrNull()?.readText()
        }
    }
}
