/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions.model

/**
 * バージョン制約.
 */
data class VersionConstraint(
    val displayName: String,
    val branch: String?,
    val strictVersion: String,
    val requiredVersion: String,
    val preferredVersion: String,
    val rejectVersions: List<String>,
) {
    constructor(original: org.gradle.api.artifacts.VersionConstraint) : this(
        displayName = original.displayName,
        branch = original.branch,
        strictVersion = original.strictVersion,
        requiredVersion = original.requiredVersion,
        preferredVersion = original.preferredVersion,
        rejectVersions = original.rejectedVersions,
    )
}
