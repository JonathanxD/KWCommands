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

import com.github.jonathanxd.iutils.`object`.Node
import com.github.jonathanxd.iutils.function.collector.BiCollectors
import com.github.jonathanxd.iutils.type.TypeInfo
import com.github.jonathanxd.jwiutils.kt.biStreamJavaBacked
import com.github.jonathanxd.kwcommands.ValidationTexts
import com.github.jonathanxd.kwcommands.argument.Argument
import com.github.jonathanxd.kwcommands.argument.ArgumentContainer
import com.github.jonathanxd.kwcommands.argument.ArgumentHandler
import com.github.jonathanxd.kwcommands.parser.*
import com.github.jonathanxd.kwcommands.reflect.env.ArgumentTypeStorage

object EmptyPossibilitesFunc : PossibilitiesFunc {
    override fun invoke(parsed: List<ArgumentContainer<*>>, current: Argument<*>): List<Input> =
            emptyList()
}

class MapTransformer<K, out V>(val keyTransformer: Transformer<K>,
                               val valueTransformer: Transformer<V>) : Transformer<Map<K, V>> {

    @Suppress("UNCHECKED_CAST")
    override fun invoke(parsed: List<ArgumentContainer<*>>, current: Argument<*>, value: Input): Map<K, V> {

        if (value is MapInput) {
            return value.input.biStreamJavaBacked()
                    .map { k, v -> Node(keyTransformer(parsed, current, k), keyTransformer(parsed, current, v)) }
                    .collect(BiCollectors.toMap()) as Map<K, V>
        }

        val str = (value as SingleInput).input

        val list = str.toCommandStringList(separator = '=')

        if (list.size == 1) {
            val input = SingleInput(list[0], str, 0, list[0].length)
            val key = keyTransformer.invoke(parsed, current, input)
            return mutableMapOf<Any?, Any?>(key to null) as MutableMap<K, V>
        } else if (list.size == 2) {
            val kInput = SingleInput(list[0], str, 0, list[0].length)
            val vInput = SingleInput(list[1], str, kInput.start + 1, list[1].length) // 1 = '='

            val key = keyTransformer.invoke(parsed, current, kInput)
            val vValue = valueTransformer.invoke(parsed, current, vInput)
            return mutableMapOf(key to vValue)
        }

        return mutableMapOf()
    }
}

object MapFormatCheckValidator : Validator {
    override fun invoke(parsed: List<ArgumentContainer<*>>, current: Argument<*>, value: Input): Validation {
        throw IllegalStateException("dummy")
    }
}

object ListFormatCheckValidator : Validator {
    override fun invoke(parsed: List<ArgumentContainer<*>>, current: Argument<*>, value: Input): Validation {
        throw IllegalStateException("dummy")
    }
}

class MapValidator(val keyValidator: Validator,
                   val valueValidator: Validator) : Validator {

    @Suppress("UNCHECKED_CAST")
    override fun invoke(parsed: List<ArgumentContainer<*>>, current: Argument<*>, value: Input): Validation {

        if (value is MapInput) {
            val inputMap = value.input

            return validation(inputMap.map { (k, v) ->
                keyValidator.invoke(parsed, current, k) to valueValidator.invoke(parsed, current, v)
            }.map { (l, r) -> Validation(l, r) })
        } else {
            return invalid(value, this, ValidationTexts.expectedInputMap(), listOf(MapInputType))
        }
    }
}

class MapPossibilitiesFunc(val keyValidator: PossibilitiesFunc,
                           val valueValidator: PossibilitiesFunc) : PossibilitiesFunc {

    override fun invoke(parsed: List<ArgumentContainer<*>>, current: Argument<*>): List<Input> {
        val list = mutableListOf<String>()

        for (ks in keyValidator(parsed, current)) {
            for (vs in valueValidator(parsed, current)) {
                list += "$ks=$vs"
            }
        }
        val map = mutableMapOf<Input, Input>()

        val ks = keyValidator.invoke(parsed, current)
        val vs = valueValidator.invoke(parsed, current)

        ks.forEach { k ->
            vs.forEach { v ->
                map.put(k, v)
            }
        }
        if (map.isEmpty())
            return emptyList()

        return listOf(MapInput(map))
    }
}

/**
 * Transformer of lists for multiple arguments.
 *
 * @property transformer Transformer of list elements.
 */
