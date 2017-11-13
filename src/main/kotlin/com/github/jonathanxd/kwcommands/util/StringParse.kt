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
import com.github.jonathanxd.kwcommands.exception.MapParseException

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

/**
 * Parse maps in the following formats:
 *
 * `[a=b, c=d]`
 * `["Name of project"="KWCommands", version=1.0]`
 *
 * You can also use map inside of map:
 *
 * `[[a=b]=c]`
 *
 * And use `:` instead of `=`.
 *
 * The possible values type in returned map is:
 *
 * - Another [Map] (with same possible value types)
 * - [String]
 *
 * This function will thrown [MapParseException] for malformed maps.
 */
fun CharIterator.parseMap(escape: Char = '\\',
                          defineChar: List<Char> = listOf(':', '='),
                          separators: List<Char> = listOf(',', ' '),
                          openCloseChars: List<Char> = listOf('"', '\'')): Map<Any, Any> {
    this.next().also {
        if (it != '[')
            throw MapParseException("Expected '[' but found '$it'")
    }

    return parseMapUncheckedStart(escape, defineChar, separators, openCloseChars)
}

private fun CharIterator.parseMapUncheckedStart(escape: Char = '\\',
                          defineChar: List<Char> = listOf(':', '='),
                          separators: List<Char> = listOf(',', ' '),
                          openCloseChars: List<Char> = listOf('"', '\'')): Map<Any, Any> {
    val map = mutableMapOf<Any, Any>()
    var key: OptObject<Any> = none()
    var value: OptObject<Any> = none()
    var obj: Any? = null

    fun set(v: Any, token: Token) {
        if (token != Token.DEFINE && !key.isPresent)
            throw MapParseException("Expected map define character, but found token: $token.")

        if (token != Token.SEPARATOR && token != Token.CLOSE && key.isPresent && !value.isPresent)
            throw MapParseException("Expected key ($key) value but found token: $token.")

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

    val strBuilder = StringBuilder()
    var lastIsEscape = false
    val openCount = openCloseChars.map { false }.toBooleanArray()

    fun buildAddAndReset(token: Token) {
        if (obj != null && strBuilder.isNotEmpty())
            throw MapParseException("Invalid entry, map object with map entry string." +
                    " Obj: $obj, entry string: $strBuilder")
        if (obj != null) {
            set(obj!!, token)
            obj = null
        } else {
            set(strBuilder.toString(), token)
        }
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
            openCount.indices.any { it != indexOfOpenClose && openCount[it] } -> strBuilder.append(it)
            indexOfOpenClose != -1 -> {
                val bvalue = openCount[indexOfOpenClose]
                openCount[indexOfOpenClose] = !bvalue
            }
            separators.any { separator -> it == separator } -> buildAddAndReset(Token.SEPARATOR)
            defineChar.any { def -> it == def } -> buildAddAndReset(Token.DEFINE)
            it == '[' ->
                obj = this.parseMapUncheckedStart(escape, defineChar, separators, openCloseChars)
            it == ']' -> {
                buildAddAndReset(Token.CLOSE)
                return map
            }
            else -> strBuilder.append(it)
        }
    }

    openCount.filter { it }.indices.forEach {
        // Remaining chars
        strBuilder.append(openCloseChars[it])
    }

    if (strBuilder.isNotEmpty()) {
        buildAddAndReset(Token.CLOSE)
    }

    throw MapParseException("Expected map close but found map entry.")
}

enum class Token {
    SEPARATOR,
    DEFINE,
    CLOSE
}
