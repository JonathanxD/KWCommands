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

import com.github.jonathanxd.iutils.`object`.Either
import com.github.jonathanxd.iutils.`object`.specialized.EitherObjBoolean
import com.github.jonathanxd.iutils.box.IMutableBox
import com.github.jonathanxd.iutils.box.MutableBox
import com.github.jonathanxd.iutils.option.Options
import com.github.jonathanxd.jwiutils.kt.left
import com.github.jonathanxd.jwiutils.kt.leftBooleanObj
import com.github.jonathanxd.jwiutils.kt.right
import com.github.jonathanxd.kwcommands.ValidationTexts
import com.github.jonathanxd.kwcommands.argument.*
import com.github.jonathanxd.kwcommands.command.Command
import com.github.jonathanxd.kwcommands.command.CommandContainer
import com.github.jonathanxd.kwcommands.fail.*
import com.github.jonathanxd.kwcommands.manager.CommandManager
import com.github.jonathanxd.kwcommands.util.*

class CommandParserImpl(override val commandManager: CommandManager) : CommandParser {

    override val options: Options = Options()

    override fun parseWithOwnerFunction(commandString: String,
                                        ownerProvider: OwnerProvider): Either<ParseFail, List<CommandContainer>> =
        parseWithOwnerFunction(commandString.sourcedCharIterator(), ownerProvider)

    override fun parseWithOwnerFunction(commandIter: SourcedCharIterator,
                                        ownerProvider: OwnerProvider): Either<ParseFail, List<CommandContainer>> =
            this.parse(commandIter, mutableListOf<CommandContainer>(), CommandHolder(), ownerProvider)

    private fun parse(commandsIterator: SourcedCharIterator,
                      containers: MutableList<CommandContainer>,
                      lastCommand: CommandHolder,
                      ownerProvider: OwnerProvider): Either<ParseFail, List<CommandContainer>> {
        if (!commandsIterator.hasNext())
            return left(createFailCNF(
                    EmptyInput(commandsIterator.sourceString),
                    containers,
                    commandsIterator
            ))

        commandsIterator.jumpBlankSpace()

        while (commandsIterator.hasNext()) {
            val isCommand =
                    parseCommand(commandsIterator, containers, lastCommand, ownerProvider, !lastCommand.isPresent)

            if (isCommand.isLeft)
                return left(isCommand.left)

            if (!isCommand.right || !commandsIterator.hasNext()) {
                val last = lastCommand.value

                if (last != null) {
                    lastCommand.set(null)

                    val args = parseArguments(commandsIterator, last, containers)

                    if (args.isLeft)
                        return left(args.left)

                    containers += CommandContainer(last, args.right, last.handler)
                }
            }
        }

        return right(containers)
    }

    private fun getCommand(name: String,
                           containers: MutableList<CommandContainer>,
                           lastCommand: CommandHolder,
                           owner: Any?): Command? {
        val list = mutableListOf<Command>()

        var last: Command? = lastCommand.getOrElse(null)

        while (last != null) {
            val cmd = this.commandManager.getSubCommand(last, name)

            if (cmd != null) {
                lastCommand.set(cmd)
                list.forEach {
                    containers += CommandContainer(it, emptyList(), it.handler)
                }
                return cmd
            } else {
                if (last.arguments.isEmpty()) {
                    list += last
                }
                last = last.parent
            }
        }

        return this.commandManager.getCommand(name, owner)?.also {
            lastCommand.set(it)
        }.also {
            if (it != null) {
                list.forEach {
                    containers += CommandContainer(it, emptyList(), it.handler)
                }
            }
        }
    }

    private fun parseCommand(commandsIterator: SourcedCharIterator,
                             containers: MutableList<CommandContainer>,
                             lastCommand: CommandHolder,
                             ownerProvider: OwnerProvider,
                             required: Boolean): EitherObjBoolean<ParseFail> {
        val last = lastCommand.getOrElse(null)

        val state = commandsIterator.pos
        val next = commandsIterator.parseSingleInput(anyArgumentType)

        val input = next.rightOrNull() as? SingleInput ?: return right(false)

        if (input.content == "&") {
            if (last.arguments.isNotEmpty())
                return leftBooleanObj(createFailAME(last, emptyList(), containers, commandsIterator))

            containers += CommandContainer(last, emptyList(), last.handler)

            lastCommand.set(null)

            return right(true)
        }

        val command = getCommand(input.input, containers, lastCommand, ownerProvider(input.input))

        if (command != null) {
            lastCommand.set(command)
            return right(true)
        }

        return if (required) {
            leftBooleanObj(createFailCNF(input, containers, commandsIterator))
        } else {
            commandsIterator.restore(state)
            right(false)
        }
    }

