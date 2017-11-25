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

import com.github.jonathanxd.iutils.type.TypeInfo
import com.github.jonathanxd.kwcommands.ValidationTexts
import com.github.jonathanxd.kwcommands.argument.invokeAsInputCapable
import com.github.jonathanxd.kwcommands.parser.*
import com.github.jonathanxd.kwcommands.reflect.env.ArgumentTypeStorage

object EmptyPossibilitesFunc : Possibilities {
    override fun invoke(): List<Input> =
            emptyList()
}

class MapTransformer<K, out V>(val keyTransformer: Transformer<Input, K>,
                               val valueTransformer: Transformer<Input, V>) : Transformer<Input, Map<K, V>> {

    @Suppress("UNCHECKED_CAST")
    override fun invoke(value: Input): Map<K, V> {

        if (value is MapInput) {
            return value.input
                    .map { (k, v) ->
                        keyTransformer(k) to valueTransformer(v)
                    }
                    .toMap()
        }

        val str = (value as SingleInput).input

        val list = str.toCommandStringList(separator = '=')

        if (list.size == 1) {
            val input = SingleInput(list[0], str, 0, list[0].length)
            val key = keyTransformer.invoke(input)
            return mutableMapOf<Any?, Any?>(key to null) as MutableMap<K, V>
        } else if (list.size == 2) {
            val kInput = SingleInput(list[0], str, 0, list[0].length)
            val vInput = SingleInput(list[1], str, kInput.start + 1, list[1].length) // 1 = '='

            val key = keyTransformer.invoke(kInput)
            val vValue = valueTransformer.invoke(vInput)
            return mutableMapOf(key to vValue)
        }

        return mutableMapOf()
    }
}

object MapFormatCheckValidator : Validator<Input> {
    override fun invoke(value: Input): Validation {
        throw IllegalStateException("dummy")
    }
}

object ListFormatCheckValidator : Validator<Input> {
    override fun invoke(value: Input): Validation {
        throw IllegalStateException("dummy")
    }
}

class MapValidator(val keyValidator: Validator<Input>,
                   val valueValidator: Validator<Input>) : Validator<Input> {

    @Suppress("UNCHECKED_CAST")
    override fun invoke(value: Input): Validation {

        if (value is MapInput) {
            val inputMap = value.input

            return validation(inputMap.map { (k, v) ->
                keyValidator.invoke(k) to valueValidator.invoke(v)
            }.map { (l, r) -> Validation(l, r) })
        } else {
            return invalid(value, this, ValidationTexts.expectedInputMap(), listOf(MapInputType))
        }
    }
}

class MapPossibilitiesFunc(val keyValidator: Possibilities,
                           val valueValidator: Possibilities) : Possibilities {

    override fun invoke(): List<Input> {
        val list = mutableListOf<Pair<Input, Input>>()

        val ks = keyValidator.invoke()
        val vs = valueValidator.invoke()

        ks.forEach { k ->
            vs.forEach { v ->
                list += k to v
            }
        }

        if (list.isEmpty())
            return emptyList()

        return listOf(MapInput(list))
    }
}

/**
 * Transformer of lists for multiple arguments.
 *
 * @property transformer Transformer of list elements.
 */
class ListTransformer<out T>(val transformer: Transformer<Input, T>) : Transformer<Input, List<T>> {
    override fun invoke(value: Input): List<T> =
            when (value) {
                is SingleInput -> mutableListOf(transformer(value))
                is EmptyInput -> mutableListOf()
                else -> (value as ListInput).input.map { transformer(it) }
            }
}

/**
 * Validator of lists for multiple arguments.
 *
 * @property transformer Transformer of list elements.
 */
class ListValidator(val validator: Validator<Input>) : Validator<Input> {
    override fun invoke(value: Input): Validation =
            when (value) {
                is SingleInput -> validator(value)
                is EmptyInput -> valid()
                is MapInput -> value.validate { validator(it) }
                else -> (value as ListInput).validate { validator(it) }
            }
}

