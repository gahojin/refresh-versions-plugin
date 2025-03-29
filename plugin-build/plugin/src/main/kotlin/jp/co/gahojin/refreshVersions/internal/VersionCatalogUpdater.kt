/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions.internal

import jp.co.gahojin.refreshVersions.Constants
import jp.co.gahojin.refreshVersions.extension.writeTextWhenUpdated
import jp.co.gahojin.refreshVersions.model.UpdatableDependency
import jp.co.gahojin.refreshVersions.toml.TomlFile
import jp.co.gahojin.refreshVersions.toml.TomlLine
import jp.co.gahojin.refreshVersions.toml.TomlSection
import java.io.File

internal object VersionCatalogUpdater {
    fun execute(
        file: File,
        libraryDependencies: List<UpdatableDependency>,
        pluginDependencies: List<UpdatableDependency>,
        sortSection: Boolean,
    ) {
        val content = file.bufferedReader().use {
            val tomlFile = TomlFile.parseToml(it)

            // バージョンカタログファイルを更新
            tomlFile.removeComments()
            execute(tomlFile, libraryDependencies, pluginDependencies)

            tomlFile.format(sortSection)
        }

        file.writeTextWhenUpdated(content)
    }

    fun execute(
        toml: TomlFile,
        libraryDependencies: List<UpdatableDependency>,
        pluginDependencies: List<UpdatableDependency>,
    ) {
        val versionRefMappings = mutableMapOf<String, UpdatableDependency>()
        execute(toml, TomlSection.Libraries, libraryDependencies) { versionRef, dependency ->
            versionRefMappings.putIfAbsent(versionRef, dependency)
        }
        execute(toml, TomlSection.Plugins, pluginDependencies) { versionRef, dependency ->
            versionRefMappings.putIfAbsent(versionRef, dependency)
        }
        executeVersionRef(toml, versionRefMappings)
    }

    fun execute(
        toml: TomlFile,
        section: TomlSection,
        dependencies: List<UpdatableDependency>,
        onDetectVersionRef: (versionRef: String, UpdatableDependency) -> Unit,
    ) {
        // 対象依存
        val targetDependencies = toml[section]

        toml[section] = targetDependencies.flatMap { initialLine -> sequence {
            // 現在の行を出力
            yield(initialLine)

            // 空行の場合、何もしない
            if (initialLine.isEmptyLine) {
                return@sequence
            }

            // 一致する依存情報を抽出し、アップデート可能なバージョン一覧を取得する
            val updatableDependency = dependencies.firstOrNull {
                it.dependency.moduleId == initialLine.moduleId
            } ?: return@sequence

            val versionRef = initialLine.versionRef
            if (versionRef.isNotEmpty()) {
                onDetectVersionRef(versionRef, updatableDependency)
            } else {
                // 行追加
                updateLines(initialLine, updatableDependency)
            }
        } }
    }

    fun executeVersionRef(
        toml: TomlFile,
        versionRefMappings: Map<String, UpdatableDependency>,
    ) {
        // バージョン
        val versions = toml[TomlSection.Versions]

        toml[TomlSection.Versions] = versions.flatMap { initialLine -> sequence {
            // 現在の行を出力
            yield(initialLine)

            // 空行の場合、何もしない
            if (initialLine.isEmptyLine) {
                return@sequence
            }

            // 一致する依存情報を抽出し、アップデート可能なバージョン一覧を取得する
            val updatableDependency = versionRefMappings[initialLine.key] ?: return@sequence

            // 行追加
            updateLines(initialLine, updatableDependency)
        } }
    }

    private suspend fun SequenceScope<TomlLine>.updateLines(
        line: TomlLine,
        updatableDependency: UpdatableDependency,
    ) {
        // バージョン番号より前の文字列を生成する
        val versionPos = line.text.indexOf(line.version)
        val prefix = buildString {
            // ##    ^ "x.y.z"
            // ^^^^^^^^^ <-この箇所を生成する
            append("##")
            append(" ".repeat((versionPos - 5).coerceAtLeast(0)))
            append("${Constants.VERSION_SYMBOL} ")
        }
        // バージョン番号より後の文字列を生成する
        val postfix = line.text.substring((versionPos + line.version.length + 1).coerceAtMost(line.text.length))

        updatableDependency.updatableVersions.forEach { version ->
            yield(TomlLine(line.section, "$prefix\"${version.value}\"$postfix"))
        }
    }
}
