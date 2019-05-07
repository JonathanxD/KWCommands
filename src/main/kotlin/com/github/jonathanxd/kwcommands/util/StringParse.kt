/*
 *      KWCommands - New generation of WCommands written in Kotlin <https://github.com/JonathanxD/KWCommands>
 *
 *         The MIT License (MIT)
 *
 *      Copyright (c) 2018 JonathanxD
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
import com.github.jonathanxd.iutils.`object`.result.Result
import com.github.jonathanxd.iutils.kt.*
import com.github.jonathanxd.iutils.opt.specialized.OptChar
import com.github.jonathanxd.iutils.opt.OptObject
import com.github.jonathanxd.kwcommands.fail.ParseFail
import com.github.jonathanxd.kwcommands.parser.*

const val ESCAPE = '\\'
const val MAP_OPEN = '{'
const val MAP_CLOSE = '}'

const val LIST_OPEN = '['
const val LIST_CLOSE = ']'

const val OPEN_TAG = '"'
const val OPEN_TAG2 = '\''

const val ASSIGN = '='

private val EMPTY_CHAR_ARRAY = charArrayOf()

fun String.escaped() = this.escape('\\')

fun String.isName() = this.isArgumentName() || this.isShortName()

fun String.isArgumentName() = this.startsWith("--")
fun String.getArgumentName() = this.substring(2) // "--".length
fun String.getArgumentNameOrNull() =
    if (!this.isArgumentName()) null
    else this.substring(2).takeUntilEquals() // "--".length

fun SingleInput.extractAssignmentValue(): SingleInput? {
    val sb = StringBuilder()

    var lastIsEscape = true
    var nameDef = false
    var offset = 0

    this.input.forEachIndexed { index, it ->
        if (!lastIsEscape && it == ESCAPE) {
            lastIsEscape = true
        } else if (!nameDef && !lastIsEscape && it == ASSIGN) {
            if (index + 1 == this.input.length) return@extractAssignmentValue null
            lastIsEscape = false
            nameDef = true
            sb.setLength(0)
            offset = index + 1
        } else {
            if (nameDef) sb.append(it)
            lastIsEscape = false
        }
    }

    return SingleInput(sb.toString(), this.source, this.start + offset, this.end)
}

fun String.takeUntilEquals(): String {
    var lastEscape = false

    return this.takeWhile {
        if (it == ESCAPE && !lastEscape) {
            lastEscape = true
            true
        } else {
            if (it == ASSIGN && !lastEscape) {
                false
            } else if (lastEscape) {
                lastEscape = false
                true
            } else it != ASSIGN
        }
    }
}


fun String.isAssignmentArg(): Boolean {
    var lastIsEscape = true

    this.forEach {
        lastIsEscape = if (it == ESCAPE && !lastIsEscape) {
            true
        } else if (it == ASSIGN && !lastIsEscape){
            return true
        } else {
            false
        }
    }

    return false
}

fun String.isShortName() = this.startsWith("-") && !this.startsWith("--")
fun String.getShortNameOrNull() =
    if (!this.isShortName()) null
    else this.substring(1)

fun String.getShortNames() =
    this.getShortNameOrNull()?.toCharArray() ?: EMPTY_CHAR_ARRAY

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
fun String.toCommandStringList(
    escape: Char = '\\',
    separator: Char = ' ',
    openCloseChars: List<Char> = listOf('"', '\''),
    mode: Mode = Mode.TOGGLE
): List<String> {

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
            openCount.indices.any { it != indexOfOpenClose && openCount[it] } && mode == Mode.TOGGLE ->
                strBuilder.append(it)
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

private fun SourcedCharIterator.parseMapOrList(
    ch: Char,
    escape: Char = '\\',
    defineChar: List<Char> = listOf(':', '='),
    separators: List<Char> = listOf(','),
    openCloseChars: List<Char> = listOf('"', '\'')
)
        : Either<InputParseFail, out Input> =

    if (ch == LIST_OPEN)
        this.parseListInputUncheckedStart(escape, defineChar, separators, openCloseChars)
    else
        this.parseMapInputUncheckedStart(escape, defineChar, separators, openCloseChars)

private fun SourcedCharIterator.parseMapListOrSingle(
    ch: Char,
    escape: Char = '\\',
    defineChar: List<Char> = listOf(':', '='),
    separators: List<Char> = listOf(','),
    openCloseChars: List<Char> = listOf('"', '\'')
)
        : Either<InputParseFail, out Input> =

    when (ch) {
        LIST_OPEN -> this.parseListInputUncheckedStart(
            escape,
            defineChar,
            separators,
            openCloseChars
        )
        MAP_OPEN -> this.parseMapInputUncheckedStart(escape, defineChar, separators, openCloseChars)
        else -> this.callPrevious().parseSingleInput(escape, defineChar, separators, openCloseChars)
    }


fun SourcedCharIterator.callPrevious(): SourcedCharIterator = this.apply { previous() }

@JvmOverloads
fun SourcedCharIterator.parseSingleInput(
    escape: Char = '\\',
    separators: List<Char> = listOf(' '),
    openCloseChars: List<Char> = listOf('"', '\''),
    mapDefineChar: List<Char> = listOf(':', '='),
    listMapSeparators: List<Char> = listOf(','),
    parseData: Boolean = false
): Either<InputParseFail, out Input> {

    this.jumpBlankSpace()

    if (!this.hasNext())
        return left(
            NoMoreElementsInputParseFail(
                ListInput(
                    emptyList(), this.sourceString,
                    this.sourceIndex,
                    this.sourceIndex
                )
            )
        )

    val strBuilder = InputBuilder(this.sourceString)
    var lastIsEscape = false
    val openCount = openCloseChars.map { false }.toBooleanArray()

    fun append(ch: Char) {
        strBuilder.append(ch) { this.sourceIndex }
    }

    fun build(): Input {
        return strBuilder.build(this.sourceIndex)
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

                return right(build())
            }
            (it == MAP_OPEN || it == LIST_OPEN) && parseData -> {

                if (strBuilder.isNotEmpty())
                    return left(InputMalformationParseFail(strBuilder.build(this.sourceIndex - 1)))

                val parse = this.parseMapOrList(
                    it, escape, mapDefineChar,
                    listMapSeparators, openCloseChars
                )

                if (parse.isLeft)
                    return left(parse.left)

                /*argumentType.validate(parse.right).also {
                    if (it.isInvalid)
                        return left(InvalidInputForArgumentTypeFail(it, parse.right, argumentType))
                }*/


                return right(parse.right)
            }
            else -> append(it)
        }
    }

    return right(build())
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
 */
