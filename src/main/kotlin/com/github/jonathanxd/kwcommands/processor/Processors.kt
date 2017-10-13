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

import com.github.jonathanxd.iutils.option.Options
import com.github.jonathanxd.kwcommands.argument.Argument
import com.github.jonathanxd.kwcommands.argument.ArgumentContainer
import com.github.jonathanxd.kwcommands.argument.ArgumentHandler
import com.github.jonathanxd.kwcommands.command.Command
import com.github.jonathanxd.kwcommands.command.CommandContainer
import com.github.jonathanxd.kwcommands.command.Container
import com.github.jonathanxd.kwcommands.exception.*
import com.github.jonathanxd.kwcommands.information.Information
import com.github.jonathanxd.kwcommands.information.MissingInformation
import com.github.jonathanxd.kwcommands.information.checkRequiredInfo
import com.github.jonathanxd.kwcommands.interceptor.CommandInterceptor
import com.github.jonathanxd.kwcommands.manager.CommandManager
import com.github.jonathanxd.kwcommands.manager.CommandManagerImpl
import com.github.jonathanxd.kwcommands.manager.InformationManager
import com.github.jonathanxd.kwcommands.requirement.checkRequirements
import com.github.jonathanxd.kwcommands.util.escape
import com.github.jonathanxd.kwcommands.util.nameOrIdWithType
import java.util.*

object Processors {

    @JvmStatic
    fun createCommonProcessor(): CommandProcessor =
            CommonCommandProcessor()

    @JvmStatic
    fun createCommonProcessor(manager: CommandManager): CommandProcessor =
            CommonCommandProcessor(manager)

    private class CommonCommandProcessor(override val commandManager: CommandManager = CommandManagerImpl()) : CommandProcessor {

        private val interceptors = mutableSetOf<CommandInterceptor>()
        override val options: Options = Options()

        override fun registerInterceptor(commandInterceptor: CommandInterceptor): Boolean =
                this.interceptors.add(commandInterceptor)

        override fun unregisterInterceptor(commandInterceptor: CommandInterceptor): Boolean =
                this.interceptors.remove(commandInterceptor)

        override fun processWithOwnerFunction(stringList: List<String>,
                                              ownerProvider: (commandName: String) -> Any?): List<CommandContainer> {
            val order = this.options[KWParserOptions.ORDER]
            val commands = mutableListOf<CommandContainer>()
            val deque: Deque<Command> = LinkedList()
            val getStr: (Int) -> String = { stringList[it].escape('\\') }

            var index = 0

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
                        command = deque.last.getSubCommand(getStr(index))

                        if (command == null) {
                            val rm = deque.removeLast()

                            if (rm.parent != null)
                                deque.offerLast(rm.parent)
                        }
                    }
                }

