/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions.model

import org.gradle.api.artifacts.ModuleIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.internal.artifacts.repositories.ArtifactResolutionDetails

class ArtifactResolutionDetailsDelegate(
    val group: String,
    val name: String,
) : ArtifactResolutionDetails {
    var found: Boolean = true
        private set

    override fun getModuleId(): ModuleIdentifier {
        return object : ModuleIdentifier {
            override fun getGroup() = this@ArtifactResolutionDetailsDelegate.group
            override fun getName() = this@ArtifactResolutionDetailsDelegate.name
        }
    }

    override fun getComponentId(): ModuleComponentIdentifier? = null

    override fun isVersionListing(): Boolean = true

    override fun notFound() {
        found = false
    }
}