class ListTransformer<out T>(val transformer: Transformer<T>) : Transformer<List<T>> {
    override fun invoke(parsed: List<ArgumentContainer<*>>, current: Argument<*>, value: Input): List<T> =
            when (value) {
                is SingleInput -> mutableListOf(transformer(parsed, current, value))
                is EmptyInput -> mutableListOf()
                else -> (value as ListInput).input.map { transformer(parsed, current, it) }
            }
}

/**
 * Validator of lists for multiple arguments.
 *
 * @property transformer Transformer of list elements.
 */
class ListValidator(val validator: Validator) : Validator {
    override fun invoke(parsed: List<ArgumentContainer<*>>, current: Argument<*>, value: Input): Validation =
            when (value) {
                is SingleInput -> validator(parsed, current, value)
                is EmptyInput -> valid()
                is MapInput -> value.validate { validator(parsed, current, it) }
                else -> (value as ListInput).validate { validator(parsed, current, it) }
            }
}

@Suppress("UNCHECKED_CAST")
class ReflectListValidator(val storage: ArgumentTypeStorage, val subType: TypeInfo<*>) : Validator {
    override fun invoke(parsed: List<ArgumentContainer<*>>, current: Argument<*>, value: Input): Validation {
        if (value is ListInput) {
            val get = storage.getArgumentType(subType)

            return value.validate { get.validator(parsed, current, it) }
        }

        if (value !is SingleInput && value !is EmptyInput)
            return invalid(value, this, ValidationTexts.expectedInputList(), listOf(SingleInputType))

        if (value is EmptyInput)
            return valid()

        val valueStr = (value as SingleInput).input
        val list = if (valueStr.contains(','))
            valueStr.split(',').toList()
        else listOf(valueStr)

        val currentArgs = mutableListOf<ArgumentContainer<*>>()

        val get = storage.getArgumentType(subType)

        return list.mapIndexed { index, it ->
            val parsedArgs = parsed + currentArgs

            val pos = value.start + list.subList(0, index).sumBy { it.length + 1 } // 1 = ','
            val input = SingleInput(it, value.source, pos, pos + it.length)

            val res = get.validator(parsedArgs, current, input)
            if (res.isValid) {
                currentArgs += ArgumentContainer(
                        current as Argument<Any>,
                        input,
                        res,
                        current.handler as ArgumentHandler<Any>?
                )
            }
            res
        }.reduce { acc, validation -> acc + validation }
    }

}

@Suppress("UNCHECKED_CAST")
class ReflectListTransform<E>(val storage: ArgumentTypeStorage, val subType: TypeInfo<E>) : Transformer<List<E>> {
    override fun invoke(parsed: List<ArgumentContainer<*>>, current: Argument<*>, value: Input): List<E> {
        if (value is ListInput) {
            val get = storage.getArgumentType(subType)

            return value.input.map { get.transformer(parsed, current, it) }
        }

        if (value !is SingleInput && value !is EmptyInput)
            return emptyList()

        if (value is EmptyInput)
            return emptyList()

        val valueStr = (value as SingleInput).input
        val list = if (valueStr.contains(','))
            valueStr.split(',').toList()
        else listOf(valueStr)

        var container: ArgumentContainer<*>? = null
        val mut = mutableListOf<E>()

        val get = storage.getArgumentType(subType)

        return list.mapIndexedTo(mut) { index, it ->
            val pos = value.start + list.subList(0, index).sumBy { it.length + 1 } // 1 = ','
            val input = SingleInput(it, value.source, pos, pos + it.length)

            val currentArgs = if (container != null) parsed + container!! else parsed
            val res = get.transformer(currentArgs, current, input)

            container = ArgumentContainer(
                    current as Argument<E>,
                    input,
                    res,
                    current.handler as ArgumentHandler<E>?
            )
            res
        }
    }
}

@Suppress("UNCHECKED_CAST")
class EnumValidator<T>(val type: Class<T>) : Validator {
    override fun invoke(parsed: List<ArgumentContainer<*>>, current: Argument<*>, value: Input): Validation {
        if (value !is SingleInput)
            return invalid(value, this, ValidationTexts.expectedEnum(), listOf(SingleInputType))
        val consts = type.enumConstants as Array<Enum<*>>
        return if (consts.any { it.name.equals(value.input, true) }) valid()
        else invalid(value, this, ValidationTexts.invalidEnum(), listOf(SingleInputType))
    }
}

