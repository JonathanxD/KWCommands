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
package com.github.jonathanxd.kwcommands.parser

import com.github.jonathanxd.iutils.annotation.Beta
import com.github.jonathanxd.iutils.option.Options
import com.github.jonathanxd.kwcommands.argument.Argument
import com.github.jonathanxd.kwcommands.argument.ArgumentContainer
import com.github.jonathanxd.kwcommands.argument.ArgumentHandler
import com.github.jonathanxd.kwcommands.command.Command
import com.github.jonathanxd.kwcommands.command.CommandContainer
import com.github.jonathanxd.kwcommands.exception.*
import com.github.jonathanxd.kwcommands.manager.CommandManager
import com.github.jonathanxd.kwcommands.util.*
import java.util.*

@Beta
class CommandParserV2(override val commandManager: CommandManager) : CommandParser {

    override val options: Options = Options()

    override fun parseWithOwnerFunction(stringList: List<String>,
                                        ownerProvider: OwnerProvider): List<CommandContainer> {
        val containers = mutableListOf<CommandContainer>()

        parse(PeekIterator(stringList), containers, CommandDeque(), ownerProvider)

        return containers
    }

    private fun parse(commandsIterator: PeekIterator,
                      containers: MutableList<CommandContainer>,
                      commands: CommandDeque,
                      ownerProvider: OwnerProvider) {
        while (commandsIterator.hasNext()) {
            val isCommand = parseCommand(commandsIterator, containers, commands, ownerProvider, commands.isEmpty())

            if (!isCommand) {
                val last = commands.peekLast()

                parseArguments(commandsIterator, containers, commands)
            }
        }
    }

    private fun parseCommand(commandsIterator: PeekIterator,
                             containers: MutableList<CommandContainer>,
                             commands: CommandDeque,
                             ownerProvider: OwnerProvider,
                             required: Boolean): Boolean {
        val last = if (commands.isEmpty()) null else commands.last
        val poll = commandsIterator.peekNext().escaped()
        val command = last?.let { this.commandManager.getSubCommand(it, poll) }
                ?: this.commandManager.getCommand(poll, ownerProvider(poll))

        if (command != null) {
            commandsIterator.next()
            commands.offerLast(command)
            return true
        }

        if (required)
            throw CommandNotFoundException(poll, containers.toList(),
                    this.commandManager,
                    "Command $poll not found${if (last != null) " in parent command ${last.toStr()}" else ""}.")
        else
            return false
    }

    private fun parseArguments(commandsIterator: PeekIterator,
                               containers: MutableList<CommandContainer>,
                               commands: CommandDeque) {
        val command = commands.pollLast()!!

        if (command.arguments.isEmpty()) {
            containers += CommandContainer(command, emptyList(), command.handler)
            return
        }

        val currentArgs = command.arguments.toMutableList()
        val args = mutableListOf<ArgumentContainer<*>>()
        val required = command.arguments.count { !it.isOptional }
        var currentRequired = 0
        val nextArgument = {
            val i = currentArgs.iterator()
            val next = i.next()
            i.remove()
            next
        }

        while (commandsIterator.hasNext() && currentArgs.isNotEmpty()) {
            val peek = commandsIterator.peekNext()

            val named = peek.getArgumentNameOrNull()?.let { name ->
                commandsIterator.next()
                (currentArgs.firstOrNull { it.nameOrId == name }
                        ?: failANFE(command, currentArgs, args, name)).also {
                    currentArgs.remove(it)
                }
            }
            val argument = named ?: nextArgument()

            val parse =
                    if (argument.isMultiple)
                        parseVarargsArgument(commandsIterator, command, argument, args, named != null)
                    else
                        parseSingleArgument(commandsIterator, command, argument, args, named != null)

            if (parse && !argument.isOptional)
                ++currentRequired
        }

        if (currentRequired != required) {
            val provided = args.map { it.argument }
            val missing = command.arguments.filterNot { provided.any { v -> it == v } }
            throw ArgumentsMissingException(command,
                    args,
                    this.commandManager,
                    "Missing arguments ${missing.toStr()} of command ${command.toStr()}. Provided: ${provided.toStr()}.")
        }

        containers += CommandContainer(command, args, command.handler)
    }

    private fun parseSingleArgument(commandsIterator: PeekIterator,
                                    command: Command,
                                    argument: Argument<*>,
                                    args: MutableList<ArgumentContainer<*>>,
                                    isNamed: Boolean): Boolean {
        val hasNext = commandsIterator.hasNext()
        val isBoolean = argument.isBoolean(args)

        val next = if (!hasNext && isBoolean) "true"
        else if (hasNext) commandsIterator.peekNext()
        else failNIFAE(command, argument, args)

        val input = SingleInput(next.escaped())

        if (argument.validator(args, argument, input)) {
            commandsIterator.next()
            @Suppress("UNCHECKED_CAST")
            args += ArgumentContainer(argument,
                    input,
                    argument.transformer(args, argument, input),
                    argument.handler as ArgumentHandler<Any?>?
            )
            return true
        } else if (isNamed)
            failIIFAE(command, input, argument, args)

        return false
    }


