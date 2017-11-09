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

import com.github.jonathanxd.kwcommands.argument.Argument
import com.github.jonathanxd.kwcommands.argument.ArgumentContainer
import com.github.jonathanxd.kwcommands.command.Command

/**
 * This property provides the "inheritance" level of this command.
 */
val Command.level: Int
    get() {
        var current: Command? = this.parent
        var count = 0

        while (current != null) {
            ++count
            current = current.parent
        }

        return count
    }

val Argument<*>.nameOrId get() = if (this.name.isEmpty()) this.id.toString() else this.name
val Argument<*>.nameOrIdWithType
    get() =
        "${this.nameOrId}: ${this.typeStr}"

val Argument<*>.typeStr: String
    get() = if (this.type.canResolve()) this.type.toString() else this.type.classLiteral

@Deprecated(message = "Use is boolean function")
val Argument<*>.isBoolean: Boolean
    get() = this.type.canResolve()
            && (this.type.typeClass == Boolean::class.javaObjectType || this.type.typeClass == Boolean::class.javaPrimitiveType)
            && this.validator(emptyList(), this, "true")

fun Argument<*>.isBoolean(parsedArgs: List<ArgumentContainer<*>>): Boolean =
        this.type.canResolve()
                && (this.type.typeClass == Boolean::class.javaObjectType
                || this.type.typeClass == Boolean::class.javaPrimitiveType
                )
                && this.validator(parsedArgs, this, "true")