@Suppress("UNCHECKED_CAST")
class ReflectListValidator(val storage: ArgumentTypeStorage, val subType: TypeInfo<*>) : Validator<Input> {
    override fun invoke(value: Input): Validation {
        if (value is ListInput) {
            val get = storage.getArgumentType(subType)

            return value.validate { get.validator.invokeAsInputCapable(it) }
        }

        if (value !is SingleInput && value !is EmptyInput)
            return invalid(value, this, ValidationTexts.expectedInputList(), listOf(SingleInputType))

        if (value is EmptyInput)
            return valid()

        val valueStr = (value as SingleInput).input
        val list = if (valueStr.contains(','))
            valueStr.split(',').toList()
        else listOf(valueStr)

        val get = storage.getArgumentType(subType)

        return list.mapIndexed { index, it ->
            val pos = value.start + list.subList(0, index).sumBy { it.length + 1 } // 1 = ','
            val input = SingleInput(it, value.source, pos, pos + it.length)

            get.validator.invokeAsInputCapable(input)
        }.reduce { acc, validation -> acc + validation }
    }

}

@Suppress("UNCHECKED_CAST")
class ReflectListTransform<E>(val storage: ArgumentTypeStorage, val subType: TypeInfo<E>)
    : Transformer<Input, List<E>> {
    override fun invoke(value: Input): List<E> {
        if (value is ListInput) {
            val get = storage.getArgumentType(subType)

            return value.input.map { get.transformer.invokeAsInputCapable(it) } as List<E>
        }

        if (value !is SingleInput && value !is EmptyInput)
            return emptyList()

        if (value is EmptyInput)
            return emptyList()

        val valueStr = (value as SingleInput).input
        val list = if (valueStr.contains(','))
            valueStr.split(',').toList()
        else listOf(valueStr)

        val mut = mutableListOf<E>()

        val get = storage.getArgumentType(subType)

        return list.mapIndexedTo(mut) { index, it ->
            val pos = value.start + list.subList(0, index).sumBy { it.length + 1 } // 1 = ','
            val input = SingleInput(it, value.source, pos, pos + it.length)

            get.transformer.invokeAsInputCapable(input) as E
        }
    }
}

@Suppress("UNCHECKED_CAST")
class EnumValidator<T>(val type: Class<T>) : Validator<Input> {
    override fun invoke(value: Input): Validation {
        if (value !is SingleInput)
            return invalid(value, this, ValidationTexts.expectedEnum(), listOf(SingleInputType))
        val consts = type.enumConstants as Array<Enum<*>>
        return if (consts.any { it.name.equals(value.input, true) }) valid()
        else invalid(value, this, ValidationTexts.invalidEnum(), listOf(SingleInputType))
    }
}

@Suppress("UNCHECKED_CAST")
class EnumTransformer<T>(val type: Class<T>) : Transformer<SingleInput, T> {
    override fun invoke(value: SingleInput): T {
        val consts = type.enumConstants as Array<Enum<*>>
        return (consts.firstOrNull { it.name == value.input }
                ?: consts.first { it.name.equals(value.input, true) }) as T
    }
}

class EnumPossibilitiesFunc(val type: Class<*>) : Possibilities {
    @Suppress("UNCHECKED_CAST")
    override fun invoke(): List<Input> =
            (type.enumConstants as Array<Enum<*>>).map { SingleInput(it.name) }

}

@Suppress("UNCHECKED_CAST")
fun enumPossibilities(type: Class<*>): List<Input> =
        (type.enumConstants as Array<Enum<*>>).map { SingleInput(it.name) }


// Short
val SHORT_DEFAULT_VALUE: Short = 0

object ShortValidator : Validator<SingleInput> {
    override fun invoke(value: SingleInput): Validation =
            when {
                value.input.toShortOrNull() == null ->
                    invalid(value, this, ValidationTexts.invalidShort(), listOf(SingleInputType))
                else -> valid()
            }


}

object ShortTransformer : Transformer<SingleInput, Short> {
    override fun invoke(value: SingleInput): Short =
            value.input.toShort()

}

object ShortPossibilities : Possibilities by EmptyPossibilitesFunc

// Char
val CHAR_DEFAULT_VALUE: Char = 0.toChar()

object CharValidator : Validator<SingleInput> {
    override fun invoke(value: SingleInput): Validation =
            when {
                value.input.toCharArray().size != 1 ->
                    invalid(value, this, ValidationTexts.invalidChar(), listOf(SingleInputType))
                else -> valid()
            }

}

