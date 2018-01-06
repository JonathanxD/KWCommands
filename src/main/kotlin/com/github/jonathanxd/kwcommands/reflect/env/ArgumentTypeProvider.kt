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
import com.github.jonathanxd.kwcommands.argument.ArgumentType

/**
 * Provides arguments.
 */
@FunctionalInterface
interface ArgumentTypeProvider {

    fun <T> provide(type: TypeInfo<T>, storage: ArgumentTypeStorage): ArgumentType<*, T>?

}

class ConcreteProviders : ArgumentTypeProvider {

    private val list = mutableListOf<ArgumentType<*, *>>()

    @Suppress("UNCHECKED_CAST")
    override fun <T> provide(type: TypeInfo<T>, storage: ArgumentTypeStorage): ArgumentType<*, T>? {

        return (this.list.find { it.type == type } ?: this.list.find {
            it.type.typeParameters.isEmpty() && it.type.classLiteral == type.classLiteral
        }) as? ArgumentType<*, T>

    }

}

class ConcreteProvider(val argumentType: ArgumentType<*, *>) : ArgumentTypeProvider {

    @Suppress("UNCHECKED_CAST")
    override fun <T> provide(type: TypeInfo<T>, storage: ArgumentTypeStorage): ArgumentType<*, T>? =
            if (this.argumentType.type == type || this.argumentType.type.isCompatible(type))
                this.argumentType as? ArgumentType<*, T>
            else null

}

fun <T> TypeInfo<T>.isCompatible(type: TypeInfo<*>) =
        this.classLiteral == type.classLiteral // Avoid resolution
                && ((type.typeParameters.isEmpty() && this.typeParameters.all { it == TypeInfo.of(Any::class.java) })
                || (this.typeParameters.isEmpty() && type.typeParameters.all { it == TypeInfo.of(Any::class.java) }))

/**
 * Cast [ArgumentType] to [ArgumentType] of [type] [T].
 */
@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
inline fun <T> ArgumentType<*, *>.cast(type: TypeInfo<T>): ArgumentType<*, T> {
    require(type.isAssignableFrom(this.type), { "Expression 'type.isAssignableFrom(this.type)' (type = $type, this.type = ${this.type}) returns false!" })
    return this as ArgumentType<*, T>
}