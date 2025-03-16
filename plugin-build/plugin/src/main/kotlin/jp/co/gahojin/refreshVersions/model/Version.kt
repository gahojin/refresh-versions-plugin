/*
 * Copyright 2001-2019 The Apache Software Foundation
 * (C) 2025 GAHOJIN, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 */
package jp.co.gahojin.refreshVersions.model

import jp.co.gahojin.refreshVersions.model.Item.CombinationItem
import jp.co.gahojin.refreshVersions.model.Item.ListItem
import jp.co.gahojin.refreshVersions.model.Item.NumberItem
import jp.co.gahojin.refreshVersions.model.Item.StringItem
import java.math.BigInteger

/**
 * バージョン情報.
 *
 * ロジック参考
 * [Reference Code](https://github.com/apache/maven/blob/master/compat/maven-artifact/src/main/java/org/apache/maven/artifact/versioning/ComparableVersion.java)
 */
class Version(
    val value: String,
) : Comparable<Version> {
    internal val items: ListItem = parseVersion(value.lowercase())

    override fun compareTo(other: Version): Int {
        return items.compareTo(other.items)
    }

    override fun equals(other: Any?) = items == (other as? Version)?.items
    override fun hashCode() = items.hashCode()
    override fun toString() = items.toString()

    companion object {
        private fun parseVersion(version: String): ListItem {
            val items = ListItem()
            val stack = mutableListOf(items)

            var list = items

            var isDigit = false
            var isCombination = false

            var startIndex = 0

            var i = 0
            while (i < version.length) {
                val character = version[i]
                var c = character.code
                if (Character.isHighSurrogate(character)) {
                    // read the next character as a low surrogate and combine into a single int
                    try {
                        val low = version[i + 1]
                        val both = charArrayOf(character, low)
                        c = Character.codePointAt(both, 0)
                        i++
                    } catch (_: IndexOutOfBoundsException) {
                        // high surrogate without low surrogate. Not a lot we can do here except treat it as a regular character
                    }
                }

                if (c == '.'.code) {
                    if (i == startIndex) {
                        list.add(NumberItem.ZERO)
                    } else {
                        list.add(parseItem(isCombination, isDigit, version.substring(startIndex, i)))
                    }
                    isCombination = false
                    startIndex = i + 1
                } else if (c == '-'.code) {
                    if (i == startIndex) {
                        list.add(NumberItem.ZERO)
                    } else {
                        // X-1 is going to be treated as X1
                        if (!isDigit && i != version.length - 1) {
                            val c1 = version[i + 1]
                            if (Character.isDigit(c1)) {
                                isCombination = true
                                i++
                                continue
                            }
                        }
                        list.add(parseItem(isCombination, isDigit, version.substring(startIndex, i)))
                    }
                    startIndex = i + 1

                    if (list.isNotEmpty) {
                        list = list.addListItem(stack)
                    }
                    isCombination = false
                } else if (c in '0'.code..'9'.code) { // Check for ASCII digits only
                    if (!isDigit && i > startIndex) {
                        // X1
                        isCombination = true

                        if (list.isNotEmpty) {
                            list = list.addListItem(stack)
                        }
                    }

                    isDigit = true
                } else {
                    if (isDigit && i > startIndex) {
                        list.add(parseItem(isCombination, true, version.substring(startIndex, i)))
                        startIndex = i

                        list = list.addListItem(stack)
                        isCombination = false
                    }

                    isDigit = false
                }
                i++
            }

            if (version.length > startIndex) {
                // 1.0.0.X1 < 1.0.0-X2
                // treat .X as -X for any string qualifier X
                if (!isDigit && list.isNotEmpty) {
                    list = list.addListItem(stack)
                }

                list.add(parseItem(isCombination, isDigit, version.substring(startIndex)))
            }

            do {
                list = stack.removeLastOrNull() ?: break
                list.normalize()
            } while (true)

            return items
        }

        internal fun parseItem(
            isCombination: Boolean,
            isDigit: Boolean,
            buf: String,
        ): Item {
            return when {
                isCombination -> CombinationItem(buf.replace("-", ""))
                isDigit -> NumberItem(stripLeadingZeroes(buf))
                else -> StringItem.create(buf)
            }
        }

        private fun stripLeadingZeroes(buf: String): String {
            return if (buf.isEmpty()) {
                "0"
            } else {
                for ((i, c) in buf.withIndex()) {
                    if (c != '0') {
                        return buf.substring(i)
                    }
                }
                return buf
            }
        }
    }
}


/**
 * 該当のバージョン以降で絞り込む
 */
fun Set<Version>.filterAfter(target: String): List<Version> {
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
    return if (isMatch) result else toList()
}

internal sealed interface Item : Comparable<Item?> {
    /** 空や0か */
    val isNull: Boolean

    /**
     * バージョンの数値部.
     */
    class NumberItem(
        val value: BigInteger,
    ) : Item {
        constructor(value: String) : this(BigInteger(value))

        override val isNull: Boolean
            get() = BigInteger.ZERO == value

        override fun compareTo(other: Item?): Int {
            return other?.let {
                when (it) {
                    is NumberItem -> value.compareTo(it.value)
                    is StringItem -> 1
                    is CombinationItem -> 1 // 1.1 > 1-sp
                    is ListItem -> 1 // 1.1 > 1-1
                }
            } ?: run {
                // 1.0 == 1, 1.1 > 1
                if (BigInteger.ZERO == value) 0 else 1
            }
        }

        override fun equals(other: Any?) = value == (other as? NumberItem)?.value
        override fun hashCode() = value.hashCode()
        override fun toString() = value.toString()

        companion object {
            val ZERO = NumberItem(BigInteger.ZERO)
        }
    }

