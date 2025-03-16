/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions.model

import org.gradle.api.artifacts.ModuleIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.internal.artifacts.repositories.ArtifactResolutionDetails

class ArtifactResolutionDetailsDelegate(
    private val moduleId: ModuleId,
    private val version: String,
) : ArtifactResolutionDetails {
    var found: Boolean = true
        private set

    private val componentId by lazy {
        object : ModuleComponentIdentifier {
            override fun getDisplayName() = "$moduleId:$version"
            override fun getGroup() = moduleId.group
            override fun getModule() = moduleId.name
            override fun getVersion() = this@ArtifactResolutionDetailsDelegate.version
            override fun getModuleIdentifier() = moduleId
        }
    }

    constructor(
        moduleId: ModuleIdentifier,
        version: String,
    ) : this(
        moduleId = ModuleId(moduleId),
        version = version,
    )

    override fun getModuleId() = moduleId

    override fun getComponentId(): ModuleComponentIdentifier {
        return componentId
    }

    override fun isVersionListing(): Boolean = true

    override fun notFound() {
        found = false
    }
}
