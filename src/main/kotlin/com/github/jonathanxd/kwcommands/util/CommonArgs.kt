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
import com.github.jonathanxd.kwcommands.argument.Argument
import com.github.jonathanxd.kwcommands.argument.ArgumentContainer
import com.github.jonathanxd.kwcommands.argument.ArgumentHandler
import com.github.jonathanxd.kwcommands.parser.*
import com.github.jonathanxd.kwcommands.reflect.env.ArgumentTypeStorage

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
            val input = SingleInput(list[0])
            val key = keyTransformer.invoke(parsed, current, input)
            return mutableMapOf<Any?, Any?>(key to null) as MutableMap<K, V>
        } else if (list.size == 2) {
            val kInput = SingleInput(list[0])
            val vInput = SingleInput(list[1])

            val key = keyTransformer.invoke(parsed, current, kInput)
            val vValue = valueTransformer.invoke(parsed, current, vInput)
            return mutableMapOf(key to vValue)
        }

        return mutableMapOf()
    }
}

class MapValidator(val keyValidator: Validator,
                   val valueValidator: Validator) : Validator {

    @Suppress("UNCHECKED_CAST")
    override fun invoke(parsed: List<ArgumentContainer<*>>, current: Argument<*>, value: Input): Boolean {

        if (value is MapInput) {
            val inputMap = value.input

            return inputMap.all { (k, v) ->
                keyValidator.invoke(parsed, current, k) && valueValidator.invoke(parsed, current, v)
            }
        } else {

            val str = (value as? SingleInput)?.input ?: return false

            val list = str.toCommandStringList(separator = '=')

            if (list.size == 1) {
                val input = SingleInput(list[0])
                return keyValidator(parsed, current, input)
            } else if (list.size == 2) {
                val kInput = SingleInput(list[0])
                val vInput = SingleInput(list[1])

                return keyValidator(parsed, current, kInput)
                        && valueValidator(parsed, current, vInput)
            }

            return false
        }
    }
}

class MapPossibilitiesFunc(val keyValidator: PossibilitiesFunc,
                           val valueValidator: PossibilitiesFunc) : PossibilitiesFunc {

    override fun invoke(parsed: List<ArgumentContainer<*>>, current: Argument<*>): Map<String, List<String>> {
        val list = mutableListOf<String>()

        for (ks in keyValidator(parsed, current)) {
            for (vs in valueValidator(parsed, current)) {
                list += "$ks=$vs"
            }
        }

        return mapOf("key" to keyValidator.invoke(parsed, current).values.flatMap { it }.toList(),
                "value" to valueValidator.invoke(parsed, current).values.flatMap { it }.toList())
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
    override fun invoke(parsed: List<ArgumentContainer<*>>, current: Argument<*>, value: Input): Boolean =
        when (value) {
            is SingleInput -> validator(parsed, current, value)
            is EmptyInput -> true
            else -> (value as ListInput).input.all { validator(parsed, current, it) }
        }
}

@Suppress("UNCHECKED_CAST")
class ReflectListValidator(val storage: ArgumentTypeStorage, val subType: TypeInfo<*>) : Validator {
    override fun invoke(parsed: List<ArgumentContainer<*>>, current: Argument<*>, value: Input): Boolean {
        if (value is ListInput) {
            val get = storage.getArgumentType(subType)

            return value.input.all { get.validator(parsed, current, it) }
        }

        if (value !is SingleInput && value !is EmptyInput)
            return false

        if (value is EmptyInput)
            return true

        val valueStr = (value as SingleInput).input
        val list = if (valueStr.contains(','))
            valueStr.split(',').toList()
        else listOf(valueStr)

        val currentArgs = mutableListOf<ArgumentContainer<*>>()

        val get = storage.getArgumentType(subType)

        return list.all {
            val parsedArgs = parsed + currentArgs
            val res = get.validator(parsedArgs, current, SingleInput(it))
            if (res) {
                currentArgs += ArgumentContainer(
                        current as Argument<Any>,
                        SingleInput(it),
                        res,
                        current.handler as ArgumentHandler<Any>?
                )
            }
            res
        }
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

        return list.mapTo(mut) {
            val currentArgs = if (container != null) parsed + container!! else parsed
            val res = get.transformer(currentArgs, current, SingleInput(it))
            container = ArgumentContainer(
                    current as Argument<E>,
                    SingleInput(valueStr),
                    res,
                    current.handler as ArgumentHandler<E>?
            )
            res
        }
    }
}

@Suppress("UNCHECKED_CAST")
class EnumValidator<T>(val type: Class<T>) : Validator {
    override fun invoke(parsed: List<ArgumentContainer<*>>, current: Argument<*>, value: Input): Boolean {
        if (value !is SingleInput)
            return false
        val consts = type.enumConstants as Array<Enum<*>>
        return consts.any { it.name.equals(value.input, true) }
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

@Suppress("UNCHECKED_CAST")
fun enumPossibilities(type: Class<*>): Map<String, List<String>> =
        mapOf("" to (type.enumConstants as Array<Enum<*>>).map { it.name })