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
package com.github.jonathanxd.kwcommands.manager

import com.github.jonathanxd.iutils.kt.typeInfo
import com.github.jonathanxd.kwcommands.command.Command
import com.github.jonathanxd.kwcommands.information.Information

/**
 * Manages and register commands. Only top-level commands can be registered.
 *
 * Command managers should link commands to an owner.
 *
 * Obs: There is no restriction to register same command instance with different owners.
 */
interface CommandManager {

    /**
     * Registered commands. The returned list cannot be modified
     */
    val registeredCommands: Set<Command>

    /**
     * Register [command].
     *
     * @param command Command to register.
     * @param owner Command owner.
     */
    fun registerCommand(command: Command, owner: Any): Boolean

    /**
     * Register all [commands]
     *
     * @return `true` if any command was registered with success.
     */
    fun registerAll(commands: Iterable<Command>, owner: Any): Boolean =
        commands.map { this.registerCommand(it, owner) }.any { it }

    /**
     * Unregister [command].
     *
     * @param command Command to unregister.
     * @param owner Owner of the command.
     */
    fun unregisterCommand(command: Command, owner: Any?): Boolean

    /**
     * Unregister all [Command] instances linked to [owner].
     *
     * @param owner Owner of the command.
     * @return True if the list was changed as result of this operation.
     */
    fun unregisterAllCommandsOfOwner(owner: Any): Boolean

    /**
     * Returns true if command is registered.
     *
     * @param command Command to check.
     * @param owner Owner of command.
     */
    fun isRegistered(command: Command, owner: Any? = null): Boolean

    /**
     * Searches for a command with name [name], this method recursively searches for this command.
     */
    fun findCommand(name: String, owner: Any? = null): Command?

    /**
     * Gets the command with specified [name].
     */
    fun getCommand(name: String, owner: Any? = null): Command?

    /**
     * Gets the command in specified [path].
     */
    fun getCommand(path: Array<String>, owner: Any? = null): Command

    /**
     * Gets the command in specified [path].
     */
    fun getOptionalCommand(path: Array<String>, owner: Any? = null): Command?

    /**
     * Gets the owner set of a command (empty if not registered),
     * in KWCommands, the same command instance can be registered with
     * different owners.
     */
    fun getOwners(command: Command): Set<Any>

    /**
     * Gets sub command of [command] that matches [name].
     */
    fun getSubCommand(command: Command, name: String): Command?

    /**
     * Creates a pair of command and the command owner.
     *
     * Modifications made in this list is not reflected in the original list.
     */
    fun createCommandsPair(): List<Pair<Command, Any>>

    /**
     * Creates a list with registered commands.
     *
     * Modifications in this class is not reflected in original class.
     */
    fun createListWithCommands(): List<Command>

    /**
     * Creates a list with all commands including sub commands.
     */
    fun createListWithAllCommands(): List<Command>

}