    private fun parseArguments(commandsIterator: SourcedCharIterator,
                               command: Command,
                               parsedCommands: List<CommandContainer>): Either<ParseFail, List<ArgumentContainer<*>>> {
        if (command.arguments.isEmpty()) {
            return right(emptyList())
        }

        val currentArgs = command.arguments.toMutableList()
        val args = mutableListOf<ArgumentContainer<*>>()
        val required = command.arguments.count { !it.isOptional }
        var currentRequired = 0

        while (commandsIterator.hasNext() && currentArgs.isNotEmpty()) {
            val state = commandsIterator.pos
            val get = commandsIterator.parseSingleInput(stringArgumentType)

            val input = get.rightOrNull() as? SingleInput ?: break

            val named = input.input.getArgumentNameOrNull()?.let { name ->
                (currentArgs.firstOrNull { it.name == name }
                        ?: return left(createFailANFE(command, args, name, parsedCommands, commandsIterator))).also {
                    currentArgs.remove(it)
                }
            }

            val argument = if (named != null) {
                currentArgs.remove(named)
                named
            } else {
                commandsIterator.restore(state)
                val i = currentArgs.iterator()
                val next = i.next()
                i.remove()
                next
            }

            val parse =
                    if (argument.argumentType.inputType !is SingleInputType)
                        parseVarargsArgument(commandsIterator, command, argument,
                                args, named != null, parsedCommands)
                    else
                        parseSingleArgument(commandsIterator, command, argument,
                                args, named != null, parsedCommands)

            if (parse.isRight && !argument.isOptional) {
                ++currentRequired
            }

            if (parse.isLeft) {
                if (argument.isOptional) {
                    commandsIterator.restore(state)
                } else {
                    return left(parse.left)
                }
            }
        }

        if (currentRequired != required) {
            return left(createFailAME(command, args, parsedCommands, commandsIterator))
        }

        command.arguments.filter { a -> args.none { it.argument == a }
                && a.isOptional
                && a.argumentType.defaultValue != null
        }.forEach {
            @Suppress("UNCHECKED_CAST")
            args += ArgumentContainer(it,
                    null,
                    it.argumentType.defaultValue,
                    it.handler as ArgumentHandler<Any?>?
            )
        }


        return right(args)
    }

    private fun parseSingleArgument(commandsIterator: SourcedCharIterator,
                                    command: Command,
                                    argument: Argument<*>,
                                    args: MutableList<ArgumentContainer<*>>,
                                    isNamed: Boolean,
                                    parsedCommands: List<CommandContainer>): EitherObjBoolean<ParseFail> {
        val isBoolean = argument.isBoolean()
        val state = commandsIterator.pos
        val peek = commandsIterator.parseSingleInput(argument.argumentType)
        val right = peek.rightOrNull() as? SingleInput

        val fill = (right == null || right.input.isArgumentName()) && isBoolean

        val next = when {
            fill -> SingleInput("true", commandsIterator.sourceString, 0, 0)
            peek.isRight -> peek.right
            peek.isLeft -> return leftBooleanObj(createFailAIPF(command, argument, args,
                    parsedCommands, peek.left, commandsIterator))
            else -> return leftBooleanObj(createFailNIFAE(command, argument, args,
                    parsedCommands, commandsIterator))
        }

        val validation = argument.validate(next)

        if (validation.isValid) {
            @Suppress("UNCHECKED_CAST")
            args += ArgumentContainer(argument,
                    next,
                    argument.argumentType.transform(next),
                    argument.handler as ArgumentHandler<Any?>?
            )
            return right(true)
        } else {
            if (!fill) commandsIterator.restore(state)
            if (isNamed || !argument.isOptional)
                return leftBooleanObj(createFailIIFAE(command, next, argument, args, validation,
                        parsedCommands, commandsIterator))
        }

        return right(false)
    }

    private fun Argument<*>.validate(input: Input): Validation =
            this.argumentType.validate(input)

