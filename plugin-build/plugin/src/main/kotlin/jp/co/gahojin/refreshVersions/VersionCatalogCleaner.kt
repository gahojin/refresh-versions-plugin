/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions

import jp.co.gahojin.refreshVersions.toml.TomlFile
import jp.co.gahojin.refreshVersions.toml.TomlSection
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

        toml.sections
            .filterNot { it.key == TomlSection.Root }
            .forEach { (section, lines) ->
                toml[section] = lines.filterNot { it.text.startsWith("##") }
            }

        return toml.format(sortSection)
    }
}
