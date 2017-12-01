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
import com.github.jonathanxd.kwcommands.argument.SingleArgumentType
import com.github.jonathanxd.kwcommands.command.Command
import com.github.jonathanxd.kwcommands.parser.SingleInput

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

fun Command.toStr() = this.fullname

fun Iterable<Argument<*>>.toStr() = this.joinToString { it.toStr() }
fun Argument<*>.toStr() = this.name

val Argument<*>.nameWithType
    get() =
        "${this.name}: ${this.typeStr}"

val Argument<*>.typeStr: String
    get() = if (this.argumentType.type.canResolve()) this.argumentType.type.toString() else this.argumentType.type.classLiteral

fun Argument<*>.isBoolean(): Boolean =
        this.argumentType is SingleArgumentType<*>
                && this.argumentType.type.toFullString().let { it == "boolean" || it == "java.lang.Boolean" }
                && this.argumentType.validate(SingleInput("true", "true", 0, 4)).isValid