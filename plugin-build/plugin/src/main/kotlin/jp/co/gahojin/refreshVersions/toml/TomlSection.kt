/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions.toml

sealed class TomlSection(open val name: String) {
    data object Root : TomlSection("root")
    data object Versions : TomlSection("versions")
    data object Libraries : TomlSection("libraries")
    data object Bundles : TomlSection("bundles")
    data object Plugins : TomlSection("plugins")
    data class Custom(override val name: String) : TomlSection(name)

    companion object {
        private val sections by lazy {
            arrayOf(Root, Bundles, Plugins, Versions, Libraries)
                .associateBy { it.name }
                .toMutableMap()
        }

        @JvmStatic
        fun from(name: String): TomlSection {
            return sections.getOrPut(name) { Custom(name) }
        }
    }
}
