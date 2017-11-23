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
import com.github.jonathanxd.iutils.box.IMutableBox
import com.github.jonathanxd.iutils.box.MutableBox
import com.github.jonathanxd.iutils.option.Options
import com.github.jonathanxd.kwcommands.ValidationTexts
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
class CommandParserImpl(override val commandManager: CommandManager) : CommandParser {

    override val options: Options = Options()

    override fun parseWithOwnerFunction(commandString: String,
                                        ownerProvider: OwnerProvider): List<CommandContainer> {
        val containers = mutableListOf<CommandContainer>()

        parse(commandString.sourcedCharIterator(), containers, CommandHolder(), ownerProvider)

        return containers
    }

    private fun parse(commandsIterator: SourcedCharIterator,
                      containers: MutableList<CommandContainer>,
                      lastCommand: CommandHolder,
                      ownerProvider: OwnerProvider) {
        while (commandsIterator.hasNext()) {
            val isCommand =
                    parseCommand(commandsIterator, containers, lastCommand, ownerProvider, !lastCommand.isPresent)

            if (!isCommand || !commandsIterator.hasNext()) {
                val last = lastCommand.value

                if (last != null) {
                    lastCommand.set(null)

                    val args = parseArguments(commandsIterator, last)

                    containers += CommandContainer(last, args, last.handler)
                }
            }
        }
    }

    private fun getCommand(name: String,
                           containers: MutableList<CommandContainer>,
                           lastCommand: CommandHolder,
                           owner: Any?): Command? {
        var last: Command? = lastCommand.getOrElse(null)

        while (last != null) {
            val cmd = this.commandManager.getSubCommand(last, name)

            if (cmd != null) {
                lastCommand.set(cmd)
                return cmd
            } else {
                if (last.arguments.isEmpty()) {
                    containers += CommandContainer(last, emptyList(), last.handler)
                }
                last = last.parent
            }
        }

        return this.commandManager.getCommand(name, owner)?.also {
            lastCommand.set(it)
        }
    }

    private fun SourcedCharIterator.parseSingle(): SingleInput = this.parseSingleInput()

    private fun SourcedCharIterator.peekSingle(): Pair<SingleInput, SourcedCharIterator> =
            this.runInNew { this.parseSingleInput() }

    private fun parseCommand(commandsIterator: SourcedCharIterator,
                             containers: MutableList<CommandContainer>,
                             lastCommand: CommandHolder,
                             ownerProvider: OwnerProvider,
                             required: Boolean): Boolean {
        val last = lastCommand.getOrElse(null)

        val peek = commandsIterator.peekSingle()
        val (input, iter) = peek

        if (input.content == "&") {
            commandsIterator.from(iter)

            if (last.arguments.isNotEmpty())
                failAME(last, emptyList(), last.arguments, emptyList())

            containers += CommandContainer(last, emptyList(), last.handler)

            lastCommand.set(null)

            return true
        }

        val command = getCommand(input.input, containers, lastCommand, ownerProvider(input.input))

        if (command != null) {
            commandsIterator.from(iter)
            lastCommand.set(command)
            return true
        }

        if (required)
            throw CommandNotFoundException(input.input, containers.toList(),
                    this.commandManager,
                    "Command '${input.input}' not found${if (last != null) " neither in parent command ${last.toStr()}" +
                            " nor in command manager" else " in command manager"}.")
        else
            return false
    }

