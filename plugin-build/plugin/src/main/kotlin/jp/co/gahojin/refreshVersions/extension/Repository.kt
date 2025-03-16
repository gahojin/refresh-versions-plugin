/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions.extension

import jp.co.gahojin.refreshVersions.model.PasswordCredentials
import org.gradle.api.artifacts.repositories.ArtifactRepository
import org.gradle.api.artifacts.repositories.IvyArtifactRepository
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.internal.artifacts.repositories.ArtifactResolutionDetails
import org.gradle.api.internal.artifacts.repositories.ContentFilteringRepository

fun ArtifactRepository.contentFilter(details: ArtifactResolutionDetails) = runCatching {
    if (this is ContentFilteringRepository) {
        contentFilter.execute(details)
    }
}

val MavenArtifactRepository.passwordCredentials: PasswordCredentials?
    get() = runCatching {
        when (url.scheme) {
            "http", "https" -> credentials.passwordCredentials
            else -> null
        }
    }.getOrNull()

val IvyArtifactRepository.passwordCredentials: PasswordCredentials?
    get() = runCatching {
        when (url.scheme) {
            "http", "https" -> credentials.passwordCredentials
            else -> null
        }
    }.getOrNull()

private val org.gradle.api.artifacts.repositories.PasswordCredentials.passwordCredentials: PasswordCredentials?
    get() {
        return PasswordCredentials(
            // 認証情報がnullの場合、credentials自体をnullとする
            username = username ?: return null,
            password = password ?: return null,
        )
    }
