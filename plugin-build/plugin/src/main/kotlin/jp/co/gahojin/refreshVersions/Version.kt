/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions

/**
 * バージョン情報
 */
@JvmInline
value class Version(val value: String)

/**
 * 該当のバージョン以降で絞り込む
 */
fun List<Version>.filterAfter(target: String?): List<Version> {
    // 対象バージョンが一覧に存在したか
    var isMatch = false
    val result = mutableListOf<Version>()

    for (version in this) {
        if (isMatch) {
            result.add(version)
        }
        if (version.value == target) {
            isMatch = true
        }
    }
    // 1つもマッチしない場合、全てのバージョンを返す
    return if (isMatch) result else this
}