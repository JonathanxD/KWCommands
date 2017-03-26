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

import com.github.jonathanxd.kwcommands.command.Command
import java.util.*

/**
 * Manages and register commands. Only top-level commands can be registered.
 */
class CommandManager {

    /**
     * Command set.
     */
    private val commands = mutableSetOf<RegisteredCommand>()

    /**
     * Register [command].
     *
     * @param command Command to register.
     * @param owner Command owner.
     */
    fun registerCommand(command: Command, owner: Any): Boolean {

        if (command.parent != null)
            throw IllegalArgumentException("Command $command must be a top level command.")

        return this.commands.add(RegisteredCommand(command, owner))
    }

    /**
     * Unregister [command].
     *
     * @param command Command to unregister.
     * @param owner Owner of the command.
     */
    @JvmOverloads
    fun unregisterCommand(command: Command, owner: Any? = null): Boolean {
        if (command.parent != null)
            throw IllegalArgumentException("Command $command must be a top level command.")

        return this.commands.removeIf { (owner == null || it.owner == owner) && it.command == command }
    }

    /**
     * Searches for a command with name [name], this method recursively searches for this command.
     */
    @JvmOverloads
    fun findCommand(name: String, owner: Any? = null): Command? {
        this.commands.forEach {

            if (owner == null || it.owner == owner) {
                it.command.getCommand(name)?.let {
                    return it
                }
            }
        }

        return null
    }

    /**
     * Gets the command with specified [name].
     */
    @JvmOverloads
    fun getCommand(name: String, owner: Any? = null): Command? = this.commands.find {
        (owner == null || it.owner == owner) && it.command.name.compareTo(name) == 0
    }?.command

    /**
     * Creates a pair of command and the command owner.
     *
     * Modifications made in this list is not reflected in the original list.
     */
    fun createCommandsPair(): List<Pair<Command, Any>> =
            this.commands.map { it.command to it.owner }


    private fun Command.getCommand(name: String): Command? {

        if (this.name.compareTo(name) == 0)
            return this

        this.subCommands.forEach {
            it.getCommand(name)?.let {
                return it
            }
        }

        return null
    }

    private class RegisteredCommand(val command: Command, val owner: Any) {

        override fun equals(other: Any?): Boolean {
            return if (other is RegisteredCommand) this.command == other.command && this.owner == other.owner else super.equals(other)
        }

        override fun hashCode(): Int {
            return Objects.hash(command, owner)
        }

        override fun toString(): String {
            return "RegisteredCommand(command: $command, owner: $owner)"
        }
    }
}