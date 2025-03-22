/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions.model

import jp.co.gahojin.refreshVersions.dependency.DependencyVersionsFetcher
import jp.co.gahojin.refreshVersions.dependency.FlatDirDependencyVersionsFetcher
import jp.co.gahojin.refreshVersions.dependency.IvyDependencyVersionsFetcher
import jp.co.gahojin.refreshVersions.dependency.MavenDependencyVersionsFetcher
import jp.co.gahojin.refreshVersions.extension.passwordCredentials
import okhttp3.Credentials
import okhttp3.OkHttpClient
import org.gradle.api.artifacts.repositories.ArtifactRepository
import org.gradle.api.artifacts.repositories.FlatDirectoryArtifactRepository
import org.gradle.api.artifacts.repositories.IvyArtifactRepository
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import java.net.URI
import kotlin.time.Duration

/**
 * リポジトリ情報.
 *
 * project.repositoriesにタスク実行中にアクセス出来ないため、キャッシュ可能な形で保持する
 */
interface Repository {
    val name: String

    fun createFetcher(
        client: OkHttpClient,
        library: Dependency,
        cacheDuration: Duration,
    ): DependencyVersionsFetcher

    companion object {
        @JvmStatic
        fun from(repository: ArtifactRepository): Repository? {
            return when (repository) {
                is MavenArtifactRepository -> Maven(repository)
                is IvyArtifactRepository -> Ivy(repository)
                is FlatDirectoryArtifactRepository -> FlatDirectory(repository)
                else -> null
            }
        }
    }

    data class Maven internal constructor(
        override val name: String,
        val url: URI,
        val credentials: PasswordCredentials?,
    ) : Repository {
        constructor(repository: MavenArtifactRepository) : this(
            name = repository.name,
            url = repository.url,
            credentials = repository.passwordCredentials,
        )

        override fun createFetcher(
            client: OkHttpClient,
            library: Dependency,
            cacheDuration: Duration,
        ): DependencyVersionsFetcher {
            return when (url.scheme) {
                "http", "https" -> MavenDependencyVersionsFetcher.ForHttp(
                    client = client,
                    repositoryUrl = url.toString(),
                    moduleId = library.moduleId,
                    authorization = credentials?.let {
                        Credentials.basic(it.username, it.password)
                    },
                    cacheDuration = cacheDuration,
                )
                "file" -> MavenDependencyVersionsFetcher.ForFile(
                    repositoryUrl = url.toString(),
                    moduleId = library.moduleId,
                )
                else -> DependencyVersionsFetcher.Empty
            }
        }
    }

    data class Ivy internal constructor(
        override val name: String,
        val url: URI,
        val credentials: PasswordCredentials?,
    ) : Repository {
        constructor(repository: IvyArtifactRepository) : this(
            name = repository.name,
            url = repository.url,
            credentials = repository.passwordCredentials,
        )

        override fun createFetcher(
            client: OkHttpClient,
            library: Dependency,
            cacheDuration: Duration,
        ): DependencyVersionsFetcher {
            return IvyDependencyVersionsFetcher()
        }
    }

    data class FlatDirectory internal constructor(
        override val name: String,
    ) : Repository {
        constructor(repository: FlatDirectoryArtifactRepository) : this(
            name = repository.name,
        )

        override fun createFetcher(
            client: OkHttpClient,
            library: Dependency,
            cacheDuration: Duration,
        ) = FlatDirDependencyVersionsFetcher
    }
}
