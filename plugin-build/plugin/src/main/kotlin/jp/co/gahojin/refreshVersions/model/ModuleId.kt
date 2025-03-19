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
) : ModuleIdentifier, Comparable<ModuleId> {
    constructor(moduleIdentifier: ModuleIdentifier) : this(
        group = moduleIdentifier.group,
        name = moduleIdentifier.name,
    )

    override fun compareTo(other: ModuleId): Int {
        var ret = group.compareTo(other.group)
        if (ret != 0) {
            ret = name.compareTo(other.name)
        }
        return ret
    }

    override fun getGroup() = group
    override fun getName() = name
    override fun toString() = "$group:$name"
}