    /**
     * バージョンの文字列部
     */
    class StringItem private constructor(
        private val value: String,
    ) : Item {
        private val qualifier: String = VersionQualifier.getComparableQualifier(value)

        override val isNull: Boolean
            get() = value.isEmpty()

        override fun compareTo(other: Item?): Int {
            return other?.let {
                when (it) {
                    is NumberItem -> -1 // 1.any < 1.1 ?
                    is StringItem -> qualifier.compareTo(it.qualifier)
                    is CombinationItem -> {
                        val result = compareTo(it.stringPart)
                        if (result == 0) -1 else result
                    }
                    is ListItem -> -1 // 1.any < 1-1
                }
            } ?: run {
                // 1-rc < 1, 1-ga > 1
                return qualifier.compareTo(VersionQualifier.releaseOrder)
            }
        }

        override fun equals(other: Any?) = value == (other as? StringItem)?.value
        override fun hashCode() = value.hashCode()
        override fun toString() = value

        companion object {
            private val aliases = mapOf(
                "cr" to "rc",
                "ga" to "",
                "final" to "",
                "release" to "",
            )

            fun create(value: String): StringItem {
                return StringItem(aliases.getOrDefault(value, value))
            }

            fun withFollowByDigit(value: String): StringItem {
                if (value.length == 1) {
                    // a1 = alpha-1, b1 = beta-1, m1 = milestone-1
                    val newValue = when (value[0]) {
                        'a' -> "alpha"
                        'b' -> "beta"
                        'm' -> "milestone"
                        else -> value
                    }
                    return create(newValue)
                }
                return create(value)
            }
        }
    }

    /**
     * 文字列と数値の組み合わせ.
     *
     * 文字列が最初、数値が後にくる
     *
     * ex. alpha1 や RC2
     */
    class CombinationItem(value: String) : Item {
        internal val digitPart: Item
        internal val stringPart: StringItem

        init {
            val index = value.indices.firstOrNull {
                Character.isDigit(value[it])
            } ?: 0

            stringPart = StringItem.withFollowByDigit(value.substring(0, index))
            digitPart = Version.parseItem(isCombination = false, isDigit = true, buf = value.substring(index))
        }

        override val isNull = false

        override fun compareTo(other: Item?): Int {
            return other?.let {
                when (it) {
                    is NumberItem -> -1
                    is StringItem -> {
                        val result = stringPart.compareTo(it)
                        // X1 > X
                        if (result == 0) 1 else result
                    }
                    is ListItem -> -1
                    is CombinationItem -> {
                        val result = stringPart.compareTo(it.stringPart)
                        if (result == 0) digitPart.compareTo(it.digitPart) else result
                    }
                }
            } ?: run {
                // 1-rc1 < 1, 1-ga1 > 1
                stringPart.compareTo(null)
            }
        }

        override fun equals(other: Any?) = (other as? CombinationItem)?.let {
            stringPart == it.stringPart && digitPart == it.digitPart
        } ?: false
        override fun hashCode() = stringPart.hashCode() + 31 * digitPart.hashCode()
        override fun toString() = stringPart.toString() + digitPart.toString()
    }

    /**
     * -や.区切りをバージョン部分をひとまとめにしたもの
     */
    class ListItem : Item {
        private val items = mutableListOf<Item>()

        override val isNull: Boolean
            get() = items.isEmpty()

        val isNotEmpty: Boolean
            get() = items.isNotEmpty()

        fun add(item: Item) {
            items.add(item)
        }

        operator fun get(index: Int): Item {
            return items[index]
        }

        fun addListItem(stack: MutableList<ListItem>): ListItem {
            return ListItem().also {
                add(it)
                stack.add(it)
            }
        }

        fun normalize() {
            var index = items.size
            while (index > 0) {
                val lastItem = items[--index]
                if (lastItem.isNull) {
                    // a-b, --, 1.0.0 -> ab, -, 1
                    if (index == items.size - 1 || items[index + 1] is StringItem) {
                        items.removeAt(index)
                    } else {
                        val nextItem = items[index + 1]
                        if (nextItem is ListItem) {
                            val item = nextItem[0]
                            if (item is CombinationItem || item is StringItem) {
                                items.removeAt(index)
                            }
                        }
                    }
                }
            }
        }

        override fun compareTo(other: Item?): Int {
            return other?.let {
                when (it) {
                    is NumberItem -> -1
                    is StringItem -> 1
                    is CombinationItem -> 1 // 1-1 > 1-sp
                    is ListItem -> {
                        val left = items.iterator()
                        val right = it.items.iterator()

                        while (left.hasNext() || right.hasNext()) {
                            val l = if (left.hasNext()) left.next() else null
                            val r = if (right.hasNext()) right.next() else null

                            // if this is shorter, then invert the compare and mul with -1
                            val result = l?.compareTo(r) ?: r?.let { -1 * r.compareTo(l) } ?: 0
                            if (result != 0) {
                                return@let result
                            }
                        }
                        return@let 0
                    }
                }
            } ?: run {
                // 1-0 = 1- (normalize) = 1
                if (items.isEmpty()) {
                    0
                } else {
                    // Compare the entire list of items with null - not just the first one, MNG-6964
                    items.map { it.compareTo(null) }.firstOrNull { it != 0 } ?: 0
                }
            }
        }

        override fun equals(other: Any?) = items == (other as? ListItem)?.items
        override fun hashCode() = items.hashCode()
        override fun toString() = buildString {
            for ((count, item) in items.withIndex()) {
                if (count > 0) {
                    if (item is ListItem) {
                        append('-')
                    } else {
                        append('.')
                    }
                }
                append(item)
            }
        }
    }
}
