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
import com.github.jonathanxd.kwcommands.argument.ArgumentType
import com.github.jonathanxd.kwcommands.argument.PairArgumentType
import com.github.jonathanxd.kwcommands.parser.*

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

private fun SourcedCharIterator.parseMapOrList(argumentType: ArgumentType<*, *>,
                                               ch: Char,
                                               escape: Char = '\\',
                                               defineChar: List<Char> = listOf(':', '='),
                                               separators: List<Char> = listOf(','),
                                               openCloseChars: List<Char> = listOf('"', '\''))
        : Either<InputParseFail, out Input> =

        if (ch == LIST_OPEN)
            this.parseListInputUncheckedStart(argumentType, escape, defineChar, separators, openCloseChars)
        else
            this.parseMapInputUncheckedStart(argumentType, escape, defineChar, separators, openCloseChars)

fun SourcedCharIterator.callPrevious(): SourcedCharIterator = this.apply { previous() }

@JvmOverloads
fun SourcedCharIterator.parseSingleInput(argumentType: ArgumentType<*, *>,
                                         escape: Char = '\\',
                                         separators: List<Char> = listOf(' '),
                                         openCloseChars: List<Char> = listOf('"', '\''),
                                         mapDefineChar: List<Char> = listOf(':', '='),
                                         listMapSeparators: List<Char> = listOf(','),
                                         parseData: Boolean = false): Either<InputParseFail, out Input> {

    this.jumpBlankSpace()

    if (!this.hasNext())
        return left(NoMoreElementsInputParseFail(ListInput(emptyList(), this.sourceString,
                this.sourceIndex,
                this.sourceIndex),
                argumentType))

    val strBuilder = InputBuilder(this.sourceString)
    var lastIsEscape = false
    val openCount = openCloseChars.map { false }.toBooleanArray()

    fun append(ch: Char) {
        strBuilder.append(ch) { this.sourceIndex }
    }

    fun build(): Either<InputParseFail, out Input> {
        val build = strBuilder.build(this.sourceIndex)

        argumentType.validate(build).also {
            if (it.isInvalid)
                return left(InvalidInputForArgumentTypeFail(it, build, argumentType))
        }

        return right(build)
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

                return build()
            }
            (it == MAP_OPEN || it == LIST_OPEN) && parseData -> {

                if (strBuilder.isNotEmpty())
                    return left(InputMalformationParseFail(strBuilder.build(this.sourceIndex - 1),
                            argumentType))

                val parse = this.parseMapOrList(argumentType, it, escape, mapDefineChar,
                        listMapSeparators, openCloseChars)

                if (parse.isLeft)
                    return left(parse.left)

                argumentType.validate(parse.right).also {
                    if (it.isInvalid)
                        return left(InvalidInputForArgumentTypeFail(it, parse.right, argumentType))
                }


                return right(parse.right)
            }
            else -> append(it)
        }
    }

    return build()
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
fun SourcedCharIterator.parseListInput(argumentType: ArgumentType<*, *>,
                                       escape: Char = '\\',
                                       mapDefineChar: List<Char> = listOf(':', '='),
                                       separators: List<Char> = listOf(','),
                                       openCloseChars: List<Char> = listOf('"', '\''))
        : Either<InputParseFail, ListInput> {
    this.jumpBlankSpace()

    this.next().also {
        if (it != LIST_OPEN)
            return left(TokenExpectedFail(listOf(LIST_OPEN),
                    it.toString(),
                    EmptyInput(this.sourceString),
                    argumentType
            ))
    }

    return parseListInputUncheckedStart(argumentType, escape, mapDefineChar, separators, openCloseChars)
}

