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
package com.github.jonathanxd.kwcommands.processor

import com.github.jonathanxd.kwcommands.argument.ArgumentContainer
import com.github.jonathanxd.kwcommands.command.Command
import com.github.jonathanxd.kwcommands.command.CommandContainer
import com.github.jonathanxd.kwcommands.exception.ArgumentsMissingException
import com.github.jonathanxd.kwcommands.exception.CommandNotFoundException
import com.github.jonathanxd.kwcommands.interceptor.CommandInterceptor
import com.github.jonathanxd.kwcommands.manager.CommandManager
import com.github.jonathanxd.kwcommands.manager.InformationManager
import com.github.jonathanxd.kwcommands.manager.RequirementManager
import com.github.jonathanxd.kwcommands.util.escape
import java.util.*

object Processors {


    @JvmStatic
    fun createCommonProcessor(): CommandProcessor =
            CommonCommandProcessor()


    private class CommonCommandProcessor : CommandProcessor {

        private val interceptors = mutableSetOf<CommandInterceptor>()

        override val commandManager: CommandManager = CommandManager()
        override val informationManager: InformationManager = InformationManager()
        override val requirementManager: RequirementManager = RequirementManager()

        override fun registerInterceptor(commandInterceptor: CommandInterceptor): Boolean =
                this.interceptors.add(commandInterceptor)

        override fun unregisterInterceptor(commandInterceptor: CommandInterceptor): Boolean =
                this.interceptors.remove(commandInterceptor)

        override fun process(stringList: List<String>, owner: Any?): List<CommandContainer> {
            val commands = mutableListOf<CommandContainer>()
            val deque: Deque<Command> = LinkedList()
            val getStr: (Int) -> String = { stringList[it].escape('\\') }

            var index = 0

            while (index < stringList.size) {
                var command: Command? = null

                if(stringList[index].isAndOp()) {
                    deque.clear()
                    ++index
                    continue
                }

                while (command == null) {

                    if (deque.isEmpty()) {
                        command = commandManager.getCommand(getStr(index), owner)

                        if(command == null)
                            throw CommandNotFoundException("Command ${getStr(index)} (index $index in $stringList) was not found.")
                    } else {
                        command = deque.last.getSubCommand(getStr(index))

                        if (command == null) {
                            val rm = deque.removeLast()

                            if(rm.parent != null)
                                deque.offerLast(rm.parent)
                        }
                    }
                }

                if(index + 1 == stringList.size ||
                        (index + 1 < stringList.size
                                && (stringList[index + 1].isAndOp() || command.getSubCommand(getStr(index + 1)) == null))) {

                    val arguments = command.arguments.toMutableList()
                    val args = mutableListOf<ArgumentContainer<*>>()

                    if(!arguments.isEmpty()) {
                        val requiredArgsCount = arguments.count { !it.isOptional }

                        if (index + requiredArgsCount >= stringList.size) {

                            val required = arguments.filter { !it.isOptional }.map { it.id.toString() }.joinToString()
                            val provided = if (index + 1 < stringList.size)
                                ", Provided arguments: ${stringList.subList(index + 1, stringList.size).joinToString()}"
                            else ""

                            throw ArgumentsMissingException("Some required arguments of command $command is missing (Required arguments ids: $required$provided).")
                        }

                        var requiredCount = 0

                        val size = (index + arguments.size).let {
                            if(it >= stringList.size)
                                stringList.size - 1
                            else
                                it
                        }

                        ((index + 1)..size).forEach {
                            val argStr = getStr(it)

                            val arg = arguments.find { it.validator(argStr) }

                            if (arg != null) {
                                if(!arg.isOptional)
                                    requiredCount++
                                args.add(ArgumentContainer(arg, argStr, arg.transformer(argStr)))
                                arguments.remove(arg)
                                ++index
                            }
                        }

                        if (requiredCount != requiredArgsCount) {
                            val missing = arguments.filter { !it.isOptional }.map { it.id.toString() }.joinToString()
                            throw ArgumentsMissingException("Some required arguments of command $command is missing. (Missing arguments ids: $missing)")
                        }

                        arguments.map { ArgumentContainer(it, null, it.defaultValue) }
                    }

                    commands += CommandContainer(
                            command = command,
                            handler = command.handler,
                            arguments = args)
                } else {
                    deque.offer(command)
                }

                ++index
            }

            return commands
        }

        override fun handle(commands: List<CommandContainer>): List<Result> {
            val results = mutableListOf<Result>()

            commands.forEach { command ->

                var container: CommandContainer? = command

                interceptors.forEach { interceptor ->
                    container?.let {
                        container = interceptor.pre(command, it)
                    }
                }

                container?.let {
                    val result = Result(it.handler?.handle(it, this.informationManager, this.requirementManager), it)
                    results += result

                    interceptors.forEach { interceptor ->
                        interceptor.post(command, it, result)
                    }
                }
            }

            return results
        }

        @Suppress("NOTHING_TO_INLINE")
        inline private fun String.isAndOp() = this == "&"
    }

}