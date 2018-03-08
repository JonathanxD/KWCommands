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
package com.github.jonathanxd.kwcommands.dispatch

import com.github.jonathanxd.iutils.option.Options
import com.github.jonathanxd.kwcommands.command.CommandContainer
import com.github.jonathanxd.kwcommands.information.InformationProviders
import com.github.jonathanxd.kwcommands.information.InformationProvidersVoid
import com.github.jonathanxd.kwcommands.interceptor.CommandInterceptor
import com.github.jonathanxd.kwcommands.manager.CommandManager
import com.github.jonathanxd.kwcommands.processor.CommandResult
import com.github.jonathanxd.kwcommands.processor.ResultHandler

interface CommandDispatcher {
    /**
     * Command manager.
     */
    val commandManager: CommandManager

    /**
     * Dispatcher options
     */
    val options: Options

    /**
     * Register a [command interceptor][commandInterceptor].
     */
    fun registerInterceptor(commandInterceptor: CommandInterceptor): Boolean

    /**
     * Registers [command interceptors][commandInterceptors]
     */
    fun registerInterceptors(commandInterceptors: List<CommandInterceptor>): Boolean =
        commandInterceptors.map { this.registerInterceptor(it) }.any { it }

    /**
     * Unregister a [command interceptor][commandInterceptor].
     */
    fun unregisterInterceptor(commandInterceptor: CommandInterceptor): Boolean

    /**
     * Registers a [dispatch handler][dispatchHandler]
     */
    fun registerDispatchHandler(dispatchHandler: DispatchHandler): Boolean

    /**
     * Registers [dispatch handlers][dispatchHandlers]
     */
    fun registerDispatchHandlers(dispatchHandlers: List<DispatchHandler>): Boolean =
        dispatchHandlers.map { this.registerDispatchHandler(it) }.any { it }

    /**
     * Unregisters a [dispatch handler][dispatchHandler]
     */
    fun unregisterDispatchHandler(dispatchHandler: DispatchHandler): Boolean

    /**
     * Handle [commands] and returns [result list][CommandResult] of command executions.
     *
     * This function will first check requirements, and then handle arguments and the command.
     *
     * @param commands Command to handle.
     * @param informationProviders Information providers.
     * @return Result of command handling process. May be command handler return values or values added via
     * [ResultHandler]. Results are commonly sorted and the list may contains more than one [CommandResult] for
     * each command.
     */
    fun dispatch(
        commands: List<CommandContainer>,
        informationProviders: InformationProviders = InformationProvidersVoid
    ): List<CommandResult>

}