    private fun parseVarargsArgument(commandsIterator: SourcedCharIterator,
                                     command: Command,
                                     argument: Argument<*>,
                                     args: MutableList<ArgumentContainer<*>>,
                                     isNamed: Boolean,
                                     parsedCommands: List<CommandContainer>): EitherObjBoolean<ParseFail> {
        val prevState = commandsIterator.pos
        val nextInput = commandsIterator.parseSingleInput(stringArgumentType)

        if (nextInput.isLeft) {
            if (argument.isOptional) {
                val input = EmptyInput(commandsIterator.sourceString)
                @Suppress("UNCHECKED_CAST")
                args += ArgumentContainer(argument,
                        input,
                        argument.argumentType.transform(input),
                        argument.handler as ArgumentHandler<Any?>?
                )
            } else {
                return leftBooleanObj(createFailNIFAE(command, argument, args, parsedCommands, commandsIterator))
            }
        }

        val next = nextInput.right

        commandsIterator.restore(prevState)

        return when {
            next.content.startsWith(MAP_OPEN) ->
                parseMapArgument(commandsIterator, command, argument, args, isNamed, parsedCommands)
            next.content.startsWith(LIST_OPEN) ->
                parseListArgument(commandsIterator, command, argument, args, isNamed, parsedCommands)
            argument.argumentType.inputType is AnyInputType ->
                parseSingleArgument(commandsIterator, command, argument, args, isNamed, parsedCommands)
            else ->
                parseListArgument(commandsIterator, command, argument, args, isNamed, parsedCommands)
        }
    }

    private fun parseListArgument(commandsIterator: SourcedCharIterator,
                                  command: Command,
                                  argument: Argument<*>,
                                  args: MutableList<ArgumentContainer<*>>,
                                  isNamed: Boolean,
                                  parsedCommands: List<CommandContainer>): EitherObjBoolean<ParseFail> {
        val prevState = commandsIterator.pos
        val nextInput = commandsIterator.parseSingleInput(stringArgumentType)
        commandsIterator.restore(prevState)

        if (nextInput.isRight && nextInput.right.content.startsWith(LIST_OPEN)) {

            val input = commandsIterator.parseListInput(argumentType = argument.argumentType)

            if (input.isLeft)
                return leftBooleanObj(createFailAIPF(command,
                        argument,
                        args,
                        parsedCommands,
                        input.left,
                        commandsIterator
                ))


            val validation = argument.validate(input.right)
            if (validation.isInvalid) {
                if (!argument.isOptional || isNamed)
                    return leftBooleanObj(createFailIIFAE(command,
                            input.right,
                            argument,
                            args,
                            validation,
                            parsedCommands,
                            commandsIterator))
                else {
                    // I know, this may slow down the parser system a bit
                    // Because the list may be parsed twice
                    // But I think that it is not a problem (at least for now)
                    // This is required because as this is a parsing algorithm, and values can be peeked
                    // before it is parsed, failing to parse at some point, requires the
                    // pointer to go back to last valid state, otherwise the input will be ignored
                    // this also allows list to be parsed to other value for other arguments
                    commandsIterator.restore(prevState) // Restore old state
                    return right(false)
                }
            }

            @Suppress("UNCHECKED_CAST")
            args += ArgumentContainer(argument,
                    input.right,
                    argument.argumentType.transform(input.right),
                    argument.handler as ArgumentHandler<Any?>?
            )

            return right(true)
        }

        val initalState = commandsIterator.pos
        val inputs = mutableListOf<Input>()
        val start = commandsIterator.sourceIndex

        var index = 0
        val type = argument.argumentType

        while (true) {
            if (!type.hasType(index))
                break

            val elementType = type.getListType(index)

            val state = commandsIterator.pos
            val input = commandsIterator.parseSingleInput(elementType)

            if (input.isLeft) {
                if (inputs.isEmpty() && !argument.isOptional) {
                    return leftBooleanObj(createFailAIPF(command,
                            argument,
                            args,
                            parsedCommands,
                            input.left,
                            commandsIterator))
                } else {
                    commandsIterator.restore(state)
                    break
                }
            }

            if (input.right.content.isArgumentName()) {
                commandsIterator.restore(state)
                break
            }


            val validation = elementType.validate(input.right)

            if (validation.isValid) {
                inputs += input.right
            } else {
                commandsIterator.restore(state)
                break
            }

            ++index
        }

        if (inputs.isEmpty()) {
            return if (!argument.isOptional || isNamed) {
                // String?
                val next = commandsIterator.parseSingleInput(stringArgumentType)
                val peek_ = next.rightOrNull() ?: EmptyInput(commandsIterator.sourceString)

                leftBooleanObj(createFailIIFAE(command,
                        peek_,
                        argument,
                        args,
                        validation(validatedElement(peek_,
                                argument.argumentType,
                                ListFormatCheckValidator,
                                ValidationTexts.expectedInputList(), ListInputType)),
                        parsedCommands,
                        commandsIterator))
            } else {
                right(false)
            }
        }

        val end =
                if (inputs.isEmpty()) start
                else inputs.last().end

        val input = ListInput(inputs, commandsIterator.sourceString, start, end)

        val validation = argument.validate(input)

        if (validation.isInvalid) {
            return if (!argument.isOptional || isNamed)
                leftBooleanObj(createFailIIFAE(command,
                        input,
                        argument,
                        args,
                        validation,
                        parsedCommands,
                        commandsIterator))
            else {
                commandsIterator.restore(initalState)
                right(false)
            }
        }

        @Suppress("UNCHECKED_CAST")
        args += ArgumentContainer(argument,
                input,
                argument.argumentType.transform(input),
                argument.handler as ArgumentHandler<Any?>?
        )

        return right(true)
    }

