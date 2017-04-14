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

import com.github.jonathanxd.iutils.type.ConcreteTypeInfo
import com.github.jonathanxd.iutils.type.TypeInfo
import com.github.jonathanxd.kwcommands.argument.Argument
import com.github.jonathanxd.kwcommands.argument.ArgumentHandler
import com.github.jonathanxd.kwcommands.information.Information
import com.github.jonathanxd.kwcommands.manager.InformationManager
import com.github.jonathanxd.kwcommands.reflect.env.ArgumentType
import com.github.jonathanxd.kwcommands.requirement.Requirement

inline fun <reified T> InformationManager.registerInformation(id: Information.Id, value: T, description: String? = null)
        = this.registerInformation(id, value, object : ConcreteTypeInfo<T>() {}, description)

inline fun <reified T> Information(id: Information.Id, value: T, description: String?): Information<T> =
        Information<T>(id, value, object : ConcreteTypeInfo<T>(){}, description)

/**
 * Reified function to create argument with a implicit [TypeInfo] of type [T].
 */
inline fun <reified T> Argument(id: Any,
                                isOptional: Boolean,
                                defaultValue: T?,
                                noinline validator: (String) -> Boolean,
                                noinline transformer: (String) -> T,
                                possibilities: List<String>,
                                requirements: List<Requirement<*, *>>,
                                handler: ArgumentHandler<T>? = null): Argument<T> =
        Argument(id, isOptional, object : ConcreteTypeInfo<T>() {}, defaultValue, validator, transformer, possibilities, requirements, handler)

inline fun <reified T> ArgumentType(noinline validator: (String) -> Boolean,
                                    noinline transformer: (String) -> T,
                                    possibilities: List<String>,
                                    defaultValue: T?): ArgumentType<T> =
        ArgumentType(object : ConcreteTypeInfo<T>() {}, validator, transformer, possibilities, defaultValue)


/**
 * Return `this` for chaining call.
 */
@Suppress("UNCHECKED_CAST")
inline fun <reified T> TypeInfo<*>.whenIs(type: TypeInfo<T>, exec: (TypeInfo<T>) -> Unit): TypeInfo<*> {
    if (this == type)
        exec(this as TypeInfo<T>)

    return this
}
