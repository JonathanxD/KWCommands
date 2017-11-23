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
package com.github.jonathanxd.kwcommands.parser

import com.github.jonathanxd.iutils.text.TextComponent
import com.github.jonathanxd.kwcommands.Texts
import com.github.jonathanxd.kwcommands.argument.Argument

/**
 * Holds the input of argument. When the [start] is equal to [end] and both is `0`, this means that input
 * is not extracted from a [source] (it includes [EmptyInput]).
 */
sealed class Input {
    abstract val source: String
    abstract val start: Int
    abstract val end: Int
    abstract val type: InputType

    /**
     * `True` is this value was extracted from [source], false otherwise.
     */
    val isFromSource get() = (start != 0 && end != 0)

    val content
        get() =
            if (!isFromSource) ""
            else this.source.slice(start..end)

    abstract fun toInputString(): String
}

/**
 * Denotes a single input for argument
 */
data class SingleInput(val input: String,
                       override val source: String,
                       override val start: Int,
                       override val end: Int) : Input() {

    override val type: InputType = SingleInputType

    constructor(input: String) : this(input, "", 0, 0)

    override fun toString(): String =
            "SingleInput(input='$input'" +
                    ", source='$source'" +
                    ", start='$start'" +
                    ", end='$end'" +
                    ", content='$content')"

    override fun toInputString(): String = input
}

/**
 * Denotes a collection of elements input for argument marked as [multiple][Argument.isMultiple].
 */
data class ListInput(val input: List<Input>,
                     override val source: String,
                     override val start: Int,
                     override val end: Int) : Input() {

    override val type: InputType = ListInputType

    constructor(input: List<Input>) : this(input, "", 0, 0)

    override fun toString(): String =
            "ListInput(input=$input" +
                    ", source='$source'" +
                    ", start='$start'" +
                    ", end='$end'" +
                    ", content='$content')"

    override fun toInputString(): String = "[${input.joinToString { it.toInputString() }}]"
}

/**
 * Denotes a map of elements input for argument marked as [multiple][Argument.isMultiple].
 */
data class MapInput(val input: Map<Input, Input>,
                    override val source: String,
                    override val start: Int,
                    override val end: Int) : Input() {

    override val type: InputType = MapInputType

    constructor(input: Map<Input, Input>) : this(input, "", 0, 0)

    override fun toString(): String =
            "MapInput(input=$input" +
                    ", source='$source'" +
                    ", start='$start'" +
                    ", end='$end'" +
                    ", content='$content')"

    override fun toInputString(): String = input.entries.joinToString {
        "{${it.key.toInputString()}=${it.value.toInputString()}}"
    }
}

/**
 * Denotes no value input for argument marked as [multiple][Argument.isMultiple].
 */
class EmptyInput(override val source: String) : Input() {
    override val start: Int = 0
    override val end: Int = 0
    override val type: InputType = EmptyInputType

    override fun toString(): String =
            "EmptyInput(source='$source', content='')"

    override fun toInputString(): String = "EMPTY[]"
}

interface InputType {
    fun getTypeString(): TextComponent
}

object SingleInputType : InputType {
    override fun getTypeString(): TextComponent = Texts.getSingleTypeText()
}

object ListInputType : InputType {
    override fun getTypeString(): TextComponent = Texts.getListTypeText()
}

object MapInputType : InputType {
    override fun getTypeString(): TextComponent = Texts.getMapTypeText()
}

object EmptyInputType : InputType {
    override fun getTypeString(): TextComponent = Texts.getEmptyTypeText()
}