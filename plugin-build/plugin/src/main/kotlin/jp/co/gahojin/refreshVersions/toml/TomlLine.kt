/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions.toml

data class TomlLine(
    val section: TomlSection,
    val text: String,
) {
    override fun toString(): String = text

    companion object {
        val NEW_LINE = TomlLine(TomlSection.Custom("blank"), "")
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
