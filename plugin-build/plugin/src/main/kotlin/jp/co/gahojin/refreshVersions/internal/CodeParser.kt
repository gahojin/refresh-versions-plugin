/*
 * (C) 2025 GAHOJIN, Inc.
 */
package jp.co.gahojin.refreshVersions.internal

import jp.co.gahojin.refreshVersions.Constants.addCommentRegex
import kotlinx.coroutines.flow.flow
import java.io.Reader

/**
 * コメントをスキップするための簡素なコードパーサー.
 */
@Suppress("MemberVisibilityCanBePrivate")
internal object CodeParser {
    private val pluginsBlockRegex = """plugins\s*\{""".toRegex()
    private val idMethodRegex = """id\s*\(""".toRegex()

    suspend fun parse(reader: Reader, visitor: Visitor) {
        var inIdMethod = false

        parse(reader).collect {
            if (it.isIdMethod) {
                inIdMethod = true
                visitor.visitPlugin(it)
                return@collect
            }
            if (inIdMethod) {
                when (it.state) {
                    is State.OneLineComment -> {
                        if (addCommentRegex.containsMatchIn(it.trimText)) {
                            visitor.visitComment(it)
                            return@collect
                        }
                    }
                    is State.General -> {
                        if (it.trimText.isNotEmpty()) {
                            inIdMethod = false
                        }
                    }
                    else -> {
                        inIdMethod = false
                    }
                }
            }
            visitor.visitOther(it)
        }
    }

    /**
     * pluginsブロック内のidメソッド呼出箇所検知
     */
    fun parse(reader: Reader) = flow {
        var inPlugins = false
        var idData: Result? = null

        suspend fun emitIdData() {
            idData?.also { emit(it) }
            idData = null
        }

        parseInternal(reader.readText())
            // 改行や3重クオートは処理しない
            .collect {
                val state = it.state
                when (state) {
                    is State.BlockCode -> {
                        if (inPlugins) {
                            inPlugins = false
                            emitIdData()
                        } else if (pluginsBlockRegex.matches(it.trimText)) {
                            inPlugins = true
                        }
                        emit(it)
                    }
                    is State.General -> {
                        val isContainNewLine = it.isContainNewLine
                        val endInclusive = if (isContainNewLine) it.range.endInclusive - 1 else it.range.endInclusive
                        idData?.updateRange(endInclusive)?.also {
                            if (isContainNewLine) {
                                emitIdData()
                            }
                        } ?: run {
                            emit(it)
                        }
                    }
                    is State.OneLineComment -> {
                        val endInclusive = if (it.isContainNewLine) it.range.endInclusive - 1 else it.range.endInclusive
                        idData?.updateRange(endInclusive)?.also {
                            emitIdData()
                        } ?: run {
                            emit(it)
                        }
                    }
                    is State.InComment -> {
                        if (state.isMultiLine) {
                            emitIdData()
                        }
                        idData?.updateRange(it.range.endInclusive) ?: run {
                            emit(it)
                        }
                    }
                    is State.InStringSingleQuote, is State.InStringDoubleQuote -> {
                        idData?.updateRange(it.range.endInclusive) ?: run {
                            emit(it)
                        }
                    }
                    is State.MethodCallCode -> {
                        if (idMethodRegex.matches(it.trimText)) {
                            idData?.also { prev ->
                                prev.updateRange(it.range.start)
                                emit(prev)
                            }
                            idData = it.also {
                                it.isIdMethod = true
                            }
                        } else {
                            idData ?: run {
                                emit(it)
                            }
                        }
                    }
                    else -> emit(it)
                }
            }
    }

    internal fun parseInternal(code: CharSequence) = flow {
        var state: State = State.General
        var startCodeIndex = 0
        var isNextEscapedChar = false

        var index = 0
        val length = code.length
        while (index < length) {
            val c = code[index]
            when {
                isNextEscapedChar -> {
                    isNextEscapedChar = false
                    index++
                    continue
                }
                c == '\\' -> {
                    isNextEscapedChar = true
                    index++
                    continue
                }
                else -> {
                    val prevIndex = index
                    val prevState = state
                    index += state.nextState(code, prevIndex) { nextState, nextOffset, fireEmit ->
                        state = nextState

                        if (fireEmit) {
                            val endIndex = prevIndex + nextOffset
                            if (endIndex > startCodeIndex) {
                                val range = startCodeIndex..<endIndex
                                emit(Result(code, prevState, range))
                            }
                            startCodeIndex = index + nextOffset
                        }
                    }
                    check(index > prevIndex)
                }
            }
        }
        if (state.isCode) {
            val range = startCodeIndex..<index
            emit(Result(code, state, range))
        }
    }

    interface Visitor {
        fun visitPlugin(result: Result)
        fun visitComment(result: Result)
        fun visitOther(result: Result)
    }

