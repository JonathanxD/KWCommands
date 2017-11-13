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

import com.github.jonathanxd.iutils.collection.wrapper.WrapperCollections
import com.github.jonathanxd.iutils.option.Options
import com.github.jonathanxd.jwiutils.kt.none
import com.github.jonathanxd.kwcommands.argument.Argument
import com.github.jonathanxd.kwcommands.argument.ArgumentContainer
import com.github.jonathanxd.kwcommands.argument.ArgumentHandler
import com.github.jonathanxd.kwcommands.command.Command
import com.github.jonathanxd.kwcommands.command.CommandContainer
import com.github.jonathanxd.kwcommands.exception.*
import com.github.jonathanxd.kwcommands.manager.CommandManager
import com.github.jonathanxd.kwcommands.processor.KWParserOptions
import com.github.jonathanxd.kwcommands.util.escape
import com.github.jonathanxd.kwcommands.util.isBoolean
import com.github.jonathanxd.kwcommands.util.nameOrId
import com.github.jonathanxd.kwcommands.util.nameOrIdWithType
import java.util.*

class CommandParserImpl(override val commandManager: CommandManager) : CommandParser {
    override val options: Options = Options()

    override fun parseWithOwnerFunction(stringList: List<String>,
                                        ownerProvider: (commandName: String) -> Any?): List<CommandContainer> {
        val orderOption = this.options[KWParserOptions.ORDER]
        val commands = mutableListOf<CommandContainer>()
        val deque: Deque<Command> = LinkedList()
        val getStr: (Int) -> String = { stringList[it].escape('\\') }
        val getRawStr: (Int) -> String = { stringList[it] }

        // Checks if is argument name
        val isName: (Int) -> Boolean = { stringList[it].startsWith("--") }
        val getName: (Int) -> String = { stringList[it].substring(2).escape('\\') }

        var index = 0 // Current index in string list

        while (index < stringList.size) {
            var command: Command? = null

            if (stringList[index].isAndOp()) {
                deque.clear()
                ++index
                continue
            }

            while (command == null) {

                if (deque.isEmpty()) {
                    command = commandManager.getCommand(getStr(index), ownerProvider(getStr(index)))

                    if (command == null)
                        throw CommandNotFoundException(getStr(index),
                                commands.toList(),
                                this.commandManager,
                                "Command ${getStr(index)} (index $index in $stringList) was not found.")
                } else {
                    command = this.commandManager.getSubCommand(deque.last, getStr(index))

                    if (command == null) {
                        val rm = deque.removeLast()

                        if (rm.parent != null)
                            deque.offerLast(rm.parent)
                    }
                }
            }

            if (index + 1 == stringList.size ||
                    (index + 1 < stringList.size
                            && (stringList[index + 1].isAndOp()
                            || this.commandManager.getSubCommand(command, getStr(index + 1)) == null))) {

                val order = orderOption || command.arguments.any { it.isMultiple }
                val arguments = command.arguments.toMutableList()
                val args = mutableListOf<ArgumentContainer<*>>()
                val immutableArgs = WrapperCollections.immutableList(args)

                if (!arguments.isEmpty()) {
                    val requiredArgsCount = arguments.count { !it.isOptional }

                    if (index + requiredArgsCount >= stringList.size) {

                        val required = arguments.filter { !it.isOptional }.joinToString { it.id.toString() }
                        val provided = if (index + 1 < stringList.size)
                            ", Provided arguments: ${stringList.subList(index + 1, stringList.size).joinToString()}"
                        else ""

                        throw ArgumentsMissingException(command,
                                args.toList(),
                                this.commandManager,
                                "Some required arguments of command $command is missing (Required arguments ids: $required$provided).")
                    }

                    var requiredCount = 0

                    val maxNames = arguments.size
                    var names = 0

                    (index + 1 until stringList.size).forEach {
                        if (isName(it) && names + 1 < maxNames)
                            ++names
                    }

                    val size = stringList.size - 1
                    var argPos = 0
                    var searchPos = index + 1 // Index to search argument value or name

                    while (searchPos <= size) {
                        val it = searchPos
                        val argStr = getStr(it)
                        var argInput = SingleInput(argStr)

                        fun parseVarargs(arg: Argument<*>): ArgumentContainer<*> {
                            var container: ArgumentContainer<*>? = null
                            val inputs = mutableListOf<Input>()
                            var values: Collection<Any?>? = null
                            var lindex = if (isName(it)) it + 1 else it // Offset add to argument value index

                            val cArgs = args.toMutableList()
                            var input = getStr(lindex)

                            while (!isName(lindex) && arg.validator(immutableArgs, arg, SingleInput(input))) {
                                val inputObj = SingleInput(input)
                                inputs += inputObj
                                ++searchPos
                                ++index
                                ++lindex

                                container?.let { cArgs.add(it) }

                                @Suppress("UNCHECKED_CAST")
                                val converted = arg.transformer(cArgs, arg, inputObj)
                                        as MutableCollection<Any?>

                                if (values == null) values = converted
                                else (values as MutableCollection<Any?>).addAll(converted)

                                container?.let { cArgs.removeAt(cArgs.lastIndex) }

                                @Suppress("UNCHECKED_CAST")
                                container = ArgumentContainer(
                                        arg,
                                        inputObj,
                                        values,
                                        arg.handler as? ArgumentHandler<Any?>
                                )

                                if (lindex <= size) {
                                    input = getStr(lindex)
                                } else
                                    break
                            }

                            if (inputs.isEmpty()) {
                                @Suppress("UNCHECKED_CAST")
                                values = arg.transformer(cArgs, arg, EmptyInput)
                                        as Collection<Any?>
                            }

                            val filtered = inputs.filterIsInstance<SingleInput>()

                            val inp = ListInput(filtered.map { SingleInput(it.input) })

                            @Suppress("UNCHECKED_CAST")
                            val argContainer = ArgumentContainer(
                                    arg,
                                    inp,
                                    values ?: emptyList<Any?>(),
                                    arg.handler as? ArgumentHandler<Any?>
                            )

                            if (!arg.isOptional)
                                requiredCount++

                            arguments.remove(arg)
                            args.add(argContainer)

                            return argContainer
                        }

                        var arg: Argument<*>? = null

                        if (isName(it)) {
                            val argName = getName(it)

                            arg = arguments.firstOrNull { it.nameOrId == argName }
                                    ?: throw ArgumentNotFoundException(command,
                                    args.toList(),
                                    argName,
                                    this.commandManager,
                                    "No argument found for input name '$argName' of command $command")

                            if (it + 1 > size) {
                                if (arg.isBoolean(args))
                                    argInput = SingleInput("true")
                                else
                                    throw NoInputForArgumentException(command,
                                            args.toList(),
                                            arg,
                                            this.commandManager,
                                            "No input for specified named argument '${arg.nameOrIdWithType}' of command $command")
                            } else {
                                if (isName(it + 1) && arg.isBoolean(args)) {
                                    argInput = SingleInput("true")
                                } else {
                                    if (arg.isMultiple) {
                                        parseVarargs(arg)
                                        arg = null
                                    } else {
                                        val input = getStr(it + 1)
                                        val inputObj = SingleInput(input)

                                        if (!arg.validator(args, arg, inputObj))
                                            throw InvalidInputForArgumentException(
                                                    command,
                                                    args.toList(),
                                                    inputObj,
                                                    arg,
                                                    this.commandManager,
                                                    "Invalid input $input for required argument " +
                                                            "'${arg.nameOrIdWithType}' of command $command.")
                                        argInput = inputObj
                                        ++searchPos
                                        ++index
                                    }
                                }
                            }
                        } else {
                            val inputObj = SingleInput(argStr)

                            if (!order) {
                                arg = arguments.find { it.validator(args, it, inputObj) }

                                if (arg == null && requiredCount == requiredArgsCount)
                                    break
                                else if (arg == null)
                                    throw NoArgumentForInputException(command,
                                            args.toList(),
                                            argStr,
                                            this.commandManager,
                                            "No argument for input string $argStr for command $command")
                            } else {
                                if (argPos >= arguments.size) {
                                    arg = null
                                } else {
                                    var any = false
                                    while (argPos < arguments.size) {
                                        val atPos = arguments[argPos]

                                        if (atPos.isMultiple) {
                                            parseVarargs(atPos)
                                            arg = null
                                            any = true
                                            break
                                        } else {
                                            if (!atPos.validator(args, atPos, inputObj)) {
                                                if (!atPos.isOptional)
                                                    throw InvalidInputForArgumentException(
                                                            command,
                                                            args.toList(),
                                                            inputObj,
                                                            atPos,
                                                            this.commandManager,
                                                            "Invalid input $argStr for required argument " +
                                                                    "'${atPos.nameOrIdWithType}' of command $command.")

                                            } else {
                                                any = true
                                                arg = atPos
                                                break
                                            }
                                        }

                                        ++argPos
                                    }

                                    if (!any)
                                        throw NoArgumentForInputException(command,
                                                args.toList(),
                                                argStr,
                                                this.commandManager,
                                                "No argument for input string $argStr for command $command")
                                }
                            }
                        }

                        if (arg != null) {
                            if (!arg.isOptional)
                                requiredCount++
                            @Suppress("UNCHECKED_CAST")
                            args.add(ArgumentContainer(
                                    arg,
                                    argInput,
                                    arg.transformer(args, arg, argInput),
                                    arg.handler as? ArgumentHandler<Any?>
                            ))
                            arguments.remove(arg)
                        }
                        ++index
                        ++searchPos
                    }

                    if (requiredCount != requiredArgsCount) {
                        val missing = arguments.filter { !it.isOptional }.joinToString {
                            val poss = it.possibilities.invoke(args, it)
                            if (poss.isNotEmpty()) "${it.id}{possibilities=$poss}"
                            else it.id.toString()
                        }
                        throw ArgumentsMissingException(command,
                                args.toList(),
                                this.commandManager,
                                "Some required arguments of command $command is missing. (Missing arguments ids: $missing)")
                    }

                    arguments.map { ArgumentContainer(it, null, it.defaultValue, null) }
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

    @Suppress("NOTHING_TO_INLINE")
    inline private fun String.isAndOp() = this == "&"
}