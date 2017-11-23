/*
 *      KWCommands - New generation of WCommands written in Kotlin <https://github.com/JonathanxD/KWCommands>
 *
 *         The MIT License (MIT)
 *
 *      Copyright (c) 2017 JonathanxD
 *      Copyright (c) contributors
 *
 *
 *      Permission is hereby granted, free of charge, to any person obtaining a copy
 *      of this software and associated documentation files (the "Software"), to deal
 *      in the Software without restriction, including without limitation the rights
 *      to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *      copies of the Software, and to permit persons to whom the Software is
 *      furnished to do so, subject to the following conditions:
 *
 *      The above copyright notice and this permission notice shall be included in
 *      all copies or substantial portions of the Software.
 *
 *      THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *      IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *      FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *      AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *      LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *      OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *      THE SOFTWARE.
 */
package com.github.jonathanxd.kwcommands.util

import com.github.jonathanxd.iutils.opt.specialized.OptObject
import com.github.jonathanxd.jwiutils.kt.none
import com.github.jonathanxd.jwiutils.kt.some
import com.github.jonathanxd.kwcommands.exception.ListParseException
import com.github.jonathanxd.kwcommands.exception.MapParseException
import com.github.jonathanxd.kwcommands.exception.listParseException
import com.github.jonathanxd.kwcommands.exception.mapParseException
import com.github.jonathanxd.kwcommands.parser.Input
import com.github.jonathanxd.kwcommands.parser.ListInput
import com.github.jonathanxd.kwcommands.parser.MapInput
import com.github.jonathanxd.kwcommands.parser.SingleInput

const val ESCAPE = '\\'
const val MAP_OPEN = '{'
const val MAP_CLOSE = '}'

const val LIST_OPEN = '['
const val LIST_CLOSE = ']'

const val OPEN_TAG = '"';
const val OPEN_TAG2 = '\'';

/**
 * Escape character
 */
fun String.escape(escape: Char): String {
    val strBuilder = StringBuilder()
    var lastIsEscape = false

    this.forEach {
        when {
            lastIsEscape -> {
                lastIsEscape = false
                strBuilder.append(it)
            }
            it == escape -> lastIsEscape = true
            else -> strBuilder.append(it)
        }
    }

    return strBuilder.toString()
}

/**
 * Creates a command string from String iterable, example:
 *
 * `[Hello, Beautiful "World"]`
 *
 * Will be converted into:
 *
 * `Hello "Beautiful \"World\""`
 */
fun Iterable<String>.toCommandString(): String =
        this.joinToString(" ") {
            val r = it.replace("$OPEN_TAG", "$ESCAPE$OPEN_TAG")
                    .replace("$OPEN_TAG2", "$ESCAPE$OPEN_TAG2")
            if (r.contains(" ")) "$OPEN_TAG$r$OPEN_TAG"
            else r
        }

/**
 * Creates a list from a command string. Example. for the following input string:
 *
 * `create KWCommands "Command system"`
 *
 * This function will produce following list:
 *
 * `[create, KWCommands, Command System]`
 *
 * To escape characters, use `\`:
 *
 * `create KWCommands "\"Command system\""`
 *
 * Produces following list:
 *
 * `[create, KWCommands, "Command System"]`
 *
 * And
 *
 * `create KWCommands \"Command system\"`
 *
 * Produces:
 *
 * `[create, KWCommands, "Command, system"]`
 *
 * Used in older versions of KWCommands, where `List<String>` was the input command.
 */
