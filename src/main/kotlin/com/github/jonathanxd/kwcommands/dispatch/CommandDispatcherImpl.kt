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
package com.github.jonathanxd.kwcommands.dispatch

import com.github.jonathanxd.iutils.option.Options
import com.github.jonathanxd.kwcommands.argument.ArgumentContainer
import com.github.jonathanxd.kwcommands.command.CommandContainer
import com.github.jonathanxd.kwcommands.command.Container
import com.github.jonathanxd.kwcommands.interceptor.CommandInterceptor
import com.github.jonathanxd.kwcommands.manager.CommandManager
import com.github.jonathanxd.kwcommands.manager.InformationProviders
import com.github.jonathanxd.kwcommands.processor.*
import com.github.jonathanxd.kwcommands.requirement.checkRequirements
import com.github.jonathanxd.kwcommands.util.MissingInformation
import com.github.jonathanxd.kwcommands.util.checkRequiredInfo
import java.util.*

class CommandDispatcherImpl(override val commandManager: CommandManager) : CommandDispatcher {
    override val options: Options = Options()

    private val interceptors = mutableSetOf<CommandInterceptor>()
    private val dispatchHandlers = mutableSetOf<DispatchHandler>()

    override fun registerInterceptor(commandInterceptor: CommandInterceptor): Boolean =
            this.interceptors.add(commandInterceptor)

    override fun unregisterInterceptor(commandInterceptor: CommandInterceptor): Boolean =
            this.interceptors.remove(commandInterceptor)

    override fun registerDispatchHandler(dispatchHandler: DispatchHandler): Boolean =
            this.dispatchHandlers.add(dispatchHandler)

    override fun unregisterDispatchHandler(dispatchHandler: DispatchHandler): Boolean =
            this.dispatchHandlers.remove(dispatchHandler)

    override fun dispatch(commands: List<CommandContainer>,
                          informationProviders: InformationProviders): List<CommandResult> {
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
                    it to it.argument.requirements.checkRequirements(informationProviders)
                }

                val argWithInfoReq = it.arguments.map {
                    it to it.argument.requiredInfo.checkRequiredInfo(informationProviders)
                }

                val commandReq =
                        command.command.requirements.checkRequirements(informationProviders)

                val commandInfoReq =
                        command.command.requiredInfo.checkRequiredInfo(informationProviders)

                if (commandReq.isNotEmpty()) {
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

                if (commandReq.isEmpty() && commandInfoReq.isEmpty() && !anyArgumentReqMissing) { // <-- WHAT?
                    argWithReq.forEach { (arg, req) ->
                        if (req.isNotEmpty()) {
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
                        @Suppress("UNCHECKED_CAST")
                        (arg as ArgumentContainer<Any?>).handler?.let { handler ->
                            val resultHandler = ParticularResultHandler(
                                    root = it,
                                    current = arg,
                                    targetList = perCommandResults)

                            val handle = handler.handle(arg, it, informationProviders, resultHandler)

                            if (resultHandler.shouldCancel())
                                shouldExecuteCommand = false

                            resultHandler.result(handle)
                        }
                    }

                    if (shouldExecuteCommand) {
                        val resultHandler = ParticularResultHandler(
                                root = null,
                                current = it,
                                targetList = perCommandResults)

                        it.handler?.let { handler ->
                            val handle = handler.handle(it, informationProviders, resultHandler)

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

        if (this.dispatchHandlers.isNotEmpty() && results.isNotEmpty()) {
            val resultsI = Collections.unmodifiableList(results)

            this.dispatchHandlers.forEach {
                it.handle(resultsI)
            }
        }

        return results
    }

    private class ParticularResultHandler(val root: Container?,
                                          val current: Container,
                                          val targetList: MutableList<CommandResult>) : ResultHandler {

        private var cancel = false

        override fun informationMissing(missingInformationList: List<MissingInformation>, requester: Any, cancel: Boolean) {
            this.targetList += MissingInformationResult(missingInformationList, requester, root, current)

            if (cancel)
                this.cancel = true
        }

        override fun result(value: Any?) {
            if (value !is Unit)
                this.targetList += ValueResult(value, root, current)
        }

        override fun shouldCancel(): Boolean {
            return this.cancel
        }
    }
}