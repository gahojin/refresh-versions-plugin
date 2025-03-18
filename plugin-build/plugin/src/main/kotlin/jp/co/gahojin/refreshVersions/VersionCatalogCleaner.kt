/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions

import jp.co.gahojin.refreshVersions.toml.TomlFile
import java.io.Reader

internal object VersionCatalogCleaner {
    fun execute(
        reader: Reader,
        sortSection: Boolean,
    ): String {
        val toml = TomlFile.parseToml(reader)
        if (toml.isEmpty) {
            return ""
        }
        toml.removeComments()
        return toml.format(sortSection)
    }
}