    private fun parseArguments(commandsIterator: SourcedCharIterator,
                               command: Command): List<ArgumentContainer<*>> {
        if (command.arguments.isEmpty()) {
            return emptyList()
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
            val peek = commandsIterator.peekSingle()
            val (input, iter) = peek

            val named = input.input.getArgumentNameOrNull()?.let { name ->
                commandsIterator.from(iter)
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
            failAME(command, args, missing, provided)

        }

        return args
    }

    private fun parseSingleArgument(commandsIterator: SourcedCharIterator,
                                    command: Command,
                                    argument: Argument<*>,
                                    args: MutableList<ArgumentContainer<*>>,
                                    isNamed: Boolean): Boolean {
        val start = commandsIterator.sourceIndex
        val hasNext = commandsIterator.hasNext()
        val isBoolean = argument.isBoolean(args)
        val peek by lazy(LazyThreadSafetyMode.NONE) {
            commandsIterator.peekSingle()
        }
        val filled = (!hasNext || peek.first.input.isArgumentName()) && isBoolean

        val next = when {
            filled -> SingleInput("true", commandsIterator.sourceString, 0, 0)
            hasNext -> peek.first
            else -> failNIFAE(command, argument, args)
        }

        val validation = argument.validate(args, next)

        if (validation.isValid) {
            if (!filled) commandsIterator.from(peek.second)
            @Suppress("UNCHECKED_CAST")
            args += ArgumentContainer(argument,
                    next,
                    argument.transformer(args, argument, next),
                    argument.handler as ArgumentHandler<Any?>?
            )
            return true
        } else if (isNamed || !argument.isOptional)
            failIIFAE(command, next, argument, args, validation)

        return false
    }

    private fun Argument<*>.validate(args: List<ArgumentContainer<*>>, input: Input): Validation =
            this.validator(args, this, input)

    private fun parseVarargsArgument(commandsIterator: SourcedCharIterator,
                                     command: Command,
                                     argument: Argument<*>,
                                     args: MutableList<ArgumentContainer<*>>,
                                     isNamed: Boolean): Boolean {

        if (!commandsIterator.hasNext()) {
            if (argument.isOptional) {
                val input = EmptyInput(commandsIterator.sourceString)
                @Suppress("UNCHECKED_CAST")
                args += ArgumentContainer(argument,
                        input,
                        argument.transformer(args, argument, input),
                        argument.handler as ArgumentHandler<Any?>?
                )
            } else {
                failNIFAE(command, argument, args)
            }
        }

        val (next, _) = commandsIterator.peekSingle()

        return when {
            next.input.startsWith(MAP_OPEN) -> parseMapArgument(commandsIterator, command, argument, args, isNamed)
            next.input.startsWith(LIST_OPEN) -> parseListArgument(commandsIterator, command, argument, args, isNamed)
            else -> parseListArgument(commandsIterator, command, argument, args, isNamed)
        }
    }

    private fun parseListArgument(commandsIterator: SourcedCharIterator,
                                  command: Command,
                                  argument: Argument<*>,
                                  args: MutableList<ArgumentContainer<*>>,
                                  isNamed: Boolean): Boolean {
        if (commandsIterator.hasNext() && commandsIterator.peekSingle().first.input.startsWith(LIST_OPEN)) {

            val copy = commandsIterator.copy()

            val input = commandsIterator.parseListInput()

            val validation = argument.validate(args, input)
            if (validation.isInvalid) {
                if (!argument.isOptional || isNamed)
                    failIIFAE(command,
                            input,
                            argument,
                            args,
                            validation)
                else {
                    // I know, this may slow down the parser system a bit
                    // Because the list may be parsed twice
                    // But I think that it is not a problem (at least for now)
                    // This is required because as this is a parsing algorithm, and values can be peeked
                    // before it is parsed, failing to parse at some point, requires the
                    // pointer to go back to last valid state, otherwise the input will be ignored
                    // this also allows list to be parsed to other value for other arguments, example,
                    // for single input arguments, the first fragment (or the entire map) can be parsed
                    // as a single input string instead of a input list.
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

        val inputs = mutableListOf<Input>()
        val start = commandsIterator.sourceIndex

        while (commandsIterator.hasNext()) {
            val (peekInput, iter) = commandsIterator.peekSingle()

            if (peekInput.input.isArgumentName())
                break

            val validation = argument.validate(args, peekInput)

            if (validation.isValid) {
                commandsIterator.from(iter)
                inputs += peekInput
            } else {
                break
            }
        }

        if (inputs.isEmpty()) {
            if (!argument.isOptional || isNamed) {
                val (peekInput, iter) = commandsIterator.peekSingle()
                failIIFAE(command,
                        peekInput,
                        argument,
                        args,
                        validation(validatedElement(peekInput, ListFormatCheckValidator,
                                ValidationTexts.expectedInputList(), listOf(ListInputType))))
            } else {
                return false
            }
        }

        val end =
                if (inputs.isEmpty()) start
                else inputs.last().end

        val input = ListInput(inputs, commandsIterator.sourceString, start, end)

        val validation = argument.validate(args, input)

        if (validation.isInvalid) {
            if (!argument.isOptional || isNamed)
                failIIFAE(command,
                        input,
                        argument,
                        args,
                        validation)
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

    private fun parseMapArgument(commandsIterator: SourcedCharIterator,
                                 command: Command,
                                 argument: Argument<*>,
                                 args: MutableList<ArgumentContainer<*>>,
                                 isNamed: Boolean): Boolean {
        val peek = commandsIterator.peekSingle()
        val (peekInput, _) = peek
        val copy = commandsIterator.copy()

        if (!peekInput.input.startsWith(MAP_OPEN)) {
            throw failIIFAE(command,
                    peekInput,
                    argument,
                    args,
                    validation(validatedElement(peekInput, MapFormatCheckValidator,
                            ValidationTexts.expectedInputMap(), listOf(MapInputType))))
        }

        val input = commandsIterator.parseMapInput()

        val validation = argument.validate(args, input)
        if (validation.isInvalid) {
            if (!argument.isOptional || isNamed)
                failIIFAE(command,
                        input,
                        argument,
                        args,
                        validation)
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

    private fun failAME(command: Command,
                        args: List<ArgumentContainer<*>>,
                        missing: List<Argument<*>>,
                        provided: List<Argument<*>>): Nothing =
            throw ArgumentsMissingException(command,
                    args,
                    this.commandManager,
                    "Missing arguments ${missing.toStr()} of command ${command.toStr()}. Provided: ${provided.toStr()}.")

    private fun failIIFAE(command: Command,
                          input: Input,
                          argument: Argument<*>,
                          args: List<ArgumentContainer<*>>,
                          validation: Validation): Nothing =
            throw InvalidInputForArgumentException(command, args, input, argument, validation, this.commandManager,
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
    class CommandHolder : IMutableBox<Command> by MutableBox()

}