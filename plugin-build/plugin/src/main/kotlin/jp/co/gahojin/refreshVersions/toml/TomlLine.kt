/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions.toml

import jp.co.gahojin.refreshVersions.model.ModuleId
import org.gradle.api.artifacts.ModuleIdentifier

data class TomlLine(
    val section: TomlSection,
    val text: String,
) {
    val textWithoutComment = text.substringBefore('#')
    val key = textWithoutComment.substringBefore('=', missingDelimiterValue = "").trim()
    val hasKey = key.isNotBlank()

    val value = if (hasKey) textWithoutComment.substringAfter('=').trim() else ""

    val attributes: Map<String, String> = parseLine(section, value.unquote())

    val isEmptyLine: Boolean = value.isBlank()

    val id by attributes

    val group: String by lazy {
        if (section == TomlSection.Plugins) id else attributes.getOrDefault("group", "")
    }

    val name: String by lazy {
        if (section == TomlSection.Plugins) "$id.gradle.plugin" else attributes.getOrDefault("name", "")
    }

    val version: String by lazy {
        if (section == TomlSection.Versions) value.unquote() else attributes.getOrDefault("version", "")
    }

    val versionRef: String by lazy {
        attributes.getOrDefault("version.ref", "")
    }

    val moduleId: ModuleIdentifier by lazy {
        ModuleId(group = group, name = name)
    }

    override fun toString(): String = "TomlLine(section=${section}, key=${key}, value=${value}, attributes=${attributes}\n$text"

    companion object {
        val NEW_LINE = TomlLine(TomlSection.Custom("blank"), "")

        private fun String.unquote() = trim().removeSurrounding("\"")

        private fun parseLine(section: TomlSection, value: String): Map<String, String> {
            val splitByColon = value.split(':')

            return when (section) {
                TomlSection.Root, TomlSection.Bundles, TomlSection.Versions -> emptyMap<String, String>()
                TomlSection.Plugins -> {
                    if (value.startsWith('{')) {
                        getAttributes(value)
                    } else if (splitByColon.size > 1) {
                        buildMap<String, String> {
                            put("id", splitByColon[0].trim())
                            put("version", splitByColon[1])
                        }
                    } else emptyMap()
                }
                TomlSection.Libraries -> {
                    if (value.startsWith('{')) {
                        getAttributes(value).also { attributes ->
                            attributes["module"]?.also {
                                // module構文はgroup:nameなので分割する
                                val tmp = it.split(':', limit = 2)
                                if (tmp.size == 2) {
                                    attributes["group"] = tmp[0]
                                    attributes["name"] = tmp[1].trim()
                                }
                                attributes.remove("module")
                            } ?: attributes
                        }
                    } else buildMap<String, String> {
                        put("group", splitByColon[0])
                        if (splitByColon.size > 1) put("name", splitByColon[1].trim())
                        if (splitByColon.size > 2) put("version", splitByColon[2])
                    }
                }

                else -> emptyMap<String, String>()
            }
        }

        private fun getAttributes(value: String): MutableMap<String, String> {
            return value.removeSurrounding("{", "}")
                .split(",")
                .associate {
                    val tmp = it.split('=', limit = 2)
                    if (tmp.size == 2) tmp[0].unquote() to tmp[1].unquote() else tmp[0].unquote() to ""
                }.toMutableMap()
        }
    }
}
