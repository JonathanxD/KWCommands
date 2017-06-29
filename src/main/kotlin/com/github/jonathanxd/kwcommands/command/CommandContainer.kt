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
package com.github.jonathanxd.kwcommands.command

import com.github.jonathanxd.iutils.type.TypeInfo
import com.github.jonathanxd.kwcommands.argument.ArgumentContainer
import com.github.jonathanxd.kwcommands.interceptor.CommandInterceptor
import java.util.*

/**
 * Container to hold parsed [command][Command].
 *
 * @property command Parsed command.
 * @property arguments Parsed arguments passed to command.
 * @property handler Handler of command. This handler is always the same as [Command.handler] for original containers, but
 * for modified containers this handler may or may not be the same as [Command.handler] (see [CommandInterceptor]).
 */
data class CommandContainer(val command: Command,
                            val arguments: List<ArgumentContainer<*>>,
                            val handler: Handler?) : Container {

    /**
     * Gets argument by [id] and [type]. Returns found argument casted to [ArgumentContainer] of [T] or
     * null if argument cannot be found.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getArgument(id: Any, type: TypeInfo<T>): ArgumentContainer<T>? {
        return arguments.firstOrNull { it.argument.id == id && it.argument.type == type } as? ArgumentContainer<T>
    }

    /**
     * Gets argument by [id]. Returns found argument casted to [ArgumentContainer] of [T] or
     * null if argument cannot be found.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getArgument(id: Any): ArgumentContainer<T>? {
        return arguments.firstOrNull { it.argument.id == id } as? ArgumentContainer<T>
    }

    /**
     * Gets argument by [id] and [type]. Returns found argument casted to [ArgumentContainer] of [T] or
     * null if argument cannot be found.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getArgumentValue(id: Any, type: TypeInfo<T>): T? {
        return this.getArgument(id, type)?.value
    }

    /**
     * Gets argument by [id]. Returns found argument casted to [ArgumentContainer] of [T] or
     * null if argument cannot be found.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getArgumentValue(id: Any): T? {
        return this.getArgument<T>(id)?.value
    }

    /**
     * Gets argument by [id] and [type]. Returns found argument casted to [ArgumentContainer] of [T] or
     * null if argument cannot be found.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getArgumentValueOptional(id: Any, type: TypeInfo<T>): Optional<T> {
        return Optional.ofNullable(this.getArgumentValue(id, type))
    }

    /**
     * Gets argument by [id]. Returns found argument casted to [ArgumentContainer] of [T] or
     * null if argument cannot be found.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getArgumentValueOptional(id: Any): Optional<T> {
        return Optional.ofNullable(this.getArgumentValue(id))
    }

    override fun toString(): String {
        return "CommandContainer(command = $command, arguments = ${this.arguments.map { "argument = Argument(${it.argument.id}: ${it.argument.type}), value = ${it.value}" }.joinToString()}, handler = ${handler?.javaClass})"
    }

}