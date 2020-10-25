/*
 *      KWCommands - New generation of WCommands written in Kotlin <https://github.com/JonathanxD/KWCommands>
 *
 *         The MIT License (MIT)
 *
 *      Copyright (c) 2020 JonathanxD
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

import com.github.jonathanxd.kwcommands.parser.*

object EmptyPossibilitesFunc : Possibilities {
    override fun invoke(): List<Input> =
        emptyList()
}

object MapFormatCheckParser : ArgumentParser<Input, Any?> {
    override fun parse(
        input: Input,
        valueOrValidationFactory: ValueOrValidationFactory
    ): ValueOrValidation<Any?> =
        throw IllegalStateException("dummy")
}

object ListFormatCheckParser : ArgumentParser<Input, Any?> {
    override fun parse(
        input: Input,
        valueOrValidationFactory: ValueOrValidationFactory
    ): ValueOrValidation<Any?> =
        throw IllegalStateException("dummy")
}

@Suppress("UNCHECKED_CAST")
class EnumParser<T>(val type: Class<T>) : ArgumentParser<SingleInput, T> {
    private val consts = type.enumConstants as Array<Enum<*>>

    override fun parse(
        input: SingleInput,
        valueOrValidationFactory: ValueOrValidationFactory
    ): ValueOrValidation<T> =
        (consts.firstOrNull { it.name == input.input }
                ?: consts.firstOrNull { it.name.equals(input.input, ignoreCase = true) }).let {
            if (it == null)
                valueOrValidationFactory.invalid()
            else
                valueOrValidationFactory.value(it as T)

        }

}

@Suppress("UNCHECKED_CAST")
class EnumPossibilities(val type: Class<*>) : Possibilities {
    private val consts = type.enumConstants as Array<Enum<*>>
    @Suppress("UNCHECKED_CAST")
    override fun invoke(): List<Input> =
        consts.map { SingleInput(it.name) }

}

@Suppress("UNCHECKED_CAST")
fun enumPossibilities(type: Class<*>): List<Input> =
    (type.enumConstants as Array<Enum<*>>).map { SingleInput(it.name) }


// Short
val SHORT_DEFAULT_VALUE: Short = 0

object ShortParser : ArgumentParser<SingleInput, Short> {
    override fun parse(
        input: SingleInput,
        valueOrValidationFactory: ValueOrValidationFactory
    ): ValueOrValidation<Short> =
        input.input.toShortOrNull()?.let { valueOrValidationFactory.value(it) }
                ?: valueOrValidationFactory.invalid()

}

object ShortPossibilities : Possibilities by EmptyPossibilitesFunc

// Char
val CHAR_DEFAULT_VALUE: Char = 0.toChar()

object CharParser : ArgumentParser<SingleInput, Char> {
    override fun parse(
        input: SingleInput,
        valueOrValidationFactory: ValueOrValidationFactory
    ): ValueOrValidation<Char> =
        when (input.input.length) {
            1 -> valueOrValidationFactory.value(input.input[0])
            else -> valueOrValidationFactory.invalid()
        }
}


object CharPossibilities : Possibilities by EmptyPossibilitesFunc

// Byte
val BYTE_DEFAULT_VALUE: Byte = 0.toByte()

object ByteParser : ArgumentParser<SingleInput, Byte> {
    override fun parse(
        input: SingleInput,
        valueOrValidationFactory: ValueOrValidationFactory
    ): ValueOrValidation<Byte> =
        input.input.toByteOrNull()?.let { valueOrValidationFactory.value(it) }
                ?: valueOrValidationFactory.invalid()

}

object BytePossibilities : Possibilities by EmptyPossibilitesFunc

// Int
val INT_DEFAULT_VALUE: Int = 0

object IntParser : ArgumentParser<SingleInput, Int> {
    override fun parse(
        input: SingleInput,
        valueOrValidationFactory: ValueOrValidationFactory
    ): ValueOrValidation<Int> =
        input.input.toIntOrNull()?.let { valueOrValidationFactory.value(it) }
                ?: valueOrValidationFactory.invalid()

}

object IntPossibilities : Possibilities by EmptyPossibilitesFunc

// Float
val FLOAT_DEFAULT_VALUE: Int = 0

object FloatParser : ArgumentParser<SingleInput, Float> {
    override fun parse(
        input: SingleInput,
        valueOrValidationFactory: ValueOrValidationFactory
    ): ValueOrValidation<Float> =
        input.input.toFloatOrNull()?.let { valueOrValidationFactory.value(it) }
                ?: valueOrValidationFactory.invalid()
}

object FloatPossibilities : Possibilities by EmptyPossibilitesFunc

// Double
val DOUBLE_DEFAULT_VALUE: Double = 0.0

object DoubleParser : ArgumentParser<SingleInput, Double> {
    override fun parse(
        input: SingleInput,
        valueOrValidationFactory: ValueOrValidationFactory
    ): ValueOrValidation<Double> =
        input.input.toDoubleOrNull()?.let { valueOrValidationFactory.value(it) }
                ?: valueOrValidationFactory.invalid()
}


object DoublePossibilities : Possibilities by EmptyPossibilitesFunc

// Long
val LONG_DEFAULT_VALUE: Long = 0L

object LongParser : ArgumentParser<SingleInput, Long> {
    override fun parse(
        input: SingleInput,
        valueOrValidationFactory: ValueOrValidationFactory
    ): ValueOrValidation<Long> =
        input.input.toLongOrNull()?.let { valueOrValidationFactory.value(it) }
                ?: valueOrValidationFactory.invalid()
}

object LongPossibilities : Possibilities by EmptyPossibilitesFunc

// Boolean
val BOOLEAN_DEFAULT_VALUE: Boolean = false

object BooleanParser : ArgumentParser<SingleInput, Boolean> {
    override fun parse(
        input: SingleInput,
        valueOrValidationFactory: ValueOrValidationFactory
    ): ValueOrValidation<Boolean> =
        when (input.input) {
            "true", "yes", "y", "valid" -> valueOrValidationFactory.value(true)
            "false", "no", "n", "invalid" -> valueOrValidationFactory.value(false)
            else -> valueOrValidationFactory.invalid()
        }
}


object BooleanPossibilities : Possibilities {
    override fun invoke(): List<Input> =
        listOf(
            SingleInput("true"), SingleInput("yes"), SingleInput("y"), SingleInput("valid"),
            SingleInput("false"), SingleInput("no"), SingleInput("n"), SingleInput("invalid")
        )
}

// String
val STRING_DEFAULT_VALUE: String? = null

class ExactStringParser(val string: String) : ArgumentParser<SingleInput, String> {
    override fun parse(
        input: SingleInput,
        valueOrValidationFactory: ValueOrValidationFactory
    ): ValueOrValidation<String> =
        if (input.input == string)
            valueOrValidationFactory.value(input.input)
        else
            valueOrValidationFactory.invalid()
}

class ExactStringPossibilities(val str: String) : Possibilities {
    override fun invoke(): List<Input> = listOf(SingleInput(str))
}


object StringParser : ArgumentParser<SingleInput, String> {
    override fun parse(
        input: SingleInput,
        valueOrValidationFactory: ValueOrValidationFactory
    ): ValueOrValidation<String> =
        valueOrValidationFactory.value(input.input)
}

object StringPossibilities : Possibilities by EmptyPossibilitesFunc

// Any

object AnyParser : ArgumentParser<Input, Any> {
    override fun parse(
        input: Input,
        valueOrValidationFactory: ValueOrValidationFactory
    ): ValueOrValidation<Any> =
        valueOrValidationFactory.value(input.toPlain())
}

object AnyPossibilities : Possibilities {
    override fun invoke(): List<Input> = emptyList()
}