    class Result(
        private val code: CharSequence,
        val state: State,
        var range: IntRange,
        var isIdMethod: Boolean = false,
    ) {
        val trimText: CharSequence
            get() = rawText.trim()

        val rawText: CharSequence
            get() = code.subSequence(range)

        val isContainNewLine: Boolean
            get() = rawText.contains('\n')

        val previousNewLinePos: Int
            get() = code.lastIndexOf('\n', range.start)

        fun updateRange(endInclusive: Int) = apply {
            range = range.start..endInclusive
        }
    }

    sealed interface State {
        val isCode: Boolean
        suspend fun nextState(code: CharSequence, index: Int, block: suspend (state: State, offset: Int, emit: Boolean) -> Unit) : Int

        class InComment(
            val prevState: State,
        ) : State {
            override val isCode = false
            var isMultiLine = false
            override suspend fun nextState(code: CharSequence, index: Int, block: suspend (State, Int, Boolean) -> Unit): Int {
                /* /* */のような、コメントの中のコメントを処理する */
                if (code.startsWith("/*", index)) {
                    block(InComment(prevState), 0, true)
                    return 2
                }
                if (code.startsWith("*/", index)) {
                    block(prevState, 2, true)
                    return 2
                }
                if (code[index] == '\n') {
                    isMultiLine = true
                }
                return 1
            }
        }
        class OneLineComment(
            val prevState: State,
        ) : State {
            override val isCode = false
            override suspend fun nextState(code: CharSequence, index: Int, block: suspend (State, Int, Boolean) -> Unit): Int {
                if (code[index] == '\n') {
                    block(prevState, 1, true)
                }
                return 1
            }
        }
        class InStringSingleQuote(
            val prevState: State,
        ) : State {
            override val isCode = true
            override suspend fun nextState(code: CharSequence, index: Int, block: suspend (State, Int, Boolean) -> Unit): Int {
                if (code[index] == '\'') {
                    block(prevState, 1, true)
                }
                return 1
            }
        }
        class InStringDoubleQuote(
            private val prevState: State,
        ) : State {
            override val isCode = true
            override suspend fun nextState(code: CharSequence, index: Int, block: suspend (State, Int, Boolean) -> Unit): Int {
                if (code[index] == '"') {
                    block(prevState, 1, true)
                }
                return 1
            }
        }
        class InStringTripleQuote(
            private val prevState: State,
            override val isCode: Boolean,
        ) : State {
            override suspend fun nextState(code: CharSequence, index: Int, block: suspend (State, Int, Boolean) -> Unit): Int {
                if (code.startsWith("\"\"\"", index)) {
                    block(prevState, 3, true)
                    return 3
                }
                if (code[index] == '\n' && isCode) {
                    // 3重クォートによる文字列の場合、複数行になった時点で、プラグイン情報が入る可能性がないため、コードとみなさない
                    block(InStringTripleQuote(prevState, false), 1, true)
                }
                return 1
            }
        }
        abstract class Code : State {
            override val isCode = true
            override suspend fun nextState(code: CharSequence, index: Int, block: suspend (State, Int, Boolean) -> Unit) : Int {
                if (code.startsWith("//", index)) {
                    block(OneLineComment(this), 0, true)
                    return 2
                }
                if (code.startsWith("/*", index)) {
                    block(InComment(this), 0, true)
                    return 2
                }
                if (code.startsWith("\"\"\"", index)) {
                    block(InStringTripleQuote(this, true), 0, true)
                    return 3
                }
                val c = code[index]
                when (c) {
                    '"' -> block(InStringDoubleQuote(this), 0, true)
                    '\'' -> block(InStringSingleQuote(this), 0, true)
                    else -> {
                        return nextState(c, code, index, block)
                    }
                }
                return 1
            }
            open suspend fun nextState(c: Char, code: CharSequence, index: Int, block: suspend (State, Int, Boolean) -> Unit): Int = 1
        }
        object General : Code() {
            override suspend fun nextState(c: Char, code: CharSequence, index: Int, block: suspend (State, Int, Boolean) -> Unit): Int {
                when (c) {
                    '{' -> block(BlockCode(this), 0, false)
                    '(' -> block(MethodCallCode(this), 0, false)
                    '\n' -> block(General, 1, true)
                }
                return super.nextState(c, code, index, block)
            }
        }
        class BlockCode(
            private val prevState: State,
        ) : Code() {
            override suspend fun nextState(c: Char, code: CharSequence, index: Int, block: suspend (State, Int, Boolean) -> Unit): Int {
                when (c) {
                    '{' -> block(BlockCode(this), 0, true)
                    '(' -> block(MethodCallCode(this), 0, true)
                    '}' -> block(prevState, 1, true)
                    else -> block(General, 0, true)
                }
                return super.nextState(c, code, index, block)
            }
        }
        class MethodCallCode(
            private val prevState: State,
        ) : Code() {
            override suspend fun nextState(c: Char, code: CharSequence, index: Int, block: suspend (State, Int, Boolean) -> Unit): Int {
                if (c == ')') {
                    block(prevState, 1, true)
                }
                return 1
            }
        }
    }
}