@Suppress("UNCHECKED_CAST")
class EnumTransformer<T>(val type: Class<T>) : Transformer<T> {
    override fun invoke(parsed: List<ArgumentContainer<*>>, current: Argument<*>, value: Input): T {
        if (value !is SingleInput)
            throw IllegalStateException()

        val consts = type.enumConstants as Array<Enum<*>>
        return (consts.firstOrNull { it.name == value.input }
                ?: consts.first { it.name.equals(value.input, true) }) as T
    }
}

class EnumPossibilitiesFunc(val type: Class<*>) : PossibilitiesFunc {
    @Suppress("UNCHECKED_CAST")
    override fun invoke(parsed: List<ArgumentContainer<*>>, current: Argument<*>): List<Input> =
            (type.enumConstants as Array<Enum<*>>).map { SingleInput(it.name) }

}

@Suppress("UNCHECKED_CAST")
fun enumPossibilities(type: Class<*>): List<Input> =
        (type.enumConstants as Array<Enum<*>>).map { SingleInput(it.name) }


// Short
val SHORT_DEFAULT_VALUE: Short = 0

object ShortValidator : Validator {
    override fun invoke(parsed: List<ArgumentContainer<*>>, current: Argument<*>, value: Input): Validation =
            when {
                value !is SingleInput ->
                    invalid(value, this, ValidationTexts.expectedShort(), listOf(SingleInputType))
                value.input.toShortOrNull() == null ->
                    invalid(value, this, ValidationTexts.invalidShort(), listOf(SingleInputType))
                else -> valid()
            }


}

object ShortTransformer : Transformer<Short> {
    override fun invoke(parsed: List<ArgumentContainer<*>>, current: Argument<*>, value: Input): Short =
            (value as SingleInput).input.toShort()

}

object ShortPossibilities : PossibilitiesFunc by EmptyPossibilitesFunc

// Char
val CHAR_DEFAULT_VALUE: Char = 0.toChar()

object CharValidator : Validator {
    override fun invoke(parsed: List<ArgumentContainer<*>>, current: Argument<*>, value: Input): Validation =
            when {
                value !is SingleInput ->
                    invalid(value, this, ValidationTexts.expectedChar(), listOf(SingleInputType))
                value.input.toCharArray().size != 1 ->
                    invalid(value, this, ValidationTexts.invalidChar(), listOf(SingleInputType))
                else -> valid()
            }

}

object CharTransformer : Transformer<Char> {
    override fun invoke(parsed: List<ArgumentContainer<*>>, current: Argument<*>, value: Input): Char =
            (value as SingleInput).input.toCharArray().single()

}

object CharPossibilities : PossibilitiesFunc by EmptyPossibilitesFunc

// Byte
val BYTE_DEFAULT_VALUE: Byte = 0.toByte()

object ByteValidator : Validator {
    override fun invoke(parsed: List<ArgumentContainer<*>>, current: Argument<*>, value: Input): Validation =
            when {
                value !is SingleInput ->
                    invalid(value, this, ValidationTexts.expectedByte(), listOf(SingleInputType))
                value.input.toByteOrNull() == null ->
                    invalid(value, this, ValidationTexts.invalidByte(), listOf(SingleInputType))
                else -> valid()
            }

}

object ByteTransformer : Transformer<Byte> {
    override fun invoke(parsed: List<ArgumentContainer<*>>, current: Argument<*>, value: Input): Byte =
            (value as SingleInput).input.toByte()

}

object BytePossibilities : PossibilitiesFunc by EmptyPossibilitesFunc

// Int
val INT_DEFAULT_VALUE: Int = 0

object IntValidator : Validator {
    override fun invoke(parsed: List<ArgumentContainer<*>>, current: Argument<*>, value: Input): Validation =
            when {
                value !is SingleInput ->
                    invalid(value, this, ValidationTexts.expectedInt(), listOf(SingleInputType))
                value.input.toIntOrNull() == null ->
                    invalid(value, this, ValidationTexts.invalidInt(), listOf(SingleInputType))
                else -> valid()
            }

}

object IntTransformer : Transformer<Int> {
    override fun invoke(parsed: List<ArgumentContainer<*>>, current: Argument<*>, value: Input): Int =
            (value as SingleInput).input.toInt()

}

