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

import com.github.jonathanxd.kwcommands.command.CommandContainer
import com.github.jonathanxd.kwcommands.dispatch.CommandDispatcher
import com.github.jonathanxd.kwcommands.interceptor.CommandInterceptor
import com.github.jonathanxd.kwcommands.manager.CommandManager
import com.github.jonathanxd.kwcommands.manager.InformationManager
import com.github.jonathanxd.kwcommands.manager.InformationManagerVoid
import com.github.jonathanxd.kwcommands.parser.CommandParser

interface CommandProcessor {

    /**
     * Command manager (the command manager of parser)
     */
    val commandManager: CommandManager
        get() = this.parser.commandManager

    /**
     * Parser of command
     */
    val parser: CommandParser

    /**
     * Dispatcher of command
     */
    val dispatcher: CommandDispatcher

    /**
     * Register a [command interceptor][CommandInterceptor].
     */
    fun registerInterceptor(commandInterceptor: CommandInterceptor): Boolean =
            this.dispatcher.registerInterceptor(commandInterceptor)

    /**
     * Unregister a [command interceptor][CommandInterceptor].
     */
    fun unregisterInterceptor(commandInterceptor: CommandInterceptor): Boolean =
            this.dispatcher.unregisterInterceptor(commandInterceptor)

    /**
     * Process command string list.
     *
     * @param commandString Command line string, with commands and arguments of commands.
     * @param owner Owner of the command. The owner is used to lookup for the command in the [commandManager], if a
     * null owner is provided, the [commandManager] will return the first found command.
     */
    fun process(commandString: String, owner: Any?): List<CommandContainer> =
            this.parser.parse(commandString, owner)

    /**
     * Process command string list.
     *
     * This provides a way to specify owner based on command input string (`commandName`).
     *
     * @param commandString Command line string, with commands and arguments of commands.
     * @param ownerProvider Provider of the owner of the input command.
     * The owner is used to lookup for the command in the [commandManager], if a
     * null owner is provided, the [commandManager] will return the first found command.
     */
    fun processWithOwnerFunction(commandString: String,
                                 ownerProvider: (commandName: String) -> Any?): List<CommandContainer> =
            this.parser.parseWithOwnerFunction(commandString, ownerProvider)

    /**
     * Handle [commands] and returns [result list][CommandResult] of command executions.
     *
     * This function will first check requirements, and then handle arguments and the command.
     *
     * @param commands Command to handle.
     * @param informationManager Information provide manager.
     * @return Result of command handling process. May be command handler return values or values added via
     * [ResultHandler]. Results are commonly sorted and the list may contains more than one [CommandResult] for
     * each command.
     */
    fun handle(commands: List<CommandContainer>,
               informationManager: InformationManager = InformationManagerVoid): List<CommandResult> =
            this.dispatcher.dispatch(commands, informationManager)

    /**
     * Calls [process] and then [handle] to handle result of [process].
     */
    fun processAndHandle(commandString: String,
                         owner: Any?,
                         informationManager: InformationManager = InformationManagerVoid): List<CommandResult> =
            processAndHandleWithOwnerFunc(commandString, { owner }, informationManager)

    /**
     * Calls [processWithOwnerFunction] and then [handle] to handle result of [process].
     */
    fun processAndHandleWithOwnerFunc(commandString: String,
                                      ownerProvider: (commandName: String) -> Any?,
                                      informationManager: InformationManager = InformationManagerVoid): List<CommandResult> =
            processWithOwnerFunction(commandString, ownerProvider).let { this.handle(it, informationManager) }
}