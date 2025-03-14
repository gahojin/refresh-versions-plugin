/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions.extension

import org.gradle.api.artifacts.repositories.ArtifactRepository
import org.gradle.api.internal.artifacts.repositories.ArtifactResolutionDetails
import org.gradle.api.internal.artifacts.repositories.ContentFilteringRepository

fun ArtifactRepository.contentFilter(details: ArtifactResolutionDetails) = runCatching {
    if (this is ContentFilteringRepository) {
        contentFilter.execute(details)
    }
}
