/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions.internal

import jp.co.gahojin.refreshVersions.extension.writeTextWhenUpdated
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.Reader

internal object SettingsCleaner {
    fun execute(files: List<File>) {
        files.forEach { file ->
            val content = file.bufferedReader().use {
                execute(it)
            }
            file.writeTextWhenUpdated(content)
        }
    }

    internal fun execute(reader: Reader) = buildString {
        runBlocking {
            CodeParser.parse(reader, object : CodeParser.Visitor {
                override fun visitPlugin(result: CodeParser.Result) {
                    this@buildString.appendLine(result.rawText)
                }

                override fun visitComment(result: CodeParser.Result) = Unit

                override fun visitOther(result: CodeParser.Result) {
                    this@buildString.append(result.rawText)
                }
            })
        }
    }
}
