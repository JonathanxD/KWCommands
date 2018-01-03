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
import com.github.jonathanxd.kwcommands.argument.Argument
import com.github.jonathanxd.kwcommands.argument.ArgumentContainer
import com.github.jonathanxd.kwcommands.argument.ArgumentHandler
import com.github.jonathanxd.kwcommands.argument.Arguments
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
                                        ownerProvider: OwnerProvider): Either<ParseFail, List<CommandContainer>> {
        val inputs = this.parseInputs(commandIter)

        /*if (inputs.isLeft) {
            return left(ContinuableFail(inputs.left, object : ContinuableParse{
                override fun resume(): Either<ParseFail, List<CommandContainer>> =
                    parse(commandIter.sourceString, inputs.right, mutableListOf(), CommandHolder(), ownerProvider)
            }))
        }*/

        return this.parse(commandIter.sourceString, inputs, commandIter, mutableListOf(), CommandHolder(), ownerProvider)
    }

    private fun parseInputs(commandsIterator: SourcedCharIterator): List<Either<InputParseFail, Input>> {
        val inputs = mutableListOf<Either<InputParseFail, Input>>()

        while (commandsIterator.hasNext()) {
            val parse = commandsIterator.parseSingleInput(parseData = true)

            if (parse.isLeft)
                inputs += left(parse.left)
            else
                inputs += right(parse.right)
        }

        return inputs
    }

    private fun parse(sourceString: String,
                      inputs: List<Either<InputParseFail, Input>>,
                      iter: SourcedCharIterator,
                      containers: MutableList<CommandContainer>,
                      lastCommand: CommandHolder,
                      ownerProvider: OwnerProvider): Either<ParseFail, List<CommandContainer>> {
        val inputIter: StatedIterator<Input> = ListBackedStatedIterator(inputs, iter)

        if (inputs.isEmpty())
            return left(createFailCNF(
                    EmptyInput(sourceString),
                    containers,
                    inputIter
            ))

        while (inputIter.hasNext()) {
            val command =
                    parseCommand(inputIter, containers, lastCommand, ownerProvider, !lastCommand.isPresent)

            if (command.isLeft)
                return left(command.left)

            if (!command.right || !inputIter.hasNext()) {
                val last = lastCommand.value

                if (last != null) {
                    lastCommand.set(null)

                    val args = parseArguments(inputIter, sourceString, last, containers)

                    if (args.isLeft)
                        return left(args.left)

                    containers += CommandContainer(last, args.right, last.handler)
                }
            }
        }

        return right(containers)

    }

    private fun parseCommand(inputIter: StatedIterator<Input>,
                             containers: MutableList<CommandContainer>,
                             lastCommand: CommandHolder,
                             ownerProvider: OwnerProvider,
                             required: Boolean): EitherObjBoolean<ParseFail> {
        val state = inputIter.pos
        val input = inputIter.next()
        val last = lastCommand.getOrElse(null)

        val commandInput = input.rightOrNull() as? SingleInput ?: return right(false)

        if (commandInput.content == "&") {
            if (last.arguments.getArguments().isNotEmpty())
                return leftBooleanObj(createFailAME(last, emptyList(), containers, commandInput.source, inputIter))

            containers += CommandContainer(last, emptyList(), last.handler)

            lastCommand.set(null)

            return right(true)
        }

        val command = getCommand(commandInput.input, containers, lastCommand, ownerProvider(commandInput.input))

        if (command != null) {
            lastCommand.set(command)
            return right(true)
        }

        return if (required) {
            leftBooleanObj(createFailCNF(commandInput, containers, inputIter))
        } else {
            inputIter.restore(state)
            right(false)
        }
    }


    private fun parseArguments(inputsIter: StatedIterator<Input>,
                               source: String,
                               command: Command,
                               parsedCommands: List<CommandContainer>): Either<ParseFail, List<ArgumentContainer<*>>> {
        if (command.arguments.getArguments().isEmpty()) {
            return right(emptyList())
        }

        val parsing = ArgumentParsing(command.arguments)
        val args = parsing.argumentList

        var currentRequired = 0

        while (inputsIter.hasNext() && parsing.hasNext()) {
            val state = inputsIter.pos
            val get = inputsIter.next()

            val name = (get.rightOrNull() as? SingleInput)?.input?.getArgumentNameOrNull()

            val argument = if (name != null) {
                parsing.getByName(name) ?: return left(createFailANFE(command, args, get.right, parsedCommands, inputsIter))
            } else {
                inputsIter.restore(state)
                parsing.next()
            }

            val parse =
                    if (argument.argumentType.inputType !is SingleInputType)
                        parseVarargsArgument(inputsIter, source, command, argument,
                                args, name != null, parsedCommands)
                    else
                        parseSingleArgument(inputsIter, source, command, argument,
                                args, name != null, parsedCommands)

            if (parse.isRight && parse.right && !argument.isOptional) {
                ++currentRequired
            }

            if (parse.isRight && !parse.right && argument.isOptional && name == null) {

                @Suppress("UNCHECKED_CAST")
                args += ArgumentContainer(argument,
                        null,
                        argument.argumentType.defaultValue,
                        argument.handler as ArgumentHandler<Any?>?
                )
            }

            if (parse.isLeft) {
                if (argument.isOptional && name == null) {
                    @Suppress("UNCHECKED_CAST")
                    args += ArgumentContainer(argument,
                            null,
                            argument.argumentType.defaultValue,
                            argument.handler as ArgumentHandler<Any?>?
                    )
                    inputsIter.restore(state)
                } else {
                    return left(parse.left)
                }
            }
        }

        val commandArgumentsList = command.arguments.getArguments(args)
        val required = commandArgumentsList.count { !it.isOptional }

        if (required != 0) {
            return left(createFailAME(command, args, parsedCommands, source, inputsIter))
        }

        commandArgumentsList.filter { a ->
            args.none { it.argument == a }
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


        return right(parsing.argumentList)
    }

    private fun parseSingleArgument(inputsIter: StatedIterator<Input>,
                                    source: String,
                                    command: Command,
                                    argument: Argument<*>,
                                    args: MutableList<ArgumentContainer<*>>,
                                    isNamed: Boolean,
                                    parsedCommands: List<CommandContainer>): EitherObjBoolean<ParseFail> {
        /*if (!inputsIter.hasNext())
            return leftBooleanObj(createFailNIFAE(command, argument, args,
                    parsedCommands,
                    source,
                    inputsIter))*/

        val isBoolean = argument.isBoolean()
        val state = inputsIter.pos
        val peek = inputsIter.next()
        val right = peek.rightOrNull() as? SingleInput

        val fill = (right == null || right.input.isArgumentName()) && isBoolean

        val next = when {
            fill -> SingleInput("true", source, 0, 0)
            peek.isRight -> peek.right
            peek.isLeft -> return leftBooleanObj(createFailAIPF(command, argument, args,
                    parsedCommands, peek.left, inputsIter))
            else -> return leftBooleanObj(createFailNIFAE(command, argument, args,
                    parsedCommands, source, inputsIter))
        }

        val parsed = argument.argumentType.parse(next)

        if (parsed.isValue) {
            @Suppress("UNCHECKED_CAST")
            args += ArgumentContainer(argument,
                    next,
                    parsed.value,
                    argument.handler as ArgumentHandler<Any?>?
            )
            return right(true)
        } else {
            if (!fill) inputsIter.restore(state)
            if (isNamed || !argument.isOptional)
                return leftBooleanObj(createFailIIFAE(command, next, argument, args, parsed.validation,
                        parsedCommands, inputsIter))
        }

        return right(false)
    }

    private fun parseVarargsArgument(inputsIter: StatedIterator<Input>,
                                     source: String,
                                     command: Command,
                                     argument: Argument<*>,
                                     args: MutableList<ArgumentContainer<*>>,
                                     isNamed: Boolean,
                                     parsedCommands: List<CommandContainer>): EitherObjBoolean<ParseFail> {

        val prevState = inputsIter.pos
        val nextInput = inputsIter.nextOrNull()

        if (nextInput == null) {
            return if (argument.isOptional) {
                val input = EmptyInput(source)
                @Suppress("UNCHECKED_CAST")
                args += ArgumentContainer(argument,
                        input,
                        argument.argumentType.defaultValue,
                        argument.handler as ArgumentHandler<Any?>?
                )
                right(true)
            } else {
                leftBooleanObj(createFailNIFAE(command, argument, args, parsedCommands, source, inputsIter))
            }
        }

        if (nextInput.isLeft)
            return leftBooleanObj(createFailAIPF(command, argument, args, parsedCommands, nextInput.left, inputsIter))

        val type = nextInput.right.type
        inputsIter.restore(prevState)

        return when {
            type is MapInputType -> parseMapArgument(inputsIter, source, command, argument, args, isNamed, parsedCommands)
            type is ListInputType -> parseListArgument(inputsIter, source, command, argument, args, isNamed, parsedCommands)
            argument.argumentType.inputType is AnyInputType ->
                parseSingleArgument(inputsIter, source, command, argument, args, isNamed, parsedCommands)
            else -> parseListArgument(inputsIter, source, command, argument, args, isNamed, parsedCommands)
        }
    }

    private fun parseListArgument(inputsIter: StatedIterator<Input>,
                                  source: String,
                                  command: Command,
                                  argument: Argument<*>,
                                  args: MutableList<ArgumentContainer<*>>,
                                  isNamed: Boolean,
                                  parsedCommands: List<CommandContainer>): EitherObjBoolean<ParseFail> {

        val prevState = inputsIter.pos
        val nextInput = inputsIter.next()
        inputsIter.restore(prevState)

        if (nextInput.isRight && nextInput.right.content.startsWith(LIST_OPEN)) {

            val input = inputsIter.next()

            if (input.isLeft)
                return leftBooleanObj(createFailAIPF(command,
                        argument,
                        args,
                        parsedCommands,
                        input.left,
                        inputsIter
                ))


            val parsed = argument.parse(input.right)

            if (parsed.isInvalid) {
                return if (!argument.isOptional || isNamed)
                    leftBooleanObj(createFailIIFAE(command,
                            input.right,
                            argument,
                            args,
                            parsed.validation,
                            parsedCommands,
                            inputsIter))
                else {
                    inputsIter.restore(prevState) // Restore old state
                    right(false)
                }
            }

            @Suppress("UNCHECKED_CAST")
            args += ArgumentContainer(argument,
                    input.right,
                    parsed.value,
                    argument.handler as ArgumentHandler<Any?>?
            )

            return right(true)
        }

        val initalState = inputsIter.pos
        val inputs = mutableListOf<Input>()

        var index = 0
        val type = argument.argumentType

        while (true) {
            if (!type.hasType(index))
                break

            val elementType = type.getListType(index)

            val state = inputsIter.pos
            val input = inputsIter.next()

            if (input.isLeft) {
                if (inputs.isEmpty() && !argument.isOptional) {
                    return leftBooleanObj(createFailAIPF(command,
                            argument,
                            args,
                            parsedCommands,
                            input.left,
                            inputsIter))
                } else {
                    inputsIter.restore(state)
                    break
                }
            }

            if (input.right.content.isArgumentName()) {
                inputsIter.restore(state)
                break
            }


            val parsed = elementType.parse(input.right)

            if (parsed.isValue) {
                inputs += input.right
            } else {
                inputsIter.restore(state)
                break
            }

            ++index
        }

        if (inputs.isEmpty()) {
            return if (!argument.isOptional || isNamed) {
                // String?
                val next = inputsIter.next()
                val peek_ = next.rightOrNull() ?: EmptyInput(source)

                leftBooleanObj(createFailIIFAE(command,
                        peek_,
                        argument,
                        args,
                        validation(validatedElement(peek_,
                                argument.argumentType,
                                ListFormatCheckParser,
                                ListInputType)),
                        parsedCommands,
                        inputsIter))
            } else {
                right(false)
            }
        }

        val start = inputs.firstOrNull()?.start ?: 0

        val end =
                if (inputs.isEmpty()) start
                else inputs.last().end

        val input = ListInput(inputs, source, start, end)

        val parsed = argument.parse(input)

        if (parsed.isInvalid) {
            return if (!argument.isOptional || isNamed)
                leftBooleanObj(createFailIIFAE(command,
                        input,
                        argument,
                        args,
                        parsed.validation,
                        parsedCommands,
                        inputsIter))
            else {
                inputsIter.restore(initalState)
                right(false)
            }
        }

        @Suppress("UNCHECKED_CAST")
        args += ArgumentContainer(argument,
                input,
                parsed.value,
                argument.handler as ArgumentHandler<Any?>?
        )

        return right(true)

    }

    private fun parseMapArgument(inputsIter: StatedIterator<Input>,
                                 source: String,
                                 command: Command,
                                 argument: Argument<*>,
                                 args: MutableList<ArgumentContainer<*>>,
                                 isNamed: Boolean,
                                 parsedCommands: List<CommandContainer>): EitherObjBoolean<ParseFail> {
        val state = inputsIter.pos
        val next = inputsIter.next()
                //nextOrNull()
                // ?: return leftBooleanObj(createFailNIFAE(command, argument, args, parsedCommands, source, inputsIter))
        inputsIter.restore(state)

        if (next.isLeft || !next.right.content.startsWith(MAP_OPEN)) {
            val peek_ = next.rightOrNull() ?: EmptyInput(source)

            return leftBooleanObj(createFailIIFAE(command,
                    peek_,
                    argument,
                    args,
                    validation(validatedElement(peek_,
                            argument.argumentType,
                            MapFormatCheckParser,
                            MapInputType)),
                    parsedCommands,
                    inputsIter))
        }

        val input = inputsIter.next()

        if (input.isLeft)
            return leftBooleanObj(
                    createFailAIPF(command,
                            argument,
                            args,
                            parsedCommands,
                            input.left,
                            inputsIter
                    ))

        val parsed = argument.parse(input.right)
        if (parsed.isInvalid) {
            if (!argument.isOptional || isNamed)
                return leftBooleanObj(createFailIIFAE(command,
                        input.right,
                        argument,
                        args,
                        parsed.validation,
                        parsedCommands,
                        inputsIter))
            else {
                inputsIter.restore(state) // Restore old state
                return right(false)
            }
        }

        @Suppress("UNCHECKED_CAST")
        args += ArgumentContainer(argument,
                input.right,
                parsed.value,
                argument.handler as ArgumentHandler<Any?>?
        )

        return right(true)
    }

    // Old code

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
                if (last.arguments.getArguments().all { it.isOptional }) {
                    list += last
                } else {
                    return null
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

/*
    private fun parse(commandsIterator: SourcedCharIterator,
                      containers: MutableList<CommandContainer>,
                      lastCommand: CommandHolder,
                      ownerProvider: OwnerProvider): Either<ParseFail, List<CommandContainer>> {
        if (!commandsIterator.hasNext())
            return left(createFailCNF(
                    EmptyInput(commandsIterator.sourceString),
                    containers
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
            if (last.arguments.getArguments().isNotEmpty())
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
        if (command.arguments.getArguments().isEmpty()) {
            return right(emptyList())
        }

        val parsing = ArgumentParsing(command.arguments)
        val args = parsing.argumentList

        var currentRequired = 0

        while (commandsIterator.hasNext() && parsing.hasNext()) {
            val state = commandsIterator.pos
            val get = commandsIterator.parseSingleInput(stringArgumentType)

            val input = get.rightOrNull() as? SingleInput ?: break

            val named = input.input.getArgumentNameOrNull()?.let { name ->
                parsing.getByName(name)
                        ?: return left(createFailANFE(command, args, name,
                        parsedCommands, commandsIterator))
            }

            val argument = if (named != null) {
                named
            } else {
                commandsIterator.restore(state)
                parsing.next()
            }

            val parse =
                    if (argument.argumentType.inputType !is SingleInputType)
                        parseVarargsArgument(commandsIterator, command, argument,
                                args, named != null, parsedCommands)
                    else
                        parseSingleArgument(commandsIterator, command, argument,
                                args, named != null, parsedCommands)

            if (parse.isRight && parse.right && !argument.isOptional) {
                ++currentRequired
            }

            if (parse.isRight && !parse.right && argument.isOptional && named == null) {
                @Suppress("UNCHECKED_CAST")
                args += ArgumentContainer(argument,
                        null,
                        argument.argumentType.defaultValue,
                        argument.handler as ArgumentHandler<Any?>?
                )
            }

            if (parse.isLeft) {
                if (argument.isOptional && named == null) {
                    @Suppress("UNCHECKED_CAST")
                    args += ArgumentContainer(argument,
                            null,
                            argument.argumentType.defaultValue,
                            argument.handler as ArgumentHandler<Any?>?
                    )
                    commandsIterator.restore(state)
                } else {
                    return left(parse.left)
                }
            }

            commandsIterator.jumpBlankSpace()
        }

        val commandArgumentsList = command.arguments.getArguments(args)
        val required = commandArgumentsList.count { !it.isOptional }

        if (required != 0) {
            return left(createFailAME(command, args, parsedCommands, commandsIterator))
        }

        commandArgumentsList.filter { a ->
            args.none { it.argument == a }
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


        return right(parsing.argumentList)
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
                                ListFormatCheckParser,
                                ListInputType)),
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
                            MapFormatCheckParser,
                            MapInputType)),
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
    }*/

    private fun createFailCNF(input: Input,
                              parsedCommands: List<CommandContainer>,
                              iter: StatedIterator<Input>): ParseFail =
            CommandNotFoundFail(input, parsedCommands, this.commandManager, iter)


    private fun createFailAME(command: Command,
                              args: List<ArgumentContainer<*>>,
                              parsedCommands: List<CommandContainer>,
                              source: String,
                              iter: StatedIterator<Input>): ParseFail =
            ArgumentsMissingFail(command, args, parsedCommands,
                    this.commandManager,
                    source, iter)

    private fun createFailIIFAE(command: Command,
                                input: Input,
                                argument: Argument<*>,
                                args: List<ArgumentContainer<*>>,
                                validation: Validation,
                                parsedCommands: List<CommandContainer>,
                                iter: StatedIterator<Input>): ParseFail =
            InvalidInputForArgumentFail(command, args, input, argument, validation, parsedCommands,
                    this.commandManager, iter)

    private fun createFailAIPF(command: Command,
                               argument: Argument<*>,
                               args: List<ArgumentContainer<*>>,
                               parsedCommands: List<CommandContainer>,
                               fail: InputParseFail,
                               iter: StatedIterator<Input>): ParseFail =
            ArgumentInputParseFail(command, args, argument, fail,
                    parsedCommands, this.commandManager, iter)

    private fun createFailNIFAE(command: Command,
                                argument: Argument<*>,
                                args: List<ArgumentContainer<*>>,
                                parsedCommands: List<CommandContainer>,
                                source: String,
                                iter: StatedIterator<Input>): ParseFail =
            NoInputForArgumentFail(command, args, argument, parsedCommands, this.commandManager, source, iter)

    private fun createFailANFE(command: Command,
                               args: List<ArgumentContainer<*>>,
                               name: Input,
                               parsedCommands: List<CommandContainer>,
                               iter: StatedIterator<Input>): ParseFail =
            ArgumentNotFoundFail(command, args, name, parsedCommands, this.commandManager, iter)


    class CommandHolder : IMutableBox<Command> by MutableBox()


    private class ParsingBackedList(val parsing: ArgumentParsing, val origin: MutableList<ArgumentContainer<*>>) :
            MutableList<ArgumentContainer<*>> by origin {
        override fun add(element: ArgumentContainer<*>): Boolean {
            val r = this.origin.add(element)
            this.parsing.reset()
            return r
        }

        override fun addAll(elements: Collection<ArgumentContainer<*>>): Boolean {
            val r = this.origin.addAll(elements)
            this.parsing.reset()
            return r
        }

        override fun add(index: Int, element: ArgumentContainer<*>) {
            this.origin.add(index, element)
            this.parsing.reset()
        }

        override fun addAll(index: Int, elements: Collection<ArgumentContainer<*>>): Boolean {
            val r = this.origin.addAll(index, elements)
            this.parsing.reset()
            return r
        }
    }

    private class ArgumentParsing(val arguments: Arguments) {
        private val argumentList_ = mutableListOf<ArgumentContainer<*>>()
        val argumentList = ParsingBackedList(this, argumentList_)

        private var pos = 0
        private var args: List<Argument<*>> = this.arguments.getArguments()

        fun hasNext(): Boolean {
            return args.isNotEmpty() && pos < args.size
        }

        fun next(): Argument<*> {
            val arg = args[pos]
            ++pos
            return arg
        }

        internal fun reset() {
            args = this.arguments.getArguments(this.argumentList_)
            pos = 0
        }

        fun addContainer(container: ArgumentContainer<*>): Boolean {
            val add = this.argumentList_.add(container)
            this.reset()
            return add
        }

        fun getByName(name: String): Argument<*>? =
                this.args.firstOrNull { it.name == name }

    }

}