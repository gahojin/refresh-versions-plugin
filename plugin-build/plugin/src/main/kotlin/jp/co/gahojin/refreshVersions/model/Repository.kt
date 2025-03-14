/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions.model

import org.gradle.api.artifacts.repositories.ArtifactRepository
import org.gradle.api.artifacts.repositories.IvyArtifactRepository
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
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
}

fun List<Repository>.maven(): List<Repository.Maven> = filterIsInstance<Repository.Maven>()
fun List<Repository>.ivy(): List<Repository.Ivy> = filterIsInstance<Repository.Ivy>()