object IntPossibilities : PossibilitiesFunc by EmptyPossibilitesFunc

// Float
val FLOAT_DEFAULT_VALUE: Int = 0

object FloatValidator : Validator {
    override fun invoke(parsed: List<ArgumentContainer<*>>, current: Argument<*>, value: Input): Validation =
            when {
                value !is SingleInput ->
                    invalid(value, this, ValidationTexts.expectedFloat(), listOf(SingleInputType))
                value.input.toFloatOrNull() == null ->
                    invalid(value, this, ValidationTexts.invalidFloat(), listOf(SingleInputType))
                else -> valid()
            }

}

object FloatTransformer : Transformer<Float> {
    override fun invoke(parsed: List<ArgumentContainer<*>>, current: Argument<*>, value: Input): Float =
            (value as SingleInput).input.toFloat()

}

object FloatPossibilities : PossibilitiesFunc by EmptyPossibilitesFunc

// Double
val DOUBLE_DEFAULT_VALUE: Double = 0.0

object DoubleValidator : Validator {
    override fun invoke(parsed: List<ArgumentContainer<*>>, current: Argument<*>, value: Input): Validation =
            when {
                value !is SingleInput ->
                    invalid(value, this, ValidationTexts.expectedDouble(), listOf(SingleInputType))
                value.input.toDoubleOrNull() == null ->
                    invalid(value, this, ValidationTexts.invalidDouble(), listOf(SingleInputType))
                else -> valid()
            }

}

object DoubleTransformer : Transformer<Double> {
    override fun invoke(parsed: List<ArgumentContainer<*>>, current: Argument<*>, value: Input): Double =
            (value as SingleInput).input.toDouble()

}

object DoublePossibilities : PossibilitiesFunc by EmptyPossibilitesFunc

// Long
val LONG_DEFAULT_VALUE: Long = 0L

object LongValidator : Validator {
    override fun invoke(parsed: List<ArgumentContainer<*>>, current: Argument<*>, value: Input): Validation =
            when {
                value !is SingleInput ->
                    invalid(value, this, ValidationTexts.expectedLong(), listOf(SingleInputType))
                value.input.toLongOrNull() == null ->
                    invalid(value, this, ValidationTexts.invalidLong(), listOf(SingleInputType))
                else -> valid()
            }

}

object LongTransformer : Transformer<Long> {
    override fun invoke(parsed: List<ArgumentContainer<*>>, current: Argument<*>, value: Input): Long =
            (value as SingleInput).input.toLong()

}

object LongPossibilities : PossibilitiesFunc by EmptyPossibilitesFunc

// Boolean
val BOOLEAN_DEFAULT_VALUE: Boolean = false

object BooleanValidator : Validator {
    override fun invoke(parsed: List<ArgumentContainer<*>>, current: Argument<*>, value: Input): Validation =
            when {
                value !is SingleInput ->
                    invalid(value, this, ValidationTexts.expectedBoolean(), listOf(SingleInputType))
                value.input != "true" && value.input != "false" ->
                    invalid(value, this, ValidationTexts.invalidBoolean(), listOf(SingleInputType))
                else -> valid()
            }

}

object BooleanTransformer : Transformer<Boolean> {
    override fun invoke(parsed: List<ArgumentContainer<*>>, current: Argument<*>, value: Input): Boolean =
            (value as SingleInput).input.toBoolean()

}

object BooleanPossibilities : PossibilitiesFunc {
    override fun invoke(parsed: List<ArgumentContainer<*>>, current: Argument<*>): List<Input> =
            listOf(SingleInput("true"), SingleInput("false"))
}

// String
val STRING_DEFAULT_VALUE: String? = null

object StringValidator : Validator {
    override fun invoke(parsed: List<ArgumentContainer<*>>, current: Argument<*>, value: Input): Validation =
            when (value) {
                !is SingleInput ->
                    invalid(value, this, ValidationTexts.expectedString(), listOf(SingleInputType))
                else -> valid()
            }

}

object StringTransformer : Transformer<String> {
    override fun invoke(parsed: List<ArgumentContainer<*>>, current: Argument<*>, value: Input): String =
            (value as SingleInput).input

}

object StringPossibilities : PossibilitiesFunc by EmptyPossibilitesFunc
