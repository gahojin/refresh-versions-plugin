/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions.internal

import jp.co.gahojin.refreshVersions.Constants
import jp.co.gahojin.refreshVersions.Constants.pluginDslRegex
import jp.co.gahojin.refreshVersions.extension.writeTextWhenUpdated
import jp.co.gahojin.refreshVersions.model.UpdatableDependency
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.Reader

internal object SettingsUpdater {
    fun execute(
        files: List<File>,
        dependencies: List<UpdatableDependency>,
    ) {
        files.forEach { file ->
            val content = file.bufferedReader().use {
                execute(it, dependencies)
            }
            file.writeTextWhenUpdated(content)
        }
    }

    internal fun execute(
        reader: Reader,
        dependencies: List<UpdatableDependency>,
    ) = buildString {
        runBlocking {
            CodeParser.parse(reader, object : CodeParser.Visitor {
                override fun visitPlugin(result: CodeParser.Result) {
                    val line = result.rawText
                    this@buildString.appendLine(line)

                    // プラグイン情報抽出
                    pluginDslRegex.find(line.trim())?.also { match ->
                        dependencies.firstOrNull {
                            it.dependency.moduleId.group == match.groupValues[1]
                        }?.also {
                            // バージョン番号より前の文字列を生成する
                            val previousNewLinePos = result.previousNewLinePos
                            val versionPos = line.indexOf(match.groupValues[2])
                            val delimiter = line[versionPos - 1]
                            val prefix = buildString {
                                // //    ^ "x.y.z"
                                // ^^^^^^^^^ <-この箇所を生成する
                                append("//")
                                append(" ".repeat((result.range.first - previousNewLinePos + versionPos - 6).coerceAtLeast(0)))
                                append("${Constants.VERSION_SYMBOL} ")
                            }
                            it.updatableVersions.forEach { version ->
                                this@buildString.append(prefix).append(delimiter).append(version.value).appendLine(delimiter)
                            }
                        }
                    }
                }

                override fun visitComment(result: CodeParser.Result) = Unit

                override fun visitOther(result: CodeParser.Result) {
                    this@buildString.append(result.rawText)
                }
            })
        }
    }
}
