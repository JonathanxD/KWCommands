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
import com.github.jonathanxd.iutils.annotation.Beta
import com.github.jonathanxd.iutils.box.IMutableBox
import com.github.jonathanxd.iutils.box.MutableBox
import com.github.jonathanxd.iutils.option.Options
import com.github.jonathanxd.jwiutils.kt.left
import com.github.jonathanxd.jwiutils.kt.leftBooleanObj
import com.github.jonathanxd.jwiutils.kt.right
import com.github.jonathanxd.kwcommands.ValidationTexts
import com.github.jonathanxd.kwcommands.argument.Argument
import com.github.jonathanxd.kwcommands.argument.ArgumentContainer
import com.github.jonathanxd.kwcommands.argument.ArgumentHandler
import com.github.jonathanxd.kwcommands.command.Command
import com.github.jonathanxd.kwcommands.command.CommandContainer
import com.github.jonathanxd.kwcommands.exception.*
import com.github.jonathanxd.kwcommands.fail.*
import com.github.jonathanxd.kwcommands.manager.CommandManager
import com.github.jonathanxd.kwcommands.util.*
import java.util.*

@Beta
class CommandParserImpl(override val commandManager: CommandManager) : CommandParser {

    override val options: Options = Options()

    override fun parseWithOwnerFunction(commandString: String,
                                        ownerProvider: OwnerProvider): Either<ParseFail, List<CommandContainer>> {
        val containers = mutableListOf<CommandContainer>()

        return parse(commandString.sourcedCharIterator(), containers, CommandHolder(), ownerProvider)
    }

    private fun parse(commandsIterator: SourcedCharIterator,
                      containers: MutableList<CommandContainer>,
                      lastCommand: CommandHolder,
                      ownerProvider: OwnerProvider): Either<ParseFail, List<CommandContainer>> {
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

    private fun SourcedCharIterator.peekSingle(/*fail: (input: Input?) -> ParseFail*/):
            Pair<SingleInput, SourcedCharIterator> =
            this.runInNew {
                this.parseSingleInput(parseData = false).right as SingleInput
            }

    private fun parseCommand(commandsIterator: SourcedCharIterator,
                             containers: MutableList<CommandContainer>,
                             lastCommand: CommandHolder,
                             ownerProvider: OwnerProvider,
                             required: Boolean): EitherObjBoolean<ParseFail> {
        val last = lastCommand.getOrElse(null)

        val peek = commandsIterator.peekSingle()
        val (input, iter) = peek

        if (input.content == "&") {
            commandsIterator.from(iter)

            if (last.arguments.isNotEmpty())
                return leftBooleanObj(createFailAME(last, emptyList(), containers, commandsIterator))

            containers += CommandContainer(last, emptyList(), last.handler)

            lastCommand.set(null)

            return right(true)
        }

        val command = getCommand(input.input, containers, lastCommand, ownerProvider(input.input))

        if (command != null) {
            commandsIterator.from(iter)
            lastCommand.set(command)
            return right(true)
        }

        if (required)
            return leftBooleanObj(createFailCNF(input, containers, lastCommand, commandsIterator))
        else
            return right(false)
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
                        ?: return left(createFailANFE(command, args, name, parsedCommands, commandsIterator))).also {
                    currentArgs.remove(it)
                }
            }

            val argument = named ?: nextArgument()

            val parse =
                    if (argument.isMultiple)
                        parseVarargsArgument(commandsIterator, command, argument,
                                args, named != null, parsedCommands)
                    else
                        parseSingleArgument(commandsIterator, command, argument,
                                args, named != null, parsedCommands)

            if (parse.isRight && !argument.isOptional)
                ++currentRequired

            if (parse.isLeft)
                return left(parse.left)
        }

        if (currentRequired != required) {
            return left(createFailAME(command, args, parsedCommands, commandsIterator))
        }

        return right(args)
    }

    private fun parseSingleArgument(commandsIterator: SourcedCharIterator,
                                    command: Command,
                                    argument: Argument<*>,
                                    args: MutableList<ArgumentContainer<*>>,
                                    isNamed: Boolean,
                                    parsedCommands: List<CommandContainer>): EitherObjBoolean<ParseFail> {
        val hasNext = commandsIterator.hasNext()
        val isBoolean = argument.isBoolean(args)
        val peek by lazy(LazyThreadSafetyMode.NONE) {
            commandsIterator.peekSingle()
        }
        val filled = (!hasNext || peek.first.input.isArgumentName()) && isBoolean

        val next = when {
            filled -> SingleInput("true", commandsIterator.sourceString, 0, 0)
            hasNext -> peek.first
            else -> return leftBooleanObj(createFailNIFAE(command, argument, args, parsedCommands, commandsIterator))
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
            return right(true)
        } else if (isNamed || !argument.isOptional)
            return leftBooleanObj(createFailIIFAE(command, next, argument, args, validation,
                    parsedCommands, commandsIterator))

        return right(false)
    }

    private fun Argument<*>.validate(args: List<ArgumentContainer<*>>, input: Input): Validation =
            this.validator(args, this, input)

    private fun parseVarargsArgument(commandsIterator: SourcedCharIterator,
                                     command: Command,
                                     argument: Argument<*>,
                                     args: MutableList<ArgumentContainer<*>>,
                                     isNamed: Boolean,
                                     parsedCommands: List<CommandContainer>): EitherObjBoolean<ParseFail> {

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
                return leftBooleanObj(createFailNIFAE(command, argument, args, parsedCommands, commandsIterator))

            }
        }

        val (next, _) = commandsIterator.peekSingle()

        return when {
            next.input.startsWith(MAP_OPEN) ->
                parseMapArgument(commandsIterator, command, argument, args, isNamed, parsedCommands)
            next.input.startsWith(LIST_OPEN) ->
                parseListArgument(commandsIterator, command, argument, args, isNamed, parsedCommands)
            else -> parseListArgument(commandsIterator, command, argument, args, isNamed, parsedCommands)
        }
    }

    private fun parseListArgument(commandsIterator: SourcedCharIterator,
                                  command: Command,
                                  argument: Argument<*>,
                                  args: MutableList<ArgumentContainer<*>>,
                                  isNamed: Boolean,
                                  parsedCommands: List<CommandContainer>): EitherObjBoolean<ParseFail> {
        if (commandsIterator.hasNext() && commandsIterator.peekSingle().first.input.startsWith(LIST_OPEN)) {

            val copy = commandsIterator.copy()

            val input = commandsIterator.parseListInput()

            if (input.isLeft)
                return leftBooleanObj(createFailIP(command,
                        argument,
                        args,
                        ListInputType,
                        parsedCommands,
                        input.left,
                        commandsIterator
                ))


            val validation = argument.validate(args, input.right)
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
                    // this also allows list to be parsed to other value for other arguments, example,
                    // for single input arguments, the first fragment (or the entire map) can be parsed
                    // as a single input string instead of a input list.
                    commandsIterator.from(copy) // Restore old state
                    return right(false)
                }
            }

            @Suppress("UNCHECKED_CAST")
            args += ArgumentContainer(argument,
                    input.right,
                    argument.transformer(args, argument, input.right),
                    argument.handler as ArgumentHandler<Any?>?
            )

            return right(true)
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
                return leftBooleanObj(createFailIIFAE(command,
                        peekInput,
                        argument,
                        args,
                        validation(validatedElement(peekInput, ListFormatCheckValidator,
                                ValidationTexts.expectedInputList(), listOf(ListInputType))),
                        parsedCommands,
                        commandsIterator))
            } else {
                return right(false)
            }
        }

        val end =
                if (inputs.isEmpty()) start
                else inputs.last().end

        val input = ListInput(inputs, commandsIterator.sourceString, start, end)

        val validation = argument.validate(args, input)

        if (validation.isInvalid) {
            if (!argument.isOptional || isNamed)
                return leftBooleanObj(createFailIIFAE(command,
                        input,
                        argument,
                        args,
                        validation,
                        parsedCommands,
                        commandsIterator))
            else
                return right(false)
        }

        @Suppress("UNCHECKED_CAST")
        args += ArgumentContainer(argument,
                input,
                argument.transformer(args, argument, input),
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
        val peek = commandsIterator.peekSingle()
        val (peekInput, _) = peek
        val copy = commandsIterator.copy()

        if (!peekInput.input.startsWith(MAP_OPEN)) {
            return leftBooleanObj(createFailIIFAE(command,
                    peekInput,
                    argument,
                    args,
                    validation(validatedElement(peekInput, MapFormatCheckValidator,
                            ValidationTexts.expectedInputMap(), listOf(MapInputType))),
                    parsedCommands,
                    commandsIterator))
        }

        val input = commandsIterator.parseMapInput()

        if (input.isLeft)
            return leftBooleanObj(
                    createFailIP(command,
                            argument,
                            args,
                            MapInputType,
                            parsedCommands,
                            input.left,
                            commandsIterator
                    ))

        val validation = argument.validate(args, input.right)
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
                // this also allows map to be parsed to other value for other arguments, example,
                // for single input arguments, the first fragment (or the entire map) can be parsed
                // as a single input string instead of a input map.
                commandsIterator.from(copy) // Restore old state
                return right(false)
            }
        }

        @Suppress("UNCHECKED_CAST")
        args += ArgumentContainer(argument,
                input.right,
                argument.transformer(args, argument, input.right),
                argument.handler as ArgumentHandler<Any?>?
        )

        return right(true)
    }

    private fun createFailCNF(input: Input,
                              parsedCommands: List<CommandContainer>,
                              lastCommand: CommandHolder,
                              iter: SourcedCharIterator): ParseFail =
            if (this.options[KWParserOptions.DEBUG_ENABLE_EXCEPTIONS]) {
                failCNF_(input, parsedCommands, lastCommand)
            } else CommandNotFoundFail(input, parsedCommands, this.commandManager, iter)


    private fun createFailAME(command: Command,
                              args: List<ArgumentContainer<*>>,
                              parsedCommands: List<CommandContainer>,
                              iter: SourcedCharIterator): ParseFail =
            if (this.options[KWParserOptions.DEBUG_ENABLE_EXCEPTIONS]) {
                val missing = command.arguments.filterNot { a -> args.any { it.argument == a } }
                val current = command.arguments.filter { a -> args.any { it.argument == a } }
                failAME_(command, args, missing, current, parsedCommands)
            } else ArgumentsMissingFail(command, args, parsedCommands,
                    this.commandManager,
                    iter)

    private fun createFailIP(command: Command,
                             argument: Argument<*>,
                             args: List<ArgumentContainer<*>>,
                             inputType: InputType,
                             parsedCommands: List<CommandContainer>,
                             fail: InputParseFail,
                             iter: SourcedCharIterator): ParseFail =
            if (this.options[KWParserOptions.DEBUG_ENABLE_EXCEPTIONS]) {
                failEI_(command, inputType, args, argument, parsedCommands)
            } else CommandInputParseFail(command, args, argument, inputType, fail, parsedCommands,
                    this.commandManager, iter)

    private fun createFailIIFAE(command: Command,
                                input: Input,
                                argument: Argument<*>,
                                args: List<ArgumentContainer<*>>,
                                validation: Validation,
                                parsedCommands: List<CommandContainer>,
                                iter: SourcedCharIterator): ParseFail =
            if (this.options[KWParserOptions.DEBUG_ENABLE_EXCEPTIONS]) {
                failIIFAE_(command, input, argument, args, validation, parsedCommands)
            } else InvalidInputForArgumentFail(command, args, input, argument, validation, parsedCommands,
                    this.commandManager, iter)

    private fun createFailNIFAE(command: Command,
                                argument: Argument<*>,
                                args: List<ArgumentContainer<*>>,
                                parsedCommands: List<CommandContainer>,
                                iter: SourcedCharIterator): ParseFail =
            if (this.options[KWParserOptions.DEBUG_ENABLE_EXCEPTIONS]) {
                failNIFAE_(command, argument, args, parsedCommands)
            } else NoInputForArgumentFail(command, args, argument, parsedCommands, this.commandManager, iter)

    private fun createFailANFE(command: Command,
                               args: List<ArgumentContainer<*>>,
                               name: String,
                               parsedCommands: List<CommandContainer>,
                               iter: SourcedCharIterator): ParseFail =
            if (this.options[KWParserOptions.DEBUG_ENABLE_EXCEPTIONS]) {
                val currentArgs = command.arguments.filterNot { a -> args.any { it.argument == a } }
                failANFE_(command, currentArgs, args, name, parsedCommands)
            } else ArgumentNotFoundFail(command, args, name, parsedCommands, this.commandManager, iter)


    private fun failCNF_(input: Input,
                         parsedCommands: List<CommandContainer>,
                         lastCommand: CommandHolder): Nothing =
            throw CommandNotFoundException(input, parsedCommands,
                    this.commandManager,
                    "Command '$input' not found${if (lastCommand.isPresent) " neither in parent command " +
                            lastCommand.value.toStr() +
                            " nor in command manager" else " in command manager"}.")

    private fun failAME_(command: Command,
                         args: List<ArgumentContainer<*>>,
                         missing: List<Argument<*>>,
                         provided: List<Argument<*>>,
                         parsedCommands: List<CommandContainer>): Nothing =
            throw ArgumentsMissingException(command,
                    args,
                    parsedCommands,
                    this.commandManager,
                    "Missing arguments ${missing.toStr()} of command ${command.toStr()}. Provided: ${provided.toStr()}.")

    private fun failIIFAE_(command: Command,
                           input: Input,
                           argument: Argument<*>,
                           args: List<ArgumentContainer<*>>,
                           validation: Validation,
                           parsedCommands: List<CommandContainer>): Nothing =
            throw InvalidInputForArgumentException(command, args, input, argument, validation, parsedCommands,
                    this.commandManager,
                    "Invalid input '$input' for argument '${argument.toStr()}' of command '${command.toStr()}'.")

    private fun failNIFAE_(command: Command,
                           argument: Argument<*>,
                           args: List<ArgumentContainer<*>>,
                           parsedCommands: List<CommandContainer>): Nothing =
            throw NoInputForArgumentException(command, args, argument, parsedCommands, this.commandManager,
                    "No input provided for argument '${argument.toStr()}' of command '${command.toStr()}'.")

    private fun failANFE_(command: Command,
                          currentArgs: List<Argument<*>>,
                          args: List<ArgumentContainer<*>>,
                          name: String,
                          parsedCommands: List<CommandContainer>): Nothing =
            throw ArgumentNotFoundException(command, args, name, parsedCommands, this.commandManager,
                    "Cannot find argument with name '$name' in arguments '${currentArgs.toStr()}'" +
                            " of command '${command.toStr()}'")

    private fun failEI_(command: Command,
                        inputType: InputType,
                        args: List<ArgumentContainer<*>>,
                        argument: Argument<*>,
                        parsedCommands: List<CommandContainer>): Nothing =
            throw ExpectedInputException(command, args, argument, inputType, parsedCommands, this.commandManager,
                    "Invalid input for input type '$inputType' for argument ${argument.nameOrId} of command '${command.toStr()}'.")

    class CommandHolder : IMutableBox<Command> by MutableBox()

}