    private fun parseMapArgument(commandsIterator: SourcedCharIterator,
                                 command: Command,
                                 argument: Argument<*>,
                                 args: MutableList<ArgumentContainer<*>>,
                                 isNamed: Boolean,
                                 parsedCommands: List<CommandContainer>): EitherObjBoolean<ParseFail> {
        val state = commandsIterator.pos
        val next = commandsIterator.parseSingleInput(stringArgumentType)
        commandsIterator.restore(state)

        if (next.isLeft || !next.right.content.startsWith(MAP_OPEN)) {
            val peek_ = next.rightOrNull() ?: EmptyInput(commandsIterator.sourceString)

            return leftBooleanObj(createFailIIFAE(command,
                    peek_,
                    argument,
                    args,
                    validation(validatedElement(peek_,
                            argument.argumentType,
                            MapFormatCheckValidator,
                            ValidationTexts.expectedInputMap(), MapInputType)),
                    parsedCommands,
                    commandsIterator))
        }

        val input = commandsIterator.parseMapInput(argumentType = argument.argumentType)

        if (input.isLeft)
            return leftBooleanObj(
                    createFailAIPF(command,
                            argument,
                            args,
                            parsedCommands,
                            input.left,
                            commandsIterator
                    ))

        val validation = argument.validate(input.right)
        if (validation.isInvalid) {
            if (!argument.isOptional || isNamed)
                return leftBooleanObj(createFailIIFAE(command,
                        input.right,
                        argument,
                        args,
                        validation,
                        parsedCommands,
                        commandsIterator))
            else {
                // I know, this may slow down the parser system a bit
                // Because the map may be parsed twice
                // But I think that it is not a problem (at least for now)
                // This is required because as this is a parsing algorithm, and values can be peeked
                // before it is parsed, failing to parse at some point, requires the
                // pointer to go back to last valid state, otherwise the input will be ignored
                // this also allows map to be parsed to other value for other arguments
                commandsIterator.restore(state) // Restore old state
                return right(false)
            }
        }

        @Suppress("UNCHECKED_CAST")
        args += ArgumentContainer(argument,
                input.right,
                argument.argumentType.transform(input.right),
                argument.handler as ArgumentHandler<Any?>?
        )

        return right(true)
    }

    private fun createFailCNF(input: Input,
                              parsedCommands: List<CommandContainer>,
                              iter: SourcedCharIterator): ParseFail =
            CommandNotFoundFail(input, parsedCommands, this.commandManager, iter)


    private fun createFailAME(command: Command,
                              args: List<ArgumentContainer<*>>,
                              parsedCommands: List<CommandContainer>,
                              iter: SourcedCharIterator): ParseFail =
            ArgumentsMissingFail(command, args, parsedCommands,
                    this.commandManager,
                    iter)

    private fun createFailIIFAE(command: Command,
                                input: Input,
                                argument: Argument<*>,
                                args: List<ArgumentContainer<*>>,
                                validation: Validation,
                                parsedCommands: List<CommandContainer>,
                                iter: SourcedCharIterator): ParseFail =
            InvalidInputForArgumentFail(command, args, input, argument, validation, parsedCommands,
                    this.commandManager, iter)

    private fun createFailAIPF(command: Command,
                               argument: Argument<*>,
                               args: List<ArgumentContainer<*>>,
                               parsedCommands: List<CommandContainer>,
                               fail: InputParseFail,
                               iter: SourcedCharIterator): ParseFail =
            ArgumentInputParseFail(command, args, argument, fail,
                    parsedCommands, this.commandManager, iter)

    private fun createFailNIFAE(command: Command,
                                argument: Argument<*>,
                                args: List<ArgumentContainer<*>>,
                                parsedCommands: List<CommandContainer>,
                                iter: SourcedCharIterator): ParseFail =
            NoInputForArgumentFail(command, args, argument, parsedCommands, this.commandManager, iter)

    private fun createFailANFE(command: Command,
                               args: List<ArgumentContainer<*>>,
                               name: String,
                               parsedCommands: List<CommandContainer>,
                               iter: SourcedCharIterator): ParseFail =
            ArgumentNotFoundFail(command, args, name, parsedCommands, this.commandManager, iter)


    class CommandHolder : IMutableBox<Command> by MutableBox()
}