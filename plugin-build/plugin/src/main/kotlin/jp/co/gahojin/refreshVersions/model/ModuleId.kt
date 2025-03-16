/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions.model

import org.gradle.api.artifacts.ModuleIdentifier

/**
 * モジュール識別子.
 */
data class ModuleId(
    private val group: String,
    private val name: String,
) : ModuleIdentifier {
    constructor(moduleIdentifier: ModuleIdentifier) : this(
        group = moduleIdentifier.group,
        name = moduleIdentifier.name,
    )

    override fun getGroup() = group
    override fun getName() = name
    override fun toString() = "$group:$name"
}
