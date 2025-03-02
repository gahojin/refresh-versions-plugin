/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions

import jp.co.gahojin.refreshVersions.toml.TomlFile
import jp.co.gahojin.refreshVersions.toml.TomlSection

internal object VersionCatalogCleaner {
    fun execute(
        fileContent: String,
        sortSection: Boolean,
    ): String {
        if (fileContent.isBlank()) return ""

        val toml = TomlFile.parseToml(fileContent)

        toml.sections
            .filterNot { it.key == TomlSection.Root }
            .forEach { (section, lines) ->
                toml[section] = lines.filterNot { it.text.startsWith("##") }
            }

        return toml.format(sortSection)
    }
}