@JvmOverloads
fun String.toCommandStringList(escape: Char = '\\',
                               separator: Char = ' ',
                               openCloseChars: List<Char> = listOf('"', '\''),
                               mode: Mode = Mode.TOGGLE): List<String> {

    val strBuilder = StringBuilder()
    val list = mutableListOf<String>()
    var lastIsEscape = false
    val openCount = openCloseChars.map { false }.toBooleanArray()

    fun buildAddAndReset() {
        list += strBuilder.toString()
        strBuilder.setLength(0)
    }

    this.forEach {
        val indexOfOpenClose = openCloseChars.indexOf(it)

        when {
            lastIsEscape -> {
                lastIsEscape = false
                strBuilder.append(it)
            }
            it == escape -> lastIsEscape = true
            openCount.indices.any { it != indexOfOpenClose && openCount[it] } && mode == Mode.TOGGLE -> strBuilder.append(it)
            indexOfOpenClose != -1 -> {
                val value = openCount[indexOfOpenClose]
                openCount[indexOfOpenClose] = !value
            }
            openCount.any { it } && mode == Mode.MULTI_DELIMITER -> strBuilder.append(it)
            it == separator -> buildAddAndReset()
            else -> strBuilder.append(it)
        }
    }

    openCount.filter { it }.indices.forEach {
        // Remaining chars
        strBuilder.append(openCloseChars[it])
    }

    if (strBuilder.isNotEmpty()) {
        buildAddAndReset()
    }

    return list
}

enum class Mode {
    /**
     * Multi delimiter mode allows multiple delimiters to be used at same time, example, for following input `'"Hey man"'`,
     * following output will be produced `Hey man`. This mode make it seem that delimiters inside delimiter was ignored, but it was not,
     * following input still valid: `"'Hey man"'`, we have switched the delimiter order, but string still valid, and the output is
     * the same.
     */
    MULTI_DELIMITER,

    /**
     * Toggle mode allows delimiter characters (`openCloseChars`) that is enclosed by another delimiter character to be added
     * as part of the string, example, for input `'"Hey man"'` the output will be `"Hey man"`. The name `TOGGLE` is because this mode
     * toggles the delimiter feature off when a delimiter is found, and turn it on when the same delimiter is found again (to close
     * delimited text).
     *
     * This is the default mode.
     */
    TOGGLE
}

fun SourcedCharIterator.parseSingleInput(escape: Char = '\\',
                                         separators: List<Char> = listOf(' '),
                                         openCloseChars: List<Char> = listOf('"', '\'')): SingleInput {
    val strBuilder = InputBuilder(this.sourceString)
    var lastIsEscape = false
    val openCount = openCloseChars.map { false }.toBooleanArray()
    var started = false

    fun append(ch: Char) {
        strBuilder.append(ch) { this.sourceIndex }
    }

    this.forEach {
        if (!started && it == ' ')
            return@forEach // Continue

        started = true

        val indexOfOpenClose = openCloseChars.indexOf(it)

        when {
            lastIsEscape -> {
                lastIsEscape = false
                append(it)
            }
            it == escape -> lastIsEscape = true
            openCount.indices.any { it != indexOfOpenClose && openCount[it] } -> append(it)
            indexOfOpenClose != -1 -> {
                strBuilder.insideOC = true
                val bvalue = openCount[indexOfOpenClose]
                openCount[indexOfOpenClose] = !bvalue
            }
            separators.any { separator -> it == separator } -> {
                return strBuilder.build(this.sourceIndex - 1)
            }
            else -> append(it)
        }
    }

    openCount.filter { it }.indices.forEach {
        // Remaining chars
        append(openCloseChars[it])
    }

    return strBuilder.build(this.sourceIndex)
}

/**
 * Parse lists in the following formats:
 *
 * `[a, b, c, d]`
 *
 * You can also use list inside of list:
 *
 * `[a, [b, c], d]`
 *
 * Or map inside list:
 *
 * `[a, {b=c}, d]`
 *
 * The possible values type in returned list is:
 *
 * - Another [List]  (with same possible value types).
 * - [Map], with types according to [parseMapInput] function.
 * - [String].
 *
 * This function will thrown [ListParseException] for malformed maps.
 */
fun SourcedCharIterator.parseListInput(escape: Char = '\\',
                                       mapDefineChar: List<Char> = listOf(':', '='),
                                       separators: List<Char> = listOf(','),
                                       openCloseChars: List<Char> = listOf('"', '\'')): ListInput {
    this.next().also {
        if (it != LIST_OPEN)
            throw this.listParseException("Expected '$LIST_OPEN' but found '$it'")
    }

    return parseListInputUncheckedStart(escape, mapDefineChar, separators, openCloseChars)
}

