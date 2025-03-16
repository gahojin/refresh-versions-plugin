/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions.model

/**
 * バージョン修飾子
 */
enum class VersionQualifier(
    val value: String,
    val order: Int,
) {
    ALPHA("alpha", 1),
    BETA("beta", 2),
    MILESTONE("milestone", 3),
    RELEASE_CANDIDATE("rc", 4),
    SNAPSHOT("snapshot", 5),
    RELEASE("", 6),
    SERVICE_PACK("sp", 7)
    ;

    companion object {
        private val cache = entries.associateBy({ it.value.lowercase() }, { it.order.toString() })
        val releaseOrder = RELEASE.order.toString()
        private val maxOrder = entries.maxOf { it.order } + 1

        fun getComparableQualifier(value: String): String {
            val key = value.lowercase()
            return cache.getOrElse(key) {
                "${maxOrder}-${key}"
            }
        }
    }
}