                if (index + 1 == stringList.size ||
                        (index + 1 < stringList.size
                                && (stringList[index + 1].isAndOp() || command.getSubCommand(getStr(index + 1)) == null))) {

                    val arguments = command.arguments.toMutableList()
                    val args = mutableListOf<ArgumentContainer<*>>()

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

                        val size = (index + arguments.size).let {
                            if (it >= stringList.size)
                                stringList.size - 1
                            else
                                it
                        }

                        var argPos = 0

                        ((index + 1)..size).forEach {
                            val argStr = getStr(it)

                            var arg: Argument<*>? = null

                            if (!order)
                                arg = arguments.find { it.validator(argStr) }
                            else {
                                if (argPos >= arguments.size)
                                    arg = null
                                else {
                                    var any = false
                                    while (argPos < arguments.size) {
                                        val atPos = arguments[argPos]
                                        if (!atPos.validator(argStr)) {
                                            if (!atPos.isOptional)
                                                throw InvalidInputForArgumentException(
                                                        command!!,
                                                        args.toList(),
                                                        argStr,
                                                        atPos,
                                                        this.commandManager,
                                                        "Invalid input $argStr for required argument " +
                                                                "'${atPos.nameOrIdWithType}' of command $command.")

                                        } else {
                                            any = true
                                            arg = atPos
                                        }
                                        ++argPos
                                    }

                                    if (!any)
                                        throw NoArgumentForInputException(command!!,
                                                args.toList(),
                                                argStr,
                                                this.commandManager,
                                                "No argument for input string $argStr for command $command")
                                }
                            }

                            if (arg != null) {
                                if (!arg.isOptional)
                                    requiredCount++
                                @Suppress("UNCHECKED_CAST")
                                args.add(ArgumentContainer(arg, argStr, arg.transformer(argStr), arg.handler as? ArgumentHandler<Any?>))
                                arguments.remove(arg)
                                ++index
                            }
                        }

                        if (requiredCount != requiredArgsCount) {
                            val missing = arguments.filter { !it.isOptional }.joinToString {
                                if (it.possibilities.isNotEmpty()) "${it.id}{possibilities=${it.possibilities}}"
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

        @Suppress("UNCHECKED_CAST")
        override fun handle(commands: List<CommandContainer>, informationManager: InformationManager): List<CommandResult> {
            val results = mutableListOf<CommandResult>()
            val perCommandResults = mutableListOf<CommandResult>()

            commands.forEach { command ->
                perCommandResults.clear()

                var container: CommandContainer? = command

                interceptors.forEach { interceptor ->
                    container?.let {
                        container = interceptor.pre(command, it)
                    }
                }

                container?.let {
                    val argWithReq = it.arguments.map {
                        it to it.argument.requirements.checkRequirements(informationManager)
                    }

                    val argWithInfoReq = it.arguments.map {
                        it to it.argument.requiredInfo.checkRequiredInfo(informationManager)
                    }

                    val commandReq =
                            command.command.requirements.checkRequirements(informationManager)

                    val commandInfoReq =
                            command.command.requiredInfo.checkRequiredInfo(informationManager)

                    if(commandReq.isNotEmpty()) {
                        perCommandResults.add(UnsatisfiedRequirementsResult(commandReq, null, it))
                    }

                    if (commandInfoReq.isNotEmpty()) {
                        perCommandResults.add(MissingInformationResult(
                                missingInformationList = commandInfoReq,
                                requester = command.command,
                                rootContainer = null,
                                container = it))
                    }

                    var anyArgumentReqMissing = false

                    if(commandReq.isEmpty() && commandInfoReq.isEmpty() && !anyArgumentReqMissing) { // <-- WHAT?
                        argWithReq.forEach { (arg, req) ->
                            if(req.isNotEmpty()) {
                                anyArgumentReqMissing = true
                                perCommandResults.add(UnsatisfiedRequirementsResult(req, rootContainer = it, container = arg))
                            }
                        }

                        argWithInfoReq.forEach { (arg, infoReq) ->
                            if (infoReq.isNotEmpty()) {
                                anyArgumentReqMissing = true

                                perCommandResults.add(MissingInformationResult(
                                        missingInformationList = infoReq,
                                        requester = arg,
                                        rootContainer = it,
                                        container = arg))
                            }
                        }

                        var shouldExecuteCommand = !anyArgumentReqMissing

                        // Process arguments first because arguments must be resolved before command handling
                        it.arguments.forEach { arg ->
                            (arg as ArgumentContainer<Any?>).handler?.let { handler ->
                                val resultHandler = ParticularResultHandler(root = it, current = arg, targetList = perCommandResults)

                                val handle = handler.handle(arg, it, informationManager, resultHandler)

                                if(resultHandler.shouldCancel())
                                    shouldExecuteCommand = false

                                resultHandler.result(handle)
                            }
                        }

                        if(shouldExecuteCommand) {
                            val resultHandler = ParticularResultHandler(
                                    root = null,
                                    current = it,
                                    targetList = perCommandResults)

                            it.handler?.let { handler ->
                                val handle = handler.handle(it, informationManager, resultHandler)

                                resultHandler.result(handle)
                            }


                        }
                    }

                    interceptors.forEach { interceptor ->
                        interceptor.post(command, it, perCommandResults)
                    }

                    results.addAll(perCommandResults)
                }
            }

            return results
        }

        @Suppress("NOTHING_TO_INLINE")
        inline private fun String.isAndOp() = this == "&"
    }

    private class ParticularResultHandler(val root: Container?,
                                           val current: Container,
                                           val targetList: MutableList<CommandResult>): ResultHandler {

        private var cancel = false

        override fun informationMissing(missingInformationList: List<MissingInformation>, requester: Any, cancel: Boolean) {
            this.targetList += MissingInformationResult(missingInformationList, requester, root, current)

            if(cancel)
                this.cancel = true
        }

        override fun result(value: Any?) {
            if(value !is Unit)
                this.targetList += ValueResult(value, root, current)
        }

        override fun shouldCancel(): Boolean {
            return this.cancel
        }
    }

}