    private fun parseVarargsArgument(commandsIterator: PeekIterator,
                                     command: Command,
                                     argument: Argument<*>,
                                     args: MutableList<ArgumentContainer<*>>,
                                     isNamed: Boolean): Boolean {

        if (!commandsIterator.hasNext()) {
            if (argument.isOptional) {
                @Suppress("UNCHECKED_CAST")
                args += ArgumentContainer(argument,
                        EmptyInput,
                        argument.transformer(args, argument, EmptyInput),
                        argument.handler as ArgumentHandler<Any?>?
                )
            } else {
                failNIFAE(command, argument, args)
            }
        }

        val next = commandsIterator.peekNext()

        return if (next.startsWith("[")) {
            parseMapArgument(commandsIterator, command, argument, args, isNamed)
        } else {
            parseListArgument(commandsIterator, command, argument, args, isNamed)
        }
    }

    private fun parseListArgument(commandsIterator: PeekIterator,
                                  command: Command,
                                  argument: Argument<*>,
                                  args: MutableList<ArgumentContainer<*>>,
                                  isNamed: Boolean): Boolean {
        val inputs = mutableListOf<Input>()

        while (commandsIterator.hasNext()) {
            val peek = commandsIterator.peekNext()

            if (peek.isArgumentName())
                break

            val input = SingleInput(peek.escaped())

            if (argument.validator(args, argument, input)) {
                commandsIterator.next()
                inputs += input
            } else {
                break
            }
        }

        if (inputs.isEmpty()) {
            if (!argument.isOptional || isNamed) {
                failIIFAE(command,
                        SingleInput(commandsIterator.peekNext()),
                        argument,
                        args)
            } else {
                return false
            }
        }

        val input = ListInput(inputs)

        if (!argument.validator(args, argument, input)) {
            if (!argument.isOptional || isNamed)
                failIIFAE(command,
                        input,
                        argument,
                        args)
            else
                return false
        }

        @Suppress("UNCHECKED_CAST")
        args += ArgumentContainer(argument,
                input,
                argument.transformer(args, argument, input),
                argument.handler as ArgumentHandler<Any?>?
        )

        return true
    }

    private fun parseMapArgument(commandsIterator: PeekIterator,
                                 command: Command,
                                 argument: Argument<*>,
                                 args: MutableList<ArgumentContainer<*>>,
                                 isNamed: Boolean): Boolean {
        val peek = commandsIterator.peekNext()
        val copy = commandsIterator.copy()

        if (!peek.startsWith("["))
            throw failIIFAE(command,
                    SingleInput(peek),
                    argument,
                    args)

        val chars = commandsIterator.charIter()

        val map = chars.parseMap()
        val input = MapInput(map.toInputMap())

        if (!argument.validator(args, argument, input)) {
            if (!argument.isOptional || isNamed)
                failIIFAE(command,
                        input,
                        argument,
                        args)
            else {
                // I know, this may slow down the parser system a bit
                // Because the map may be parsed twice
                // But I think that it is not a problem (at least for now)
                // This is required because as this is a parsing algorithm, and values can be peeked
                // before it is parsed, failing to parse at some point, requires the
                // pointer to go back to last valid state, otherwise the input will be ignored
                // this also allows map to be parsed to other value for other arguments, example,
                // for single input arguments, the first fragment (or the entire map) can be parsed
                // as a single input string instead of a input map.
                commandsIterator.from(copy) // Restore old state
                return false
            }
        }

        @Suppress("UNCHECKED_CAST")
        args += ArgumentContainer(argument,
                input,
                argument.transformer(args, argument, input),
                argument.handler as ArgumentHandler<Any?>?
        )

        return true
    }

    @Suppress("UNCHECKED_CAST")
    private fun Map<Any, Any>.toInputMap(): Map<Input, Input> =
            this.map { (k, v) ->
                val cK = if (k is String) SingleInput(k) else MapInput((k as Map<Any, Any>).toInputMap())
                val cV = if (v is String) SingleInput(v) else MapInput((v as Map<Any, Any>).toInputMap())
                cK to cV
            }.toMap()

    private fun failIIFAE(command: Command,
                          input: Input,
                          argument: Argument<*>,
                          args: List<ArgumentContainer<*>>): Nothing =
            throw InvalidInputForArgumentException(command, args, input, argument, this.commandManager,
                    "Invalid input '$input' for argument '${argument.toStr()}' of command '${command.toStr()}'.")

    private fun failNIFAE(command: Command,
                          argument: Argument<*>,
                          args: List<ArgumentContainer<*>>): Nothing =
            throw NoInputForArgumentException(command, args, argument, this.commandManager,
                    "No input provided for argument '${argument.toStr()}' of command '${command.toStr()}'.")

    private fun failANFE(command: Command,
                         currentArgs: List<Argument<*>>,
                         args: List<ArgumentContainer<*>>,
                         name: String): Nothing =
            throw ArgumentNotFoundException(command, args, name, this.commandManager,
                    "Cannot find argument with name '$name' in arguments '${currentArgs.toStr()}'" +
                            " of command '${command.toStr()}'")

    private fun String.escaped() = this.escape('\\')
    private fun String.isArgumentName() = this.startsWith("--")
    private fun String.getArgumentName() = this.substring(2) // "--".length
    private fun String.getArgumentNameOrNull() = if (!this.isArgumentName()) null else this.substring(2) // "--".length

    class CommandDeque : Deque<Command> by LinkedList()


}