private fun SourcedCharIterator.parseListInputUncheckedStart(argumentType: ArgumentType<*, *>,
                                                             escape: Char = '\\',
                                                             mapDefineChar: List<Char> = listOf(':', '='),
                                                             separators: List<Char> = listOf(','),
                                                             openCloseChars: List<Char> = listOf('"', '\''))
        : Either<InputParseFail, ListInput> {

    val list = mutableListOf<Input>()
    val start = this.sourceIndex
    var index = 0

    fun createListInput() =
            ListInput(list, this.sourceString, start, this.sourceIndex)

    if (!this.hasNext())
        return left(TokenOrElementExpectedFail(listOf(LIST_CLOSE),
                "",
                createListInput(),
                argumentType
        ))

    while (this.hasNext()) {
        val elementType = argumentType.getListType(index)

        this.jumpBlankSpace()

        if (!this.hasNext() && list.isEmpty())
            return left(TokenOrElementExpectedFail(listOf(LIST_CLOSE),
                    "",
                    createListInput(),
                    elementType
            ))

        if (this.hasNext()) {
            val next = this.next()

            if (next == LIST_CLOSE)
                return right(createListInput())

            this.previous()
        }

        this.jumpBlankSpace()

        if (!this.hasNext())
            return left(NextElementNotFoundFail(createListInput(), elementType))

        val elem = this.parseSingleInput(elementType,
                escape,
                listOf(' ') + separators + LIST_CLOSE,
                openCloseChars,
                mapDefineChar,
                separators,
                true)

        if (elem.isLeft) {
            list += elem.left.input
            return left(elem.left)
        } else {
            list += elem.right
        }

        this.jumpBlankSpace()

        val define = this.tryNext()

        val isSeparator = define.isPresent && separators.any { it == define.value }

        if (!define.isPresent
                || (!isSeparator && define.value != LIST_CLOSE))
            return left(TokenExpectedFail(
                    separators + LIST_CLOSE,
                    define.toOptString().orElse(EMPTY_STR),
                    createListInput(),
                    argumentType
            ))

        if (isSeparator && !this.hasNext() && argumentType.hasType(index + 1))
            return left(NextElementNotFoundFail(createListInput(), argumentType.getListType(index + 1)))

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
fun SourcedCharIterator.parseMapInput(argumentType: ArgumentType<*, *>,
                                      escape: Char = '\\',
                                      defineChar: List<Char> = listOf(':', '='),
                                      separators: List<Char> = listOf(','),
                                      openCloseChars: List<Char> = listOf('"', '\''))
        : Either<InputParseFail, MapInput> {
    this.jumpBlankSpace()

    this.next().also {
        if (it != MAP_OPEN)
            return left(TokenExpectedFail(listOf(MAP_OPEN),
                    it.toString(),
                    EmptyInput(this.sourceString),
                    argumentType))
    }

    return parseMapInputUncheckedStart(argumentType, escape, defineChar, separators, openCloseChars)
}

private fun SourcedCharIterator.parseMapInputUncheckedStart(argumentType: ArgumentType<*, *>,
                                                            escape: Char = '\\',
                                                            defineChar: List<Char> = listOf(':', '='),
                                                            separators: List<Char> = listOf(','),
                                                            openCloseChars: List<Char> = listOf('"', '\''))
        : Either<InputParseFail, MapInput> {

    val map = mutableListOf<Pair<Input, Input>>()
    val start = this.sourceIndex
    var index = 0

    fun createMapInput(): MapInput =
            MapInput(map, this.sourceString, start, this.sourceIndex)

    this.jumpBlankSpace()

    if (!this.hasNext())
        return left(TokenOrElementExpectedFail(
                listOf(MAP_CLOSE),
                "",
                createMapInput(),
                argumentType
        ))

    while (this.hasNext()) {
        this.jumpBlankSpace()

        if (!this.hasNext() && map.isEmpty()) {
            return left(TokenOrElementExpectedFail(
                    listOf(MAP_CLOSE),
                    "",
                    createMapInput(),
                    argumentType
            ))
        }


        if (this.hasNext()) {
            val next = this.next()

            if (next == MAP_CLOSE)
                return right(createMapInput())

            this.previous()
        }

        val keyType = argumentType.getMapKeyType(index)

        this.jumpBlankSpace()

        if (!this.hasNext())
            return left(NextElementNotFoundFail(createMapInput(),
                    keyType
            ))

        val k = this.parseSingleInput(keyType,
                escape,
                listOf(' ') + defineChar,
                openCloseChars,
                defineChar,
                separators,
                true)

        if (k.isLeft) {
            map += k.left.input to EmptyInput(this.sourceString)
            return left(k.left)
        }

        this.jumpBlankSpace()

        val define = this.tryNext()

        if (!define.isPresent || defineChar.none { it == define.value })
            return left(TokenExpectedFail(
                    defineChar,
                    define.toOptString().orElse(EMPTY_STR),
                    createMapInput(),
                    argumentType
            ))

        val valueType = argumentType.getMapValueType(index)

        this.jumpBlankSpace()

        if (!this.hasNext()) {
            map += k.right to EmptyInput(this.sourceString)
            return left(NextElementNotFoundFail(
                    createMapInput(),
                    valueType
            ))
        }

        val v = this.parseSingleInput(valueType,
                escape,
                listOf(' ') + separators + MAP_CLOSE,
                openCloseChars,
                defineChar,
                separators,
                true)

        if (v.isLeft) {
            map += k.right to v.left.input
            return left(v.left)
        }

        map += k.right to v.right

        this.jumpBlankSpace()

        val defineV = this.tryNext()
        val isSeparator = defineV.isPresent && separators.any { it == defineV.value }

        if (!defineV.isPresent
                || (!isSeparator && defineV.value != MAP_CLOSE))
            return left(TokenExpectedFail(
                    separators + MAP_CLOSE,
                    defineV.toOptString().orElse(EMPTY_STR),
                    createMapInput(),
                    argumentType
            ))

        if (isSeparator && !this.hasNext() && argumentType.hasType(index + 1))
            return left(NextElementNotFoundFail(createMapInput(), argumentType.getMapKeyType(index + 1)))

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

abstract class InputParseFail(val input: Input,
                              val argumentType: ArgumentType<*, *>) {
    override fun toString(): String =
            "${this::class.java.simpleName}[input=$input, argumentType=$argumentType]"
}

class InvalidInputForArgumentTypeFail(val validation: Validation,
                                      input: Input,
                                      argumentType: ArgumentType<*, *>) : InputParseFail(input, argumentType) {
    override fun toString(): String =
            "InvalidInputForArgumentTypeFail[argumentType=$argumentType, input=$input]"
}

class InputMalformationParseFail(input: Input,
                                 argumentType: ArgumentType<*, *>) : InputParseFail(input, argumentType) {
    override fun toString(): String =
            "InputMalformationParseFail[input=$input, argumentType=$argumentType]"
}

class NoMoreElementsInputParseFail(input: Input,
                                   argumentType: ArgumentType<*, *>) : InputParseFail(input, argumentType)

class TokenExpectedFail(val tokens: List<Char>,
                        val currentToken: String,
                        input: Input,
                        argumentType: ArgumentType<*, *>) : InputParseFail(input, argumentType)

class TokenOrElementExpectedFail(val tokens: List<Char>,
                                 val currentToken: String,
                                 input: Input,
                                 argumentType: ArgumentType<*, *>) : InputParseFail(input, argumentType)

/**
 * @param input Current input of values.
 * @param argumentType Type of element that was not found.
 * @param iter Iterator.
 */
class NextElementNotFoundFail(input: Input,
                              argumentType: ArgumentType<*, *>) : InputParseFail(input, argumentType)

class TypeNotFoundFail(input: Input,
                       argumentType: ArgumentType<*, *>) : InputParseFail(input, argumentType)

class ListTypeGetter(val types: List<ArgumentType<*, *>>) : TypeGetter {
    override fun get(index: Int): ArgumentType<*, *>? =
            if (types.size < index) null else types[index]
}

class SingleTypeGetter(val type: ArgumentType<*, *>) : TypeGetter {
    override fun get(index: Int): ArgumentType<*, *>? = type
}

interface TypeGetter {
    fun get(index: Int): ArgumentType<*, *>?
}

class ListMapTypeGetter(val types: List<PairArgumentType<*, *>>) : MapTypeGetter {
    override fun getKey(index: Int): ArgumentType<*, *>? =
            if (types.size < index) null else types[index].aPairType

    override fun getValue(index: Int): ArgumentType<*, *>? =
            if (types.size < index) null else types[index].bPairType

}

class SingleMapTypeGetter(val keyType: ArgumentType<*, *>,
                          val valueType: ArgumentType<*, *>) : MapTypeGetter {
    override fun getKey(index: Int): ArgumentType<*, *>? = keyType
    override fun getValue(index: Int): ArgumentType<*, *>? = valueType
}


interface MapTypeGetter {
    fun getKey(index: Int): ArgumentType<*, *>?
    fun getValue(index: Int): ArgumentType<*, *>?
}


