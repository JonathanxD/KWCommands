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
import com.github.jonathanxd.kwcommands.argument.Argument
import com.github.jonathanxd.kwcommands.argument.ArgumentContainer
import com.github.jonathanxd.kwcommands.argument.ArgumentHandler
import com.github.jonathanxd.kwcommands.parser.Input
import com.github.jonathanxd.kwcommands.argument.Transformer
import com.github.jonathanxd.kwcommands.argument.Validator
import com.github.jonathanxd.kwcommands.reflect.env.ArgumentTypeStorage

/**
 * Transformer of lists for varargs arguments.
 *
 * @property transformer Transformer of list elements.
 */
class ListTransformer<out T>(val transformer: Transformer<T>) : Transformer<List<T>> {
    override fun invoke(parsed: List<ArgumentContainer<*>>, current: Argument<*>, value: Input): List<T> =
            if (!value.input.isPresent) emptyList()
            else mutableListOf(transformer(parsed, current, value))
}

@Suppress("UNCHECKED_CAST")
class ReflectListValidator(val storage: ArgumentTypeStorage, val subType: TypeInfo<*>) : Validator {
    override fun invoke(parsed: List<ArgumentContainer<*>>, current: Argument<*>, value: Input): Boolean {
        if (!value.input.isPresent)
            return true

        val valueStr = value.value
        val list = if (valueStr.contains(','))
            valueStr.split(',').toList()
        else listOf(valueStr)

        val currentArgs = mutableListOf<ArgumentContainer<*>>()

        val get = storage.getArgumentType(subType)

        return list.all {
            val parsedArgs = parsed + currentArgs
            val res = get.validator(parsedArgs, current, Input(it))
            if (res) {
                currentArgs += ArgumentContainer(
                        current as Argument<Any>,
                        it,
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
        if (!value.input.isPresent)
            return emptyList()

        val valueStr = value.value
        val list = if (valueStr.contains(','))
            valueStr.split(',').toList()
        else listOf(valueStr)

        var container: ArgumentContainer<*>? = null
        val mut = mutableListOf<E>()

        val get = storage.getArgumentType(subType)

        return list.mapTo(mut) {
            val currentArgs = if (container != null) parsed + container!! else parsed
            val res = get.transformer(currentArgs, current, Input(it))
            container = ArgumentContainer(
                    current as Argument<E>,
                    valueStr,
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
        val consts = type.enumConstants as Array<Enum<*>>
        return consts.any { it.name.equals(value.value, true) }
    }
}

@Suppress("UNCHECKED_CAST")
class EnumTransformer<T>(val type: Class<T>) : Transformer<T> {
    override fun invoke(parsed: List<ArgumentContainer<*>>, current: Argument<*>, value: Input): T {
        val consts = type.enumConstants as Array<Enum<*>>
        return (consts.firstOrNull { it.name == value.value }
                ?: consts.first { it.name.equals(value.value, true) }) as T
    }
}

@Suppress("UNCHECKED_CAST")
fun enumPossibilities(type: Class<*>): List<String> =
    (type.enumConstants as Array<Enum<*>>).map { it.name }