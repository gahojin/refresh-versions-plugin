/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions.dependency

import jp.co.gahojin.refreshVersions.HttpException
import jp.co.gahojin.refreshVersions.model.Version
import kotlinx.coroutines.ExperimentalCoroutinesApi
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.coroutines.executeAsync
import org.gradle.api.artifacts.ModuleIdentifier
import java.io.File
import kotlin.time.Duration

/**
 * Maven依存バージョン取得.
 */
sealed class MavenDependencyVersionsFetcher : DependencyVersionsFetcher {
    protected abstract suspend fun fetchXmlMetadata(): Result<String?>

    override suspend fun fetchVersions(): List<Version> {
        val xml = fetchXmlMetadata().getOrNull() ?: return emptyList()
        return MavenMetadataParser.parse(xml)
    }

    class ForHttp(
        private val client: OkHttpClient,
        repositoryUrl: String,
        moduleId: ModuleIdentifier,
        private val authorization: String?,
        private val cacheDuration: Duration,
    ) : MavenDependencyVersionsFetcher() {
        private val metadataUrl = "${repositoryUrl.removeSuffix("/")}/${moduleId.group.replace('.', '/')}/${moduleId.name}/maven-metadata.xml"
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
                } else {
                    when (response.code) {
                        401, 404 -> null
                        else -> throw HttpException(response.code, response.body)
                    }
                }
            }
        }
    }

    class ForFile(
        repositoryUrl: String,
        moduleId: ModuleIdentifier,
    ) : MavenDependencyVersionsFetcher() {
        private val repositoryDir = File(repositoryUrl.substringAfter("file:").removeSuffix("/"))
        private val targetDir = repositoryDir.resolve("${moduleId.group.replace('.', File.separatorChar)}/${moduleId.name}")

        override suspend fun fetchXmlMetadata() = runCatching {
            // メタデータのXMLファイルを抽出
            targetDir.walkTopDown()
                .filter { it.name.startsWith("maven-metadata") && it.name.endsWith(".xml") }
                .singleOrNull()?.readText()
        }
    }
}
