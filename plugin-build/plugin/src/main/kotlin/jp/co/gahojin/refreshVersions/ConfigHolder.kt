/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions

import org.gradle.api.initialization.Settings

object ConfigHolder {
    internal lateinit var settings: Settings
        private set

    internal fun initialize(
        settings: Settings,
    ) {
        this.settings = settings
    }
}
