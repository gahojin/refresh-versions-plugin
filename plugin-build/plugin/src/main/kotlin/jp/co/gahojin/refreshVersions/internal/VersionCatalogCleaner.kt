/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions.internal

import jp.co.gahojin.refreshVersions.extension.writeTextWhenUpdated
import jp.co.gahojin.refreshVersions.toml.TomlFile
import java.io.File
import java.io.Reader

internal object VersionCatalogCleaner {
    fun execute(
        file: File,
        sortSection: Boolean,
    ) {
        val content = file.bufferedReader().use {
            execute(it, sortSection)
        }
        file.writeTextWhenUpdated(content)
    }

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
