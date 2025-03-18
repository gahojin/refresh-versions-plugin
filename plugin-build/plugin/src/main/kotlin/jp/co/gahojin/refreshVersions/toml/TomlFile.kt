/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions.toml

import jp.co.gahojin.refreshVersions.Constants
import java.io.Reader
import kotlin.collections.component1
import kotlin.collections.component2

data class TomlFile(
    val sections: MutableMap<TomlSection, List<TomlLine>>,
) {
    val isEmpty: Boolean
        get() = sections.size <= 1 && sections[TomlSection.Root]?.isEmpty() ?: true

    override fun toString() = format(false)

    internal operator fun get(section: TomlSection): List<TomlLine> {
        return sections[section] ?: emptyList()
    }

    internal operator fun set(section: TomlSection, lines: List<TomlLine>) {
        sections[section] = lines
    }

    fun format(sortSection: Boolean): String = buildString {
        initializeRoot()
        val tmpSections = if (sortSection) sortedSections() else sections.keys

        for (section in tmpSections) {
            val lines = get(section)
            if (lines.isNotEmpty()) {
                if (section != TomlSection.Root) {
                    appendLine()
                    append('[').append(section.name).appendLine(']')
                    appendLine()
                }
                val content = lines.removeDuplicateBlanks()
                    .joinToString(prefix = "\n", separator = "\n", postfix = "\n") { it.text }
                    .trim()
                appendLine(content)
            }
        }
    }

    /**
     * 以前のバージョンコメントの削除
     */
    fun removeComments() {
        sections
            .filterNot { it.key == TomlSection.Root }
            .forEach { (section, lines) ->
                set(section, lines.filterNot { it.text.startsWith("##") })
            }
    }

    private fun List<TomlLine>.removeDuplicateBlanks(): List<TomlLine> {
        var isPreviousNotBlank = false
        return filter {
            val current = it.text.isNotBlank()
            val result = isPreviousNotBlank || current
            isPreviousNotBlank = current
            return@filter result
        }
    }

    private fun initializeRoot() {
        var rootLines = get(TomlSection.Root)
        if (rootLines.none { it.text.isNotBlank() }) {
            this[TomlSection.Root] = listOf(
                TomlLine(TomlSection.Root, "## Generated by $ ./gradlew refreshVersions"),
                TomlLine.NEW_LINE,
            )
        } else {
            var isReplaced = false
            rootLines = rootLines.map {
                if (!isReplaced && it.text.startsWith("## Generated by ")) {
                    isReplaced = true
                    it.copy(text = "## Generated by $ ./gradlew refreshVersions")
                } else {
                    it
                }
            }
            set(TomlSection.Root, rootLines)
        }
    }

    private fun sortedSections() = (Constants.orderTomlSections + sections.keys).toSet()

    companion object {
        fun parseToml(reader: Reader): TomlFile {
            val result = linkedMapOf<TomlSection, List<TomlLine>>()
            var section: TomlSection = TomlSection.Root
            var current = mutableListOf<TomlLine>()
            result[section] = current

            reader.forEachLine { line ->
                val trimmedLine = line.trim()
                if (trimmedLine.startsWith("[") && trimmedLine.endsWith("]")) {
                    // in Section
                    section = TomlSection.from(trimmedLine.trim('[', ']'))
                    current = mutableListOf()
                    result[section] = current
                } else {
                    current.add(TomlLine(section, trimmedLine))
                }
            }
            return TomlFile(result)
        }
    }
}
