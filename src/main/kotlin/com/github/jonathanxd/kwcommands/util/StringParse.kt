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

import com.github.jonathanxd.iutils.`object`.Either
import com.github.jonathanxd.iutils.opt.specialized.OptChar
import com.github.jonathanxd.iutils.opt.specialized.OptObject
import com.github.jonathanxd.jwiutils.kt.*
import com.github.jonathanxd.kwcommands.exception.ListParseException
import com.github.jonathanxd.kwcommands.exception.MapParseException
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

fun String.escaped() = this.escape('\\')
fun String.isArgumentName() = this.startsWith("--")
fun String.getArgumentName() = this.substring(2) // "--".length
fun String.getArgumentNameOrNull() =
        if (!this.isArgumentName()) null
        else this.substring(2) // "--".length

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

const val EMPTY_STR = ""

private fun SourcedCharIterator.parseMapOrList(ch: Char,
                                               escape: Char = '\\',
                                               defineChar: List<Char> = listOf(':', '='),
                                               separators: List<Char> = listOf(','),
                                               openCloseChars: List<Char> = listOf('"', '\''))
        : Either<InputParseFail, out Input> =

        if (ch == LIST_OPEN)
            this.parseListInputUncheckedStart(escape, defineChar, separators, openCloseChars)
        else
            this.parseMapInputUncheckedStart(escape, defineChar, separators, openCloseChars)

fun SourcedCharIterator.callPrevious(): SourcedCharIterator = this.apply { previous() }

fun SourcedCharIterator.parseSingleInput(escape: Char = '\\',
                                         separators: List<Char> = listOf(' '),
                                         openCloseChars: List<Char> = listOf('"', '\''),
                                         mapDefineChar: List<Char> = listOf(':', '='),
                                         listMapSeparators: List<Char> = listOf(','),
                                         parseData: Boolean = false):
        Either<InputParseFail, out Input> {

    this.jumpBlankSpace()

    if (!this.hasNext())
        return left(NoMoreElementsInputParseFail(this))

    val strBuilder = InputBuilder(this.sourceString)
    var lastIsEscape = false
    val openCount = openCloseChars.map { false }.toBooleanArray()

    fun append(ch: Char) {
        strBuilder.append(ch) { this.sourceIndex }
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
            separators.any { separator -> it == separator } -> {
                this.previous()
                return right(strBuilder.build(this.sourceIndex))
            }
            (it == MAP_OPEN || it == LIST_OPEN) && parseData -> {

                if (strBuilder.isNotEmpty())
                    return left(InputMalformationParseFail(strBuilder.build(this.sourceIndex - 1), this))

                val parse =
                        this.parseMapOrList(it, escape, mapDefineChar, listMapSeparators, openCloseChars)


                if (parse.isLeft)
                    return left(parse.left)

                return right(parse.right)
            }
            else -> append(it)
        }
    }

    return right(strBuilder.build(this.sourceIndex))
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
                                       openCloseChars: List<Char> = listOf('"', '\''))
        : Either<InputParseFail, ListInput> {
    this.jumpBlankSpace()

    this.next().also {
        if (it != LIST_OPEN)
            return left(ListTokenExpectedFail(listOf(LIST_OPEN),
                    it.toString(),
                    ListInput(emptyList(), this.sourceString, this.sourceIndex, this.sourceIndex),
                    this.callPrevious()))
    }

    return parseListInputUncheckedStart(escape, mapDefineChar, separators, openCloseChars)
}

private fun SourcedCharIterator.parseListInputUncheckedStart(escape: Char = '\\',
                                                             mapDefineChar: List<Char> = listOf(':', '='),
                                                             separators: List<Char> = listOf(','),
                                                             openCloseChars: List<Char> = listOf('"', '\''))
        : Either<InputParseFail, ListInput> {

    val list = mutableListOf<Input>()
    val start = this.sourceIndex

    while (this.hasNext()) {
        this.jumpBlankSpace()

        val next = this.next()

        if (next == LIST_CLOSE)
            break

        this.previous()

        val elem =
                this.parseSingleInput(escape, listOf(' ') + separators + LIST_CLOSE, openCloseChars,
                        mapDefineChar,
                        separators,
                        true)

        this.jumpBlankSpace()

        val define = this.tryNext()

        if (!define.isPresent
                || (separators.none { it == define.value } && define.value != LIST_CLOSE))
            return left(ListTokenExpectedFail(separators + LIST_CLOSE, define.toOptString().orElse(EMPTY_STR),
                    ListInput(list, this.sourceString, start, this.sourceIndex + if (define.isPresent) 1 else 0),
                    this))

        if (elem.isLeft)
            return left(NestedInputParseFail(
                    ListElementNotFound(ListInput(list, this.sourceString, start, this.sourceIndex), this),
                    elem.left
            ))

        list += elem.right

        if (define.isPresent && define.value == LIST_CLOSE)
            break
    }

    return right(ListInput(list, this.sourceString, start, this.sourceIndex))
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
                                      openCloseChars: List<Char> = listOf('"', '\''))
        : Either<InputParseFail, MapInput> {
    this.jumpBlankSpace()

    this.next().also {
        if (it != MAP_OPEN)
            return left(MapTokenExpectedFail(listOf(MAP_OPEN), it.toString(),
                    MapInput(emptyMap(), this.sourceString, this.sourceIndex - 1, this.sourceIndex - 1),
                    this))
    }

    return parseMapInputUncheckedStart(escape, defineChar, separators, openCloseChars)
}

