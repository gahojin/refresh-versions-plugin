/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions.extension

import jp.co.gahojin.refreshVersions.model.Dependency
import org.gradle.api.artifacts.VersionCatalog
import kotlin.jvm.optionals.getOrNull

fun VersionCatalog.plugins() = pluginAliases.asSequence()
    .mapNotNull { findPlugin(it).getOrNull()?.orNull }
    .map { Dependency.from(it) }
    .toSet()

fun VersionCatalog.libraries() = libraryAliases.asSequence()
    .mapNotNull { findLibrary(it).getOrNull()?.orNull }
    .map { Dependency.from(it) }
    .toSet()
