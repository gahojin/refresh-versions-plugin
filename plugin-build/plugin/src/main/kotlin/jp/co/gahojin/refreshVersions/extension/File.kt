/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions.extension

import java.io.File

fun File.writeTextWhenUpdated(newText: String) {
    val oldText = readText()
    if (oldText != newText) {
        writeText(newText)
    }
}
