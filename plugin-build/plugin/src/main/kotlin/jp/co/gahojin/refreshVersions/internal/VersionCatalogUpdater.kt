/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions.internal

import jp.co.gahojin.refreshVersions.Constants
import jp.co.gahojin.refreshVersions.model.UpdatableDependency
import jp.co.gahojin.refreshVersions.toml.TomlFile
import jp.co.gahojin.refreshVersions.toml.TomlLine
import jp.co.gahojin.refreshVersions.toml.TomlSection

internal object VersionCatalogUpdater {
    fun execute(
        toml: TomlFile,
        section: TomlSection,
        dependencies: List<UpdatableDependency>,
    ) {
        // バージョン
        val versions = toml[TomlSection.Versions]
        // 対象依存
        val targetDependencies = toml[section]
        // 対象依存からバージョン参照
        val versionRefs = targetDependencies.filter {
            it.versionRef.isNotEmpty()
        }.groupBy { it.versionRef }

        toml[TomlSection.Versions] = versions.flatMap { initialLine -> sequence {
            // 現在の行を出力
            yield(initialLine)

            // 空行やバージョン未定義の場合、何もしない
            if (initialLine.isEmptyLine) {
                return@sequence
            }

            // 一致する依存情報を抽出し、アップデート可能なバージョン一覧を取得する
            val updatableDependency = versionRefs[initialLine.key]?.firstNotNullOfOrNull { line ->
                dependencies.firstOrNull {
                    it.dependency.moduleId == line.moduleId
                }
            }

            // 行追加
            updateLines(initialLine, updatableDependency)
        } }
        toml[section] = targetDependencies.flatMap { initialLine -> sequence {
            // 現在の行を出力
            yield(initialLine)

            // 空行やバージョン参照の場合、何もしない
            if (initialLine.isEmptyLine || initialLine.versionRef.isNotEmpty()) {
                return@sequence
            }

            // 一致する依存情報を抽出し、アップデート可能なバージョン一覧を取得する
            val updatableDependency = dependencies
                .firstOrNull { it.dependency.moduleId == initialLine.moduleId }

            // 行追加
            updateLines(initialLine, updatableDependency)
        } }
    }

    private suspend fun SequenceScope<TomlLine>.updateLines(
        line: TomlLine,
        updatableDependency: UpdatableDependency?,
    ) {
        updatableDependency ?: return

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
