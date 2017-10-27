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

import com.github.jonathanxd.iutils.type.AbstractTypeInfo
import com.github.jonathanxd.iutils.type.TypeInfo
import com.github.jonathanxd.kwcommands.argument.Argument
import com.github.jonathanxd.kwcommands.argument.ArgumentHandler
import com.github.jonathanxd.kwcommands.information.Information
import com.github.jonathanxd.kwcommands.information.RequiredInformation
import com.github.jonathanxd.kwcommands.manager.InformationManager
import com.github.jonathanxd.kwcommands.reflect.env.ArgumentType
import com.github.jonathanxd.kwcommands.requirement.Requirement

inline fun <reified T> InformationManager.registerInformation(tags: Array<String>, value: T, description: String? = null)
        = this.registerInformation(Information.Id(object : AbstractTypeInfo<T>() {}, tags), value, description)

inline fun <reified T> Information(tags: Array<String>, value: T, description: String?): Information<T> =
        Information<T>(Information.Id(object : AbstractTypeInfo<T>() {}, tags), value, description)

inline fun <reified T> InformationId(tags: Array<String>): Information.Id<T> =
        Information.Id(object : AbstractTypeInfo<T>() {}, tags)

/**
 * Reified function to create argument with a implicit [TypeInfo] of type [T].
 */
inline fun <reified T> Argument(id: Any,
                                name: String,
                                description: String,
                                isOptional: Boolean,
                                defaultValue: T?,
                                noinline validator: Validator,
                                noinline transformer: Transformer<T>,
                                noinline possibilities: PossibilitiesFunc,
                                requirements: List<Requirement<*, *>>,
                                requiredInfo: Set<RequiredInformation>,
                                handler: ArgumentHandler<T>? = null): Argument<T> =
        Argument(id,
                name,
                description,
                isOptional,
                object : AbstractTypeInfo<T>() {},
                defaultValue,
                validator,
                transformer,
                possibilities,
                requirements,
                requiredInfo,
                handler)

inline fun <reified T> ArgumentType(noinline validator: Validator,
                                    noinline transformer: Transformer<T>,
                                    noinline possibilities: PossibilitiesFunc,
                                    defaultValue: T?): ArgumentType<T> =
        ArgumentType(object : AbstractTypeInfo<T>() {}, validator, transformer, possibilities, defaultValue)


/**
 * Return `this` for chaining call.
 */
@Suppress("UNCHECKED_CAST")
inline fun <reified T> TypeInfo<*>.whenIs(type: TypeInfo<T>, exec: (TypeInfo<T>) -> Unit): TypeInfo<*> {
    if (this == type)
        exec(this as TypeInfo<T>)

    return this
}


inline fun <reified T> type(): TypeInfo<T> = object : AbstractTypeInfo<T>() {}