private fun SourcedCharIterator.parseListInputUncheckedStart(escape: Char = '\\',
                                                             mapDefineChar: List<Char> = listOf(':', '='),
                                                             separators: List<Char> = listOf(','),
                                                             openCloseChars: List<Char> = listOf('"', '\'')): ListInput {
    val list = mutableListOf<Input>()
    val start = this.sourceIndex
    var obj: Input? = null

    fun set(v: Input, token: Token) {
        if (token != Token.SEPARATOR && token != Token.CLOSE)
            throw this.listParseException("Expected list element but found token: $token.")

        list += v
    }

    fun setObj(v: Input?) {
        if (obj != null)
            throw this.listParseException("Expected either list or map, but found both.")

        obj = v
    }

    val strBuilder = InputBuilder(this.sourceString)
    var lastIsEscape = false
    val openCount = openCloseChars.map { false }.toBooleanArray()

    fun append(ch: Char) {
        strBuilder.append(ch) { this.sourceIndex }
    }

    fun buildAddAndReset(token: Token) {
        if (obj != null && strBuilder.isNotEmpty())
            throw this.listParseException("Invalid entry, list object with list entry string." +
                    " Obj: $obj, entry string: $strBuilder")
        if (obj != null) {
            set(obj!!, token)
            obj = null
        } else {
            set(strBuilder.build(this.sourceIndex - 1), token)
        }
    }

    this.forEach {
        val indexOfOpenClose = openCloseChars.indexOf(it)

        when {
            lastIsEscape -> {
                lastIsEscape = false
                append(it)
            }
            it == escape -> lastIsEscape = true
            openCount.indices.any { it != indexOfOpenClose && openCount[it] } -> append(it)
            indexOfOpenClose != -1 -> {
                strBuilder.insideOC = true
                val bvalue = openCount[indexOfOpenClose]
                openCount[indexOfOpenClose] = !bvalue
            }
            separators.any { separator -> it == separator } -> buildAddAndReset(Token.SEPARATOR)
            it == MAP_OPEN ->
                setObj(this.parseMapInputUncheckedStart(escape, mapDefineChar, separators, openCloseChars))
            it == MAP_CLOSE ->
                buildAddAndReset(Token.CLOSE)
            it == LIST_OPEN ->
                setObj(this.parseListInputUncheckedStart(escape, mapDefineChar, separators, openCloseChars))
            it == LIST_CLOSE -> {
                buildAddAndReset(Token.CLOSE)
                return ListInput(list, this.sourceString, start, this.sourceIndex)
            }
            else -> append(it)
        }
    }

    openCount.filter { it }.indices.forEach {
        // Remaining chars
        append(openCloseChars[it])
    }

    if (strBuilder.isNotEmpty()) {
        buildAddAndReset(Token.CLOSE)
    }

    throw this.listParseException("Expected list close but found list entry.")
}

/**
 * Parse maps in the following formats:
 *
 * `{a=b, c=d}`
 * `{"Name of project"="KWCommands", version=1.0}`
 *
 * You can also use map inside of map:
 *
 * `{{a=b}=c}`
 *
 * Or list inside map:
 *
 * `{a=[b, c]}`
 *
 * And use `:` instead of `=`.
 *
 * The possible values type in returned map is:
 *
 * - Another [Map] (with same possible value types).
 * - [List], with types according to [parseListInput] function.
 * - [String].
 *
 * This function will thrown [MapParseException] for malformed maps.
 */
fun SourcedCharIterator.parseMapInput(escape: Char = '\\',
                                      defineChar: List<Char> = listOf(':', '='),
                                      separators: List<Char> = listOf(','),
                                      openCloseChars: List<Char> = listOf('"', '\'')): MapInput {
    this.next().also {
        if (it != '{')
            throw this.mapParseException("Expected '{' but found '$it'")
    }

    return parseMapInputUncheckedStart(escape, defineChar, separators, openCloseChars)
}

