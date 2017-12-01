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
import com.github.jonathanxd.iutils.type.TypeInfoUtil
import com.github.jonathanxd.kwcommands.argument.Argument
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
     * Gets argument by [name] and [type]. Returns found argument casted to [Argument] of [T] or
     * null if argument cannot be found.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getCommandArgument(name: String, type: TypeInfo<T>): Argument<T>? =
            this.command.arguments.firstOrNull { it.name == name
                    && TypeInfoUtil.isNormalizedEquals(it.argumentType.type, type) } as? Argument<T>

    /**
     * Gets [command] argument by [name] and [type]. Returns found argument casted to [ArgumentContainer] of [T] or
     * null if argument cannot be found.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getArgument(name: String, type: TypeInfo<T>): ArgumentContainer<T>? =
            this.getCommandArgument(name, type)?.let { arg ->
                arguments.firstOrNull { it.argument == arg } as ArgumentContainer<T>
            }

    /**
     * Gets [command] argument by [name]. Returns found argument casted to [Argument] of [T] or
     * null if argument cannot be found.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getCommandArgument(name: String): Argument<T>? =
            this.command.arguments.firstOrNull { it.name == name } as? Argument<T>

    /**
     * Gets argument by [name]. Returns found argument casted to [ArgumentContainer] of [T] or
     * null if argument cannot be found.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getArgument(name: String): ArgumentContainer<T>? =
            this.getCommandArgument<T>(name)?.let { arg ->
                arguments.firstOrNull { it.argument == arg } as ArgumentContainer<T>
            }

    /**
     * Gets argument container of [argument] specification.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getArgumentContainer(argument: Argument<T>): ArgumentContainer<T>? =
            this.arguments.firstOrNull { it.argument == argument } as ArgumentContainer<T>

    /**
     * Gets argument by [name]. Returns found argument casted to [ArgumentContainer] of [T] or
     * null if argument cannot be found.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getArgumentValue(name: String, type: TypeInfo<T>): T? =
        this.getArgument(name, type)?.value

    /**
     * Gets argument by [name]. Returns found argument casted to [ArgumentContainer] of [T] or
     * null if argument cannot be found.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getArgumentValue(name: String): T? =
        this.getArgument<T>(name)?.value

    /**
     * Gets argument by [name] and [type]. Returns found argument casted to [ArgumentContainer] of [T] or
     * null if argument cannot be found.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getArgumentValueOptional(name: String, type: TypeInfo<T>): Optional<T> =
        Optional.ofNullable(this.getArgumentValue(name, type))

    /**
     * Gets argument by [name]. Returns found argument casted to [ArgumentContainer] of [T] or
     * null if argument cannot be found.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getArgumentValueOptional(name: String): Optional<T> =
        Optional.ofNullable(this.getArgumentValue(name))

    override fun toString(): String {
        return "CommandContainer(command = $command, arguments = ${this.arguments.map { "argument = Argument(${it.argument.name}: ${it.argument.argumentType}), value = ${it.value}" }.joinToString()}, handler = ${handler?.javaClass})"
    }

}