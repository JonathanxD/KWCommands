/*
 *      KWCommands - New generation of WCommands written in Kotlin <https://github.com/JonathanxD/KWCommands>
 *
 *         The MIT License (MIT)
 *
 *      Copyright (c) 2018 JonathanxD
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
import com.github.jonathanxd.iutils.kt.left
import com.github.jonathanxd.iutils.kt.leftBooleanObj
import com.github.jonathanxd.iutils.kt.right
import com.github.jonathanxd.iutils.option.Options
import com.github.jonathanxd.iutils.text.localizer.Localizer
import com.github.jonathanxd.kwcommands.argument.*
import com.github.jonathanxd.kwcommands.command.Command
import com.github.jonathanxd.kwcommands.command.CommandContainer
import com.github.jonathanxd.kwcommands.fail.*
import com.github.jonathanxd.kwcommands.manager.CommandManager
import com.github.jonathanxd.kwcommands.util.*

class CommandParserImpl(override val commandManager: CommandManager) : CommandParser {

    override val options: Options = Options()

    override fun parseWithOwnerFunction(
            commandString: String,
            ownerProvider: OwnerProvider,
            localizer: Localizer?
    ): Either<ParseFail, List<CommandContainer>> =
            this.parseWithOwnerFunction(commandString.sourcedCharIterator(), ownerProvider, localizer)

    override fun parseWithOwnerFunction(
            commandIter: SourcedCharIterator,
            ownerProvider: OwnerProvider,
            localizer: Localizer?
    ): Either<ParseFail, List<CommandContainer>> {
        val inputs = this.parseInputs(commandIter)

        return this.parse(
                commandIter.sourceString,
                inputs,
                commandIter,
                mutableListOf(),
                CommandHolder(),
                ownerProvider,
                localizer
        )
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

    private fun parse(
            sourceString: String,
            inputs: List<Either<InputParseFail, Input>>,
            iter: SourcedCharIterator,
            containers: MutableList<CommandContainer>,
            lastCommand: CommandHolder,
            ownerProvider: OwnerProvider,
            localizer: Localizer?
    ): Either<ParseFail, List<CommandContainer>> {
        val inputIter: StatedIterator<Input> = ListBackedStatedIterator(inputs, iter)

        if (inputs.isEmpty())
            return left(
                    createFailCNF(
                            EmptyInput(sourceString),
                            containers,
                            inputIter
                    )
            )

        while (inputIter.hasNext()) {
            val command =
                    parseCommand(
                            inputIter,
                            containers,
                            lastCommand,
                            ownerProvider,
                            localizer,
                            !lastCommand.isPresent
                    )

            if (command.isLeft)
                return left(command.left)

            if (!command.right || !inputIter.hasNext()) {
                val last = lastCommand.value

                if (last != null) {
                    lastCommand.set(null)

                    val args = parseArguments(inputIter, sourceString, last, containers, localizer)

                    if (args.isLeft)
                        return left(args.left)

                    containers += CommandContainer(last, args.right, last.handler)
                }
            }
        }

        return right(containers)

    }

    private fun parseCommand(
            inputIter: StatedIterator<Input>,
            containers: MutableList<CommandContainer>,
            lastCommand: CommandHolder,
            ownerProvider: OwnerProvider,
            localizer: Localizer?,
            required: Boolean
    ): EitherObjBoolean<ParseFail> {
        val state = inputIter.pos
        val input = inputIter.next()
        val last = lastCommand.getOrElse(null)

        val commandInput = input.rightOrNull() as? SingleInput ?: return right(false)

        if (commandInput.content == "&") {
            val remain = last.arguments.getRemainingArguments()
            if (remain.isNotEmpty())
                return leftBooleanObj(
                        createFailAME(
                                last,
                                emptyList(),
                                remain,
                                containers,
                                commandInput.source,
                                inputIter
                        )
                )

            containers += CommandContainer(last, emptyList(), last.handler)

            lastCommand.set(null)

            return right(true)
        }

        val command = getCommand(
                commandInput.input,
                containers,
                lastCommand,
                ownerProvider(commandInput.input),
                localizer
        )

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


    private fun parseArguments(
            inputsIter: StatedIterator<Input>,
            source: String,
            command: Command,
            parsedCommands: List<CommandContainer>,
            localizer: Localizer?
    ): Either<ParseFail, List<ArgumentContainer<*>>> {
        if (command.arguments.getRemainingArguments().isEmpty()) {
            return right(emptyList())
        }

        val parsing = ArgumentParsing(command.arguments, localizer)
        val args = parsing.argumentList

        var currentRequired = 0

        while (inputsIter.hasNext() && parsing.hasNext()) {
            val state = inputsIter.pos
            val get = inputsIter.next()

            val input = (get.rightOrNull() as? SingleInput)?.input
            val shortName = input?.getShortNames()
            val name = input?.getArgumentNameOrNull()
            val isAssignment = if (name != null) input.isAssignmentArg() else false

            val arguments = if (shortName?.isNotEmpty() == true) {
                if (shortName.size == 1) {
                    listOf(
                            parsing.getByShortName(shortName[0])
                                    ?: return left(
                                            createFailASNNFE(
                                                    command,
                                                    args,
                                                    get.right,
                                                    parsedCommands,
                                                    inputsIter
                                            )
                                    )
                    )
                } else {
                    shortName.map {
                        parsing.getByShortName(it) ?: return left(
                                createFailASNNFE(
                                        command,
                                        args,
                                        get.right,
                                        parsedCommands,
                                        inputsIter
                                )
                        )
                    }
                }
            } else if (name != null) {
                listOf(
                        parsing.getByName(name)
                                ?: return left(
                                        createFailANFE(
                                                command,
                                                args,
                                                get.right,
                                                parsedCommands,
                                                inputsIter
                                        )
                                )
                )
            } else {
                inputsIter.restore(state)
                listOf(parsing.next())
            }

            if (isAssignment) {
                inputsIter.restore(state)
            }

            val lastArgument = arguments.last()

            arguments.forEach {
                if (!lastArgument.argumentType.inputType.isCompatible(it.argumentType.inputType))
                    return left(
                            createFailIITFSAF(
                                    command,
                                    args,
                                    lastArgument,
                                    it,
                                    get.right,
                                    parsedCommands,
                                    inputsIter
                            )
                    )
            }

            val named = name != null
            val shortNamed = shortName != null

            val parse =
                    if (lastArgument.argumentType.inputType !== SingleInputType)
                        parseVarargsArgument(
                                inputsIter, source, command, lastArgument, arguments,
                                args, named, shortNamed, isAssignment, parsedCommands
                        )
                    else
                        parseSingleArgument(
                                inputsIter, source, command, lastArgument, arguments,
                                args, named, shortNamed, isAssignment, parsedCommands
                        )

            val optional = arguments.all { it.isOptional }

            if (parse.isRight && parse.right && !optional) {
                currentRequired += arguments.count()
            }

            if (parse.isRight && !parse.right && optional && name == null) {

                arguments.forEach { argument ->
                    @Suppress("UNCHECKED_CAST")
                    args += ArgumentContainer(
                            argument,
                            null,
                            argument.argumentType.defaultValue,
                            argument.handler as ArgumentHandler<Any?>?
                    )
                }
            }

            if (parse.isLeft) {
                arguments.forEach { argument ->
                    if (argument.isOptional && name == null) {
                        @Suppress("UNCHECKED_CAST")
                        args += ArgumentContainer(
                                argument,
                                null,
                                argument.argumentType.defaultValue,
                                argument.handler as ArgumentHandler<Any?>?
                        )
                    } else {
                        return left(parse.left)
                    }
                }

                inputsIter.restore(state)
            }
        }

        val commandArgumentsList = command.arguments.getRemainingArguments(args)
        val required = commandArgumentsList.count { !it.isOptional }

        if (required != 0) {
            return left(
                    createFailAME(
                            command,
                            args,
                            commandArgumentsList,
                            parsedCommands,
                            source,
                            inputsIter
                    )
            )
        }

        commandArgumentsList.filter { a ->
            args.none { it.argument == a }
                    && a.isOptional
                    && a.argumentType.defaultValue != null
        }.forEach {
            @Suppress("UNCHECKED_CAST")
            args += ArgumentContainer(
                    it,
                    null,
                    it.argumentType.defaultValue,
                    it.handler as ArgumentHandler<Any?>?
            )
        }


        return right(parsing.argumentList)
    }

    private fun parseSingleArgument(
            inputsIter: StatedIterator<Input>,
            source: String,
            command: Command,
            argument: Argument<*>,
            all: List<Argument<*>>,
            args: MutableList<ArgumentContainer<*>>,
            isNamed: Boolean,
            isShort: Boolean,
            isAssignment: Boolean,
            parsedCommands: List<CommandContainer>
    ): EitherObjBoolean<ParseFail> {
        val isBoolean = argument.isBoolean()
        val state = inputsIter.pos
        val peek = inputsIter.next()
        val right = peek.rightOrNull() as? SingleInput

        val fill = (right == null || right.input.isName()) && isBoolean

        val next = when {
            fill -> SingleInput("true", source, 0, 0)
            peek.isRight -> peek.right
            peek.isLeft && peek.left is NoMoreElementsInputParseFail ->
                return leftBooleanObj(
                        createFailNIFAE(
                                command, argument, args,
                                parsedCommands, source, inputsIter
                        )
                )
            peek.isLeft -> return leftBooleanObj(
                    createFailAIPF(
                            command, argument, args,
                            parsedCommands, peek.left, inputsIter
                    )
            )
            else -> return leftBooleanObj(
                    createFailNIFAE(
                            command, argument, args,
                            parsedCommands, source, inputsIter
                    )
            )
        }

        val parseInput = argument.argumentType.parse(
                next.applyModification(isAssignment)
                        ?: return EitherObjBoolean.left(createFailNIFAE(
                                command, argument,
                                args, parsedCommands, source,
                                inputsIter
                        ))
        )

        val parsed =
                if (!fill && parseInput.isInvalid && isShort && isBoolean) {
                    inputsIter.restore(state)
                    argument.argumentType.parse(SingleInput("true", source, 0, 0))
                } else {
                    parseInput
                }

        if (parsed.isValue) {
            all.forEach {
                @Suppress("UNCHECKED_CAST")
                args += ArgumentContainer(
                        it,
                        next,
                        parsed.value,
                        it.handler as ArgumentHandler<Any?>?
                )
            }
            return right(true)
        } else {
            if (!fill) inputsIter.restore(state)
            if (isNamed || !argument.isOptional) {
                return leftBooleanObj(
                        createFailIIFAE(
                                command, next, argument, args, parsed.validation,
                                parsedCommands, inputsIter
                        )
                )
            }
        }

        return right(false)
    }

    private fun Input.applyModification(isAssignment: Boolean): Input? =
            if (!isAssignment) this
            else when (this) {
                is SingleInput -> this.extractAssignmentValue()
                else -> this
            }

    private fun parseVarargsArgument(
            inputsIter: StatedIterator<Input>,
            source: String,
            command: Command,
            argument: Argument<*>,
            all: List<Argument<*>>,
            args: MutableList<ArgumentContainer<*>>,
            isNamed: Boolean,
            isShort: Boolean,
            isAssignment: Boolean,
            parsedCommands: List<CommandContainer>
    ): EitherObjBoolean<ParseFail> {

        val prevState = inputsIter.pos
        val nextInput = inputsIter.nextOrNull() ?: return if (argument.isOptional) {
            val input = EmptyInput(source)
            @Suppress("UNCHECKED_CAST")
            args += ArgumentContainer(
                    argument,
                    input,
                    argument.argumentType.defaultValue,
                    argument.handler as ArgumentHandler<Any?>?
            )
            right(true)
        } else {
            leftBooleanObj(
                    createFailNIFAE(
                            command,
                            argument,
                            args,
                            parsedCommands,
                            source,
                            inputsIter
                    )
            )
        }

        if (nextInput.isLeft)
            return leftBooleanObj(
                    createFailAIPF(
                            command,
                            argument,
                            args,
                            parsedCommands,
                            nextInput.left,
                            inputsIter
                    )
            )

        val type = nextInput.right.type
        inputsIter.restore(prevState)

        return when {
            type is MapInputType -> parseMapArgument(
                    inputsIter,
                    source,
                    command,
                    argument,
                    args,
                    isNamed,
                    parsedCommands
            )
            type is ListInputType -> parseListArgument(
                    inputsIter,
                    source,
                    command,
                    argument,
                    args,
                    isNamed,
                    parsedCommands
            )
            argument.argumentType.inputType === AnyInputType ->
                parseSingleArgument(
                        inputsIter,
                        source,
                        command,
                        argument,
                        all,
                        args,
                        isNamed,
                        isShort,
                        isAssignment,
                        parsedCommands
                )
            else -> parseListArgument(
                    inputsIter,
                    source,
                    command,
                    argument,
                    args,
                    isNamed,
                    parsedCommands
            )
        }
    }

    private fun parseListArgument(
            inputsIter: StatedIterator<Input>,
            source: String,
            command: Command,
            argument: Argument<*>,
            args: MutableList<ArgumentContainer<*>>,
            isNamed: Boolean,
            parsedCommands: List<CommandContainer>
    ): EitherObjBoolean<ParseFail> {

        val prevState = inputsIter.pos
        val nextInput = inputsIter.next()
        inputsIter.restore(prevState)

        if (nextInput.isRight && nextInput.right.content.startsWith(LIST_OPEN)) {

            val input = inputsIter.next()

            if (input.isLeft)
                return leftBooleanObj(
                        createFailAIPF(
                                command,
                                argument,
                                args,
                                parsedCommands,
                                input.left,
                                inputsIter
                        )
                )


            val parsed = argument.parse(input.right)

            if (parsed.isInvalid) {
                return if (!argument.isOptional || isNamed)
                    leftBooleanObj(
                            createFailIIFAE(
                                    command,
                                    input.right,
                                    argument,
                                    args,
                                    parsed.validation,
                                    parsedCommands,
                                    inputsIter
                            )
                    )
                else {
                    inputsIter.restore(prevState) // Restore old state
                    right(false)
                }
            }

            @Suppress("UNCHECKED_CAST")
            args += ArgumentContainer(
                    argument,
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
                    return leftBooleanObj(
                            createFailAIPF(
                                    command,
                                    argument,
                                    args,
                                    parsedCommands,
                                    input.left,
                                    inputsIter
                            )
                    )
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

                leftBooleanObj(
                        createFailIIFAE(
                                command,
                                peek_,
                                argument,
                                args,
                                validation(
                                        invalidElement(
                                                peek_,
                                                argument.argumentType,
                                                ListFormatCheckParser
                                        )
                                ),
                                parsedCommands,
                                inputsIter
                        )
                )
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
                leftBooleanObj(
                        createFailIIFAE(
                                command,
                                input,
                                argument,
                                args,
                                parsed.validation,
                                parsedCommands,
                                inputsIter
                        )
                )
            else {
                inputsIter.restore(initalState)
                right(false)
            }
        }

        @Suppress("UNCHECKED_CAST")
        args += ArgumentContainer(
                argument,
                input,
                parsed.value,
                argument.handler as ArgumentHandler<Any?>?
        )

        return right(true)

    }

    private fun parseMapArgument(
            inputsIter: StatedIterator<Input>,
            source: String,
            command: Command,
            argument: Argument<*>,
            args: MutableList<ArgumentContainer<*>>,
            isNamed: Boolean,
            parsedCommands: List<CommandContainer>
    ): EitherObjBoolean<ParseFail> {
        val state = inputsIter.pos
        val next = inputsIter.next()
        //nextOrNull()
        // ?: return leftBooleanObj(createFailNIFAE(command, argument, args, parsedCommands, source, inputsIter))
        inputsIter.restore(state)

        if (next.isLeft || !next.right.content.startsWith(MAP_OPEN)) {
            val peek_ = next.rightOrNull() ?: EmptyInput(source)

            return leftBooleanObj(
                    createFailIIFAE(
                            command,
                            peek_,
                            argument,
                            args,
                            validation(
                                    invalidElement(
                                            peek_,
                                            argument.argumentType,
                                            MapFormatCheckParser
                                    )
                            ),
                            parsedCommands,
                            inputsIter
                    )
            )
        }

        val input = inputsIter.next()

        if (input.isLeft)
            return leftBooleanObj(
                    createFailAIPF(
                            command,
                            argument,
                            args,
                            parsedCommands,
                            input.left,
                            inputsIter
                    )
            )

        val parsed = argument.parse(input.right)
        if (parsed.isInvalid) {
            if (!argument.isOptional || isNamed)
                return leftBooleanObj(
                        createFailIIFAE(
                                command,
                                input.right,
                                argument,
                                args,
                                parsed.validation,
                                parsedCommands,
                                inputsIter
                        )
                )
            else {
                inputsIter.restore(state) // Restore old state
                return right(false)
            }
        }

        @Suppress("UNCHECKED_CAST")
        args += ArgumentContainer(
                argument,
                input.right,
                parsed.value,
                argument.handler as ArgumentHandler<Any?>?
        )

        return right(true)
    }

    private fun getCommand(
            name: String,
            containers: MutableList<CommandContainer>,
            lastCommand: CommandHolder,
            owner: Any?,
            localizer: Localizer?
    ): Command? {
        val list = mutableListOf<Command>()

        var last: Command? = lastCommand.getOrElse(null)

        while (last != null) {
            val cmd = this.commandManager.getSubCommand(last, name, localizer)

            if (cmd != null) {
                lastCommand.set(cmd)
                list.forEach {
                    containers += CommandContainer(it, emptyList(), it.handler)
                }
                return cmd
            } else {
                if (last.arguments.getRemainingArguments().all { it.isOptional }) {
                    list += last
                } else {
                    return null
                }
                last = last.parent
            }
        }

        return this.commandManager.getCommand(name, owner, localizer)?.also {
            lastCommand.set(it)
        }.also {
            if (it != null) {
                list.forEach {
                    containers += CommandContainer(it, emptyList(), it.handler)
                }
            }
        }
    }

    private fun createFailCNF(
            input: Input,
            parsedCommands: List<CommandContainer>,
            iter: StatedIterator<Input>
    ): ParseFail =
            CommandNotFoundFail(input, parsedCommands, this.commandManager, iter)


    private fun createFailAME(
            command: Command,
            args: List<ArgumentContainer<*>>,
            arguments: List<Argument<*>>,
            parsedCommands: List<CommandContainer>,
            source: String,
            iter: StatedIterator<Input>
    ): ParseFail =
            ArgumentsMissingFail(
                    command, args, arguments, parsedCommands,
                    this.commandManager,
                    source, iter
            )

    private fun createFailIIFAE(
            command: Command,
            input: Input,
            argument: Argument<*>,
            args: List<ArgumentContainer<*>>,
            validation: Validation,
            parsedCommands: List<CommandContainer>,
            iter: StatedIterator<Input>
    ): ParseFail =
            InvalidInputForArgumentFail(
                    command, args, input, argument, validation, parsedCommands,
                    this.commandManager, iter
            )

    private fun createFailAIPF(
            command: Command,
            argument: Argument<*>,
            args: List<ArgumentContainer<*>>,
            parsedCommands: List<CommandContainer>,
            fail: InputParseFail,
            iter: StatedIterator<Input>
    ): ParseFail =
            ArgumentInputParseFail(
                    command, args, argument, fail,
                    parsedCommands, this.commandManager, iter
            )

    private fun createFailNIFAE(
            command: Command,
            argument: Argument<*>,
            args: List<ArgumentContainer<*>>,
            parsedCommands: List<CommandContainer>,
            source: String,
            iter: StatedIterator<Input>
    ): ParseFail =
            NoInputForArgumentFail(
                    command,
                    args,
                    argument,
                    parsedCommands,
                    this.commandManager,
                    source,
                    iter
            )

    private fun createFailIITFSAF(
            command: Command,
            parsedArgs: List<ArgumentContainer<*>>,
            expectedArg: Argument<*>,
            incompatibleArg: Argument<*>,
            input: Input,
            parsedCommands: List<CommandContainer>,
            iter: StatedIterator<Input>
    ): ParseFail =
            IncompatibleInputTypesForShortArgumentsFail(
                    command,
                    parsedArgs,
                    expectedArg,
                    incompatibleArg,
                    input,
                    parsedCommands,
                    this.commandManager,
                    iter
            )

    private fun createFailANFE(
            command: Command,
            args: List<ArgumentContainer<*>>,
            name: Input,
            parsedCommands: List<CommandContainer>,
            iter: StatedIterator<Input>
    ): ParseFail =
            ArgumentNotFoundFail(command, args, name, parsedCommands, this.commandManager, iter)

    private fun createFailASNNFE(
            command: Command,
            args: List<ArgumentContainer<*>>,
            name: Input,
            parsedCommands: List<CommandContainer>,
            iter: StatedIterator<Input>
    ): ParseFail =
            ArgumentShortNamesNotFoundFail(
                    command,
                    args,
                    name,
                    parsedCommands,
                    this.commandManager,
                    iter
            )


    class CommandHolder : IMutableBox<Command> by MutableBox()


    private class ParsingBackedList(
            val parsing: ArgumentParsing,
            val origin: MutableList<ArgumentContainer<*>>
    ) :
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

    private class ArgumentParsing(val arguments: Arguments, val localizer: Localizer?) {
        private val argumentList_ = mutableListOf<ArgumentContainer<*>>()
        val argumentList = ParsingBackedList(this, argumentList_)

        private var pos = 0
        private var args: List<Argument<*>> = this.arguments.getRemainingArguments()

        fun hasNext(): Boolean {
            return args.isNotEmpty() && pos < args.size
        }

        fun next(): Argument<*> {
            val arg = args[pos]
            ++pos
            return arg
        }

        internal fun reset() {
            args = this.arguments.getRemainingArguments(this.argumentList_)
            pos = 0
        }

        fun addContainer(container: ArgumentContainer<*>): Boolean {
            val add = this.argumentList_.add(container)
            this.reset()
            return add
        }

        fun getByName(name: String): Argument<*>? =
                this.args.firstWithName(name, this.localizer)


        fun getByShortName(name: Char): Argument<*>? =
                this.args.firstNameMatches(this.localizer) { it[0] == name }

    }

}