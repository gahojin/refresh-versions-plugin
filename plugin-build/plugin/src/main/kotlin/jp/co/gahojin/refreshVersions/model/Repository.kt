/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions.model

import jp.co.gahojin.refreshVersions.extension.passwordCredentials
import org.gradle.api.artifacts.repositories.ArtifactRepository
import org.gradle.api.artifacts.repositories.FlatDirectoryArtifactRepository
import org.gradle.api.artifacts.repositories.IvyArtifactRepository
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import java.io.File
import java.net.URI

/**
 * リポジトリ情報.
 *
 * project.repositoriesにタスク実行中にアクセス出来ないため、キャッシュ可能な形で保持する
 */
sealed class Repository {
    abstract val name: String

    companion object {
        fun from(repository: ArtifactRepository): Repository? {
            return when (repository) {
                is MavenArtifactRepository -> Maven(repository)
                is IvyArtifactRepository -> Ivy(repository)
                is FlatDirectoryArtifactRepository -> FlatDirectory(repository)
                else -> null
            }
        }
    }

    @ConsistentCopyVisibility
    data class Maven private constructor(
        override val name: String,
        val url: URI,
        val credentials: PasswordCredentials?,
    ) : Repository() {
        constructor(repository: MavenArtifactRepository) : this(
            name = repository.name,
            url = repository.url,
            credentials = repository.passwordCredentials,
        )
    }

    @ConsistentCopyVisibility
    data class Ivy private constructor(
        override val name: String,
        val url: URI,
        val credentials: PasswordCredentials?,
    ) : Repository() {
        constructor(repository: IvyArtifactRepository) : this(
            name = repository.name,
            url = repository.url,
            credentials = repository.credentials?.let {
                PasswordCredentials(
                    // 認証情報がnullの場合、credentials自体をnullとする
                    username = it.username ?: return@let null,
                    password = it.password ?: return@let null,
                )
            },
        )
    }

    @ConsistentCopyVisibility
    data class FlatDirectory private constructor(
        override val name: String,
        val dirs: Set<File>,
    ) : Repository() {
        constructor(repository: FlatDirectoryArtifactRepository) : this(
            name = repository.name,
            dirs = repository.dirs,
        )
    }
}

fun List<Repository>.maven() = filterIsInstance<Repository.Maven>()
fun List<Repository>.ivy() = filterIsInstance<Repository.Ivy>()
fun List<Repository>.flatDirectory() = filterIsInstance<Repository.FlatDirectory>()