object CharTransformer : Transformer<SingleInput, Char> {
    override fun invoke(value: SingleInput): Char =
            value.input.toCharArray().single()

}

object CharPossibilities : Possibilities by EmptyPossibilitesFunc

// Byte
val BYTE_DEFAULT_VALUE: Byte = 0.toByte()

object ByteValidator : Validator<SingleInput> {
    override fun invoke(value: SingleInput): Validation =
            when {
                value.input.toByteOrNull() == null ->
                    invalid(value, this, ValidationTexts.invalidByte(), listOf(SingleInputType))
                else -> valid()
            }

}

object ByteTransformer : Transformer<SingleInput, Byte> {
    override fun invoke(value: SingleInput): Byte =
            value.input.toByte()

}

object BytePossibilities : Possibilities by EmptyPossibilitesFunc

// Int
val INT_DEFAULT_VALUE: Int = 0

object IntValidator : Validator<SingleInput> {
    override fun invoke(value: SingleInput): Validation =
            when {
                value.input.toIntOrNull() == null ->
                    invalid(value, this, ValidationTexts.invalidInt(), listOf(SingleInputType))
                else -> valid()
            }

}

object IntTransformer : Transformer<SingleInput, Int> {
    override fun invoke(value: SingleInput): Int =
            value.input.toInt()

}

object IntPossibilities : Possibilities by EmptyPossibilitesFunc

// Float
val FLOAT_DEFAULT_VALUE: Int = 0

object FloatValidator : Validator<SingleInput> {
    override fun invoke(value: SingleInput): Validation =
            when {
                value.input.toFloatOrNull() == null ->
                    invalid(value, this, ValidationTexts.invalidFloat(), listOf(SingleInputType))
                else -> valid()
            }

}

object FloatTransformer : Transformer<SingleInput, Float> {
    override fun invoke(value: SingleInput): Float =
            value.input.toFloat()

}

object FloatPossibilities : Possibilities by EmptyPossibilitesFunc

// Double
val DOUBLE_DEFAULT_VALUE: Double = 0.0

object DoubleValidator : Validator<SingleInput> {
    override fun invoke(value: SingleInput): Validation =
            when {
                value.input.toDoubleOrNull() == null ->
                    invalid(value, this, ValidationTexts.invalidDouble(), listOf(SingleInputType))
                else -> valid()
            }

}

object DoubleTransformer : Transformer<SingleInput, Double> {
    override fun invoke(value: SingleInput): Double =
            value.input.toDouble()

}

object DoublePossibilities : Possibilities by EmptyPossibilitesFunc

// Long
val LONG_DEFAULT_VALUE: Long = 0L

object LongValidator : Validator<SingleInput> {
    override fun invoke(value: SingleInput): Validation =
            when {
                value.input.toLongOrNull() == null ->
                    invalid(value, this, ValidationTexts.invalidLong(), listOf(SingleInputType))
                else -> valid()
            }

}

object LongTransformer : Transformer<SingleInput, Long> {
    override fun invoke(value: SingleInput): Long =
            value.input.toLong()

}

object LongPossibilities : Possibilities by EmptyPossibilitesFunc

// Boolean
val BOOLEAN_DEFAULT_VALUE: Boolean = false

object BooleanValidator : Validator<SingleInput> {
    override fun invoke(value: SingleInput): Validation =
            when {
                value.input != "true" && value.input != "false" ->
                    invalid(value, this, ValidationTexts.invalidBoolean(), listOf(SingleInputType))
                else -> valid()
            }

}

object BooleanTransformer : Transformer<SingleInput, Boolean> {
    override fun invoke(value: SingleInput): Boolean =
            value.input.toBoolean()

}

object BooleanPossibilities : Possibilities {
    override fun invoke(): List<Input> =
            listOf(SingleInput("true"), SingleInput("false"))
}

// String
val STRING_DEFAULT_VALUE: String? = null

object StringValidator : Validator<SingleInput> {
    override fun invoke(value: SingleInput): Validation =
            valid()

}

object StringTransformer : Transformer<SingleInput, String> {
    override fun invoke(value: SingleInput): String =
            value.input

}

object StringPossibilities : Possibilities by EmptyPossibilitesFunc