private fun SourcedCharIterator.parseMapInputUncheckedStart(escape: Char = '\\',
                                                            defineChar: List<Char> = listOf(':', '='),
                                                            separators: List<Char> = listOf(','),
                                                            openCloseChars: List<Char> = listOf('"', '\''))
        : Either<InputParseFail, MapInput> {

    val map = mutableMapOf<Input, Input>()
    val start = this.sourceIndex

    while (this.hasNext()) {
        this.jumpBlankSpace()

        val next = this.next()

        if (next == MAP_CLOSE)
            return right(MapInput(map, this.sourceString, start, this.sourceIndex))

        this.previous()

        val k =
                this.parseSingleInput(escape, listOf(' ') + defineChar, openCloseChars)

        if (k.isLeft)
            return left(NestedInputParseFail(
                    MapKeyNotFound(MapInput(map, this.sourceString, start, this.sourceIndex), this),
                    k.left
            ))

        this.jumpBlankSpace()

        val define = this.tryNext()

        if (!define.isPresent || defineChar.none { it == define.value })
            return left(MapTokenExpectedFail(defineChar, define.toOptString().orElse(EMPTY_STR),
                    MapInput(map, this.sourceString, start,
                            this.sourceIndex + if (define.isPresent) 1 else 0), this))

        val v =
                this.parseSingleInput(escape, listOf(' ') + separators + MAP_CLOSE, openCloseChars,
                        defineChar,
                        separators,
                        true)

        if (v.isLeft)
            return left(NestedInputParseFail(
                    MapValueNotFound(k.right, MapInput(map, this.sourceString, start, this.sourceIndex), this),
                    v.left
            ))

        map.put(k.right, v.right)

        this.jumpBlankSpace()

        val defineV = this.tryNext()

        if (!defineV.isPresent
                || (separators.none { it == defineV.value } && defineV.value != MAP_CLOSE))
            return left(MapTokenExpectedFail(separators + MAP_CLOSE,
                    defineV.toOptString().orElse(EMPTY_STR),
                    MapInput(map, this.sourceString, start,
                            this.sourceIndex + if (defineV.isPresent) 1 else 0), this))

        if (defineV.isPresent && defineV.value == MAP_CLOSE)
            break
    }

    return right(MapInput(map, this.sourceString, start, this.sourceIndex))
}

private fun OptChar.toOptString(): OptObject<String> =
        this.flatMapTo({ some(it.toString()) }, { none() })

private fun SourcedCharIterator.tryNext(): OptChar =
        if (this.hasNext()) some(this.next())
        else noneChar()

private fun SourcedCharIterator.jumpBlankSpace() {
    while (this.hasNext()) {
        val c = this.next()

        if (c != ' ') {
            this.previous()
            return
        }
    }
}

class InputBuilder(val source: String) {
    var start = -1
    var insideOC = false
    val builder = StringBuilder()
    var blankSpace = 0

    fun initIndex(index: () -> Int) {
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

abstract class InputParseFail(val iter: SourcedCharIterator) {
    override fun toString(): String =
            "${this::class.java.simpleName}[iter=$iter]"
}

class InputMalformationParseFail(val input: Input, iter: SourcedCharIterator) : InputParseFail(iter) {
    override fun toString(): String =
            "InputMalformationParseFail[input=$input, iter=$iter]"
}


abstract class ListInputParseFail(val list: ListInput, iter: SourcedCharIterator) : InputParseFail(iter) {
    override fun toString(): String =
            "${this::class.java.simpleName}[list=$list, iter=$iter]"
}

abstract class MapInputParseFail(val map: MapInput, iter: SourcedCharIterator) : InputParseFail(iter) {
    override fun toString(): String =
            "${this::class.java.simpleName}[map=$map, iter=$iter]"
}

class NoMoreElementsInputParseFail(iter: SourcedCharIterator) : InputParseFail(iter)

class ListElementNotFound(list: ListInput, iter: SourcedCharIterator) : ListInputParseFail(list, iter)
class MapKeyNotFound(map: MapInput, iter: SourcedCharIterator) : MapInputParseFail(map, iter)
class MapValueNotFound(val key: Input,
                       map: MapInput,
                       iter: SourcedCharIterator) : MapInputParseFail(map, iter)

class NestedInputParseFail(val fail: InputParseFail,
                           val fail2: InputParseFail) : InputParseFail(fail.iter)

class MapTokenExpectedFail(val tokens: List<Char>,
                           val foundToken: String,
                           map: MapInput,
                           iter: SourcedCharIterator) : MapInputParseFail(map, iter) {
    override fun toString(): String =
            "MapTokenExpectedFail[tokens=$tokens, foundToken=$foundToken, map=$map, iter=$iter]"
}

class ListTokenExpectedFail(val tokens: List<Char>,
                            val foundToken: String,
                            list: ListInput,
                            iter: SourcedCharIterator) : ListInputParseFail(list, iter) {
    override fun toString(): String =
            "ListTokenExpectedFail[tokens=$tokens, foundToken=$foundToken, list=$list, iter=$iter]"
}