@JvmOverloads
fun SourcedCharIterator.parseListInput(
    escape: Char = '\\',
    mapDefineChar: List<Char> = listOf(':', '='),
    separators: List<Char> = listOf(','),
    openCloseChars: List<Char> = listOf('"', '\'')
)
        : Either<InputParseFail, ListInput> {
    this.jumpBlankSpace()

    this.next().also {
        if (it != LIST_OPEN)
            return left(
                TokenExpectedFail(
                    listOf(LIST_OPEN),
                    it.toString(),
                    EmptyInput(this.sourceString)
                )
            )
    }

    return parseListInputUncheckedStart(escape, mapDefineChar, separators, openCloseChars)
}

private fun SourcedCharIterator.parseListInputUncheckedStart(
    escape: Char = '\\',
    mapDefineChar: List<Char> = listOf(':', '='),
    separators: List<Char> = listOf(','),
    openCloseChars: List<Char> = listOf('"', '\'')
)
        : Either<InputParseFail, ListInput> {

    val list = mutableListOf<Input>()
    val start = this.sourceIndex
    var index = 0

    fun createListInput() =
        ListInput(list, this.sourceString, start, this.sourceIndex)

    if (!this.hasNext())
        return left(
            TokenOrElementExpectedFail(
                listOf(LIST_CLOSE),
                "",
                createListInput()
            )
        )

    while (this.hasNext()) {

        this.jumpBlankSpace()

        if (!this.hasNext() && list.isEmpty())
            return left(
                TokenOrElementExpectedFail(
                    listOf(LIST_CLOSE),
                    "",
                    createListInput()
                )
            )

        if (this.hasNext()) {
            val next = this.next()

            if (next == LIST_CLOSE)
                return right(createListInput())

            this.previous()
        }

        this.jumpBlankSpace()

        if (!this.hasNext())
            return left(NextElementNotFoundFail(createListInput()))

        val elem = this.parseSingleInput(
            escape,
            listOf(' ') + separators + LIST_CLOSE,
            openCloseChars,
            mapDefineChar,
            separators,
            true
        )

        if (elem.isLeft) {
            list += (elem.left.root ?: elem.left.input)
            return left(elem.left.also { it.root = createListInput() })
        } else {
            list += elem.right
        }

        this.jumpBlankSpace()

        val define = this.tryNext()

        val isSeparator = define.isPresent && separators.any { it == define.value }

        if (!define.isPresent
                || (!isSeparator && define.value != LIST_CLOSE)
        )
            return left(
                TokenExpectedFail(
                    separators + LIST_CLOSE,
                    define.toOptString().orElse(EMPTY_STR),
                    createListInput()
                )
            )

        if (isSeparator && !this.hasNext()/* && argumentType.hasType(index + 1)*/) {
            list += EmptyInput(this.sourceString)
            return left(NextElementNotFoundFail(createListInput()).also {
                it.root = createListInput()
            })
        }

        if (define.isPresent && define.value == LIST_CLOSE)
            break
        ++index
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
 */
@JvmOverloads
fun SourcedCharIterator.parseMapInput(
    escape: Char = '\\',
    defineChar: List<Char> = listOf(':', '='),
    separators: List<Char> = listOf(','),
    openCloseChars: List<Char> = listOf('"', '\'')
)
        : Either<InputParseFail, MapInput> {
    this.jumpBlankSpace()

    this.next().also {
        if (it != MAP_OPEN)
            return left(
                TokenExpectedFail(
                    listOf(MAP_OPEN),
                    it.toString(),
                    EmptyInput(this.sourceString)
                )
            )
    }

    return parseMapInputUncheckedStart(escape, defineChar, separators, openCloseChars)
}

private fun SourcedCharIterator.parseMapInputUncheckedStart(
    escape: Char = '\\',
    defineChar: List<Char> = listOf(':', '='),
    separators: List<Char> = listOf(','),
    openCloseChars: List<Char> = listOf('"', '\'')
)
        : Either<InputParseFail, MapInput> {

    val map = mutableListOf<Pair<Input, Input>>()
    val start = this.sourceIndex
    var index = 0

    fun createMapInput(): MapInput =
        MapInput(map, this.sourceString, start, this.sourceIndex)

    this.jumpBlankSpace()

    if (!this.hasNext())
        return left(
            TokenOrElementExpectedFail(
                listOf(MAP_CLOSE),
                "",
                createMapInput()
            )
        )

    while (this.hasNext()) {
        this.jumpBlankSpace()

        if (!this.hasNext() && map.isEmpty()) {
            return left(
                TokenOrElementExpectedFail(
                    listOf(MAP_CLOSE),
                    "",
                    createMapInput()
                )
            )
        }


        if (this.hasNext()) {
            val next = this.next()

            if (next == MAP_CLOSE)
                return right(createMapInput())

            this.previous()
        }

        this.jumpBlankSpace()

        if (!this.hasNext()) {
            map += EmptyInput(this.sourceString) to EmptyInput(this.sourceString)
            return left(NextElementNotFoundFail(createMapInput()))
        }

        val k = this.parseSingleInput(
            escape,
            listOf(' ') + defineChar,
            openCloseChars,
            defineChar,
            separators,
            true
        )

        if (k.isLeft) {
            map += (k.left.root ?: k.left.input) to EmptyInput(this.sourceString)
            return left(k.left.also { it.root = createMapInput() })
        }

        this.jumpBlankSpace()

        val define = this.tryNext()

        if (!define.isPresent || defineChar.none { it == define.value })
            return left(
                TokenExpectedFail(
                    defineChar,
                    define.toOptString().orElse(EMPTY_STR),
                    createMapInput()
                )
            )

        //val valueType = argumentType.getMapValueType(index)

        this.jumpBlankSpace()

        if (!this.hasNext()) {
            map += k.right to EmptyInput(this.sourceString)
            return left(NextElementNotFoundFail(createMapInput()))
        }

        val v = this.parseSingleInput(
            escape,
            listOf(' ') + separators + MAP_CLOSE,
            openCloseChars,
            defineChar,
            separators,
            true
        )

        if (v.isLeft) {
            map += k.right to (v.left.root ?: v.left.input)
            return left(v.left.also { it.root = createMapInput() })
        }

        map += k.right to v.right

        this.jumpBlankSpace()

        val defineV = this.tryNext()
        val isSeparator = defineV.isPresent && separators.any { it == defineV.value }

        if (!defineV.isPresent
                || (!isSeparator && defineV.value != MAP_CLOSE)
        )
            return left(
                TokenExpectedFail(
                    separators + MAP_CLOSE,
                    defineV.toOptString().orElse(EMPTY_STR),
                    createMapInput()
                )
            )

        if (isSeparator && !this.hasNext()/* && argumentType.hasType(index + 1)*/) {
            map += EmptyInput(this.sourceString) to EmptyInput(this.sourceString)
            return left(NextElementNotFoundFail(createMapInput()).also {
                it.root = createMapInput()
            })
        }

        if (defineV.isPresent && defineV.value == MAP_CLOSE)
            break

        ++index
    }

    return right(MapInput(map, this.sourceString, start, this.sourceIndex))
}

private fun OptChar.toOptString(): OptObject<String> =
    this.flatMapTo({ some(it.toString()) }, { none() })

private fun SourcedCharIterator.tryNext(): OptChar =
    if (this.hasNext()) some(this.next())
    else noneChar()

fun SourcedCharIterator.jumpBlankSpace() {
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
        val input = SingleInput(
            str,
            this.source,
            start,
            end - blankSpace
        )
        builder.setLength(0)
        blankSpace = 0
        insideOC = false
        start = -1
        return input
    }

    fun isEmpty() = this.builder.isEmpty()
    fun isNotEmpty() = this.builder.isNotEmpty()
}

abstract class InputParseFail(val input: Input) {

    var root: Input? = null

    override fun toString(): String =
        "${this::class.java.simpleName}[input=$input]"
}

class InvalidInputForArgumentTypeFail(
    val validation: Validation,
    input: Input
) : InputParseFail(input) {
    override fun toString(): String =
        "InvalidInputForArgumentTypeFail[validation=$validation, input=$input]"
}

class InputMalformationParseFail(input: Input) : InputParseFail(input)
class NoMoreElementsInputParseFail(input: Input) : InputParseFail(input)

class TokenExpectedFail(
    val tokens: List<Char>,
    val currentToken: String,
    input: Input
) : InputParseFail(input)

class TokenOrElementExpectedFail(
    val tokens: List<Char>,
    val currentToken: String,
    input: Input
) : InputParseFail(input) {
    override fun toString(): String =
        "InvalidInputForArgumentTypeFail[tokens=$tokens, currentToken=$currentToken, input=$input]"
}

/**
 * @param input Current input of values.
 * @param argumentType Type of element that was not found.
 * @param iter Iterator.
 */
class NextElementNotFoundFail(input: Input) : InputParseFail(input)

inline fun String.lookbackForEach(f: (prev: () -> Char?, current: Char) -> Unit) =
    this.forEachIndexed { index, c ->
        f({ this.getOrNull(index - 1) }, c)
    }