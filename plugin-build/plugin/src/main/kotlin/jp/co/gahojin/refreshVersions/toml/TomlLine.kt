/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions.toml

data class TomlLine(
    val section: TomlSection,
    val text: String,
) {
    val textWithoutComment = text.substringBefore('#')
    val key = textWithoutComment.substringBefore('=', missingDelimiterValue = "").trim()
    val hasKey = key.isNotBlank()

    val value = if (hasKey) textWithoutComment.substringAfter('=').trim() else ""

    val attributes: Map<String, String> = parseLine(section, value)

    val id by attributes

    val group: String
        get() = if (section == TomlSection.Plugins) id else attributes.getOrDefault("group", "")

    val name: String
        get() = if (section == TomlSection.Plugins) "$id.gradle.plugin" else attributes.getOrDefault("name", "")

    val version: String
        get() = if (section == TomlSection.Versions) value.unquote() else attributes.getOrDefault("version", "")

    val versionRef: String
        get() = attributes.getOrDefault("version.ref", "")

    override fun toString(): String = "TomlLine(section=${section}, key=${key}, value=${value}, attributes=${attributes}\n$text"

    companion object {
        val NEW_LINE = TomlLine(TomlSection.Custom("blank"), "")

        private fun String.unquote() = trim().removeSurrounding("\"").trim()

        private fun parseLine(section: TomlSection, value: String): Map<String, String> {
            val splitByColon = value.split(':')

            return when (section) {
                TomlSection.Root, TomlSection.Bundles, TomlSection.Versions -> emptyMap<String, String>()
                TomlSection.Plugins -> {
                    if (value.startsWith('{')) {
                        getAttributes(value)
                    } else if (splitByColon.size > 1) {
                        buildMap<String, String> {
                            put("id", splitByColon[0])
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
                                    attributes["group"] = tmp[0].trim()
                                    attributes["name"] = tmp[1].trim()
                                }
                                attributes.remove("module")
                            } ?: attributes
                        }
                    } else buildMap<String, String> {
                        put("group", splitByColon[0])
                        if (splitByColon.size > 1) put("name", splitByColon[1])
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

internal fun TomlLine(
    section: TomlSection,
    key: String,
    value: String,
): TomlLine = TomlLine(
    section = section,
    text = "$key = \"$value\"",
)

internal fun TomlLine(
    section: TomlSection,
    key: String,
    values: Map<String, String>,
): TomlLine {
    require(values.isNotEmpty())
    val formatMap = values.entries.joinToString(", ") { (k, v) ->
        "$k = \"$v\""
    }
    return TomlLine(section = section, key = key, value = formatMap)
}
