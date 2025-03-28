/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions.internal

import kotlinx.coroutines.runBlocking
import org.gradle.api.provider.Property
import java.io.File
import java.io.Reader

internal object SettingsCleaner {
    fun execute(
        files: List<Property<File>>,
    ) {
        files.mapNotNull { it.orNull }
            .forEach { file ->
                val newContent = execute(file.reader())
                file.writeText(newContent)
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