private fun SourcedCharIterator.parseMapInputUncheckedStart(escape: Char = '\\',
                                                            defineChar: List<Char> = listOf(':', '='),
                                                            separators: List<Char> = listOf(','),
                                                            openCloseChars: List<Char> = listOf('"', '\'')): MapInput {
    val map = mutableMapOf<Input, Input>()
    val start = this.sourceIndex
    var key: OptObject<Input> = none()
    var value: OptObject<Input> = none()
    var obj: Input? = null

    fun setObj(v: Input?) {
        if (obj != null)
            throw this.mapParseException("Expected either list or map, but found both.")

        obj = v
    }

    fun set(v: Input, token: Token) {
        if (token != Token.DEFINE && !key.isPresent)
            throw this.mapParseException("Expected map define character, but found token: $token")

        if (token != Token.SEPARATOR && token != Token.CLOSE && key.isPresent && !value.isPresent)
            throw this.mapParseException("Expected key ($key) value but found token: $token")

        if (!key.isPresent)
            key = some(v)
        else if (!value.isPresent)
            value = some(v)

        if (key.isPresent && value.isPresent) {
            map.put(key.value, value.value)
            key = none()
            value = none()
        }
    }

    val strBuilder = InputBuilder(this.sourceString)
    var lastIsEscape = false
    val openCount = openCloseChars.map { false }.toBooleanArray()

    fun append(ch: Char) {
        strBuilder.append(ch) { this.sourceIndex }
    }

    fun buildAddAndReset(token: Token) {
        if (obj != null && strBuilder.isNotEmpty())
            throw this.mapParseException("Invalid entry, map object with map entry string." +
                    " Obj: $obj, entry string: $strBuilder")
        if (obj != null) {
            set(obj!!, token)
            obj = null
        } else {
            set(strBuilder.build(this.sourceIndex - 1), token)
        }
    }

    this.forEach {
        val indexOfOpenClose = openCloseChars.indexOf(it)

        when {
            lastIsEscape -> {
                lastIsEscape = false
                append(it)
            }
            it == escape -> lastIsEscape = true
            openCount.indices.any { it != indexOfOpenClose && openCount[it] } -> append(it)
            indexOfOpenClose != -1 -> {
                strBuilder.insideOC = true
                val bvalue = openCount[indexOfOpenClose]
                openCount[indexOfOpenClose] = !bvalue
            }
            separators.any { separator -> it == separator } -> buildAddAndReset(Token.SEPARATOR)
            defineChar.any { def -> it == def } -> buildAddAndReset(Token.DEFINE)
            it == LIST_OPEN ->
                setObj(this.parseListInputUncheckedStart(escape, defineChar, separators, openCloseChars))
            it == LIST_CLOSE ->
                buildAddAndReset(Token.CLOSE)
            it == MAP_OPEN ->
                setObj(this.parseMapInputUncheckedStart(escape, defineChar, separators, openCloseChars))
            it == MAP_CLOSE -> {
                buildAddAndReset(Token.CLOSE)
                return MapInput(map, this.sourceString, start, this.sourceIndex)
            }
            else -> append(it)
        }
    }

    openCount.filter { it }.indices.forEach {
        // Remaining chars
        append(openCloseChars[it])
    }

    if (strBuilder.isNotEmpty()) {
        buildAddAndReset(Token.CLOSE)
    }

    throw this.mapParseException("Expected map close but found map entry.")
}

class InputBuilder(val source: String) {
    var start = -1
    var insideOC = false
    val builder = StringBuilder()
    var blankSpace = 0

    private fun initIndex(index: () -> Int) {
        if (start == -1)
            start = index()
    }

    fun append(ch: Char, index: () -> Int) {
        if (ch == ' ') {
            if (builder.isNotEmpty() || insideOC) {
                blankSpace++
                initIndex(index)
            }
        } else {
            initIndex(index)
            if (blankSpace > 0) {
                for (i in 0 until blankSpace) {
                    builder.append(' ')
                }
                blankSpace = 0
            }
            builder.append(ch)
        }
    }

    fun build(end: Int): SingleInput {
        if (start == -1)
            throw IllegalStateException("Start not initialized")

        val str = builder.toString()
        val input = SingleInput(str,
                this.source,
                start,
                end - blankSpace)
        builder.setLength(0)
        blankSpace = 0
        insideOC = false
        start = -1
        return input
    }

    fun isEmpty() = this.builder.isEmpty()
    fun isNotEmpty() = this.builder.isNotEmpty()
}

enum class Token {
    SEPARATOR,
    SEPARATOR_SIMPLE,
    DEFINE,
    CLOSE
}
