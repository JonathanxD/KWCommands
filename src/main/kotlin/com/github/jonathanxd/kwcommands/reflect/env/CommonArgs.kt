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
package com.github.jonathanxd.kwcommands.reflect.env

import com.github.jonathanxd.iutils.type.TypeInfo

class ListValidator(val storage: ArgumentTypeStorage, val subType: TypeInfo<*>) : (String) -> Boolean {
    override fun invoke(p1: String): Boolean {
        val list = if (p1.contains(','))
            p1.split(',').toList()
        else listOf(p1)

        val get = storage.getArgumentType(subType)

        return list.all { get.validator(it) }
    }

}

class ListTransform<E>(val storage: ArgumentTypeStorage, val subType: TypeInfo<E>) : (String) -> List<E> {
    override fun invoke(p1: String): List<E> {
        val list = if (p1.contains(','))
            p1.split(',').toList()
        else listOf(p1)

        val mut = mutableListOf<E>()

        val get = storage.getArgumentType(subType)

        return list.mapTo(mut) { get.transformer(it) }
    }

}

@Suppress("UNCHECKED_CAST")
class EnumValidator<T>(val type: Class<T>) : (String) -> Boolean {
    override fun invoke(p1: String): Boolean {
        val consts = type.enumConstants as Array<Enum<*>>
        return consts.any { it.name.equals(p1, true) }
    }

}

@Suppress("UNCHECKED_CAST")
class EnumTransformer<T>(val type: Class<T>) : (String) -> T {
    override fun invoke(p1: String): T {
        val consts = type.enumConstants as Array<Enum<*>>
        return (consts.firstOrNull { it.name == p1 } ?: consts.first { it.name.equals(p1, true) }) as T
    }
}

fun enumPossibilities(type: Class<*>): List<String> =
    (type.enumConstants as Array<Enum<*>>).map { it.name }