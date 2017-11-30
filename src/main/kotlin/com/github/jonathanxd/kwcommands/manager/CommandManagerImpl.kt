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

import com.github.jonathanxd.iutils.collection.view.ViewCollections
import com.github.jonathanxd.iutils.iterator.IteratorUtil
import com.github.jonathanxd.kwcommands.command.Command
import com.github.jonathanxd.kwcommands.exception.NoCommandException
import com.github.jonathanxd.kwcommands.util.allSubCommandsTo

/**
 * Command manager implementation.
 */
class CommandManagerImpl : CommandManager {

    private val commands = mutableSetOf<RegisteredCommand>()
    private val commands_ = ViewCollections.setMapped(commands,
            { e, i -> IteratorUtil.mapped(e, i, { it.command }) },
            { throw UnsupportedOperationException() },
            { throw UnsupportedOperationException() })

    override val registeredCommands: Set<Command> = this.commands_

    override fun registerCommand(command: Command, owner: Any): Boolean {

        if (command.parent != null) {
            if (!command.parent.subCommands.contains(command))
                return command.parent.addSubCommand(command)
        } else {
            return this.commands.add(RegisteredCommand(command, owner))
        }

        return false
    }

    override fun unregisterCommand(command: Command, owner: Any?): Boolean {
        if (command.parent != null)
            throw IllegalArgumentException("Command $command must be a top level command.")

        return this.commands.removeIf { (owner == null || it.owner == owner) && it.command == command }
    }

    override fun unregisterAllCommandsOfOwner(owner: Any): Boolean =
            commands.removeIf { it.owner == owner }


    override fun isRegistered(command: Command, owner: Any?) = this.commands.any { (owner == null || it.owner == owner) && it.command == command }

    override fun findCommand(name: String, owner: Any?): Command? {
        this.commands.forEach {

            if (owner == null || it.owner == owner) {
                it.command.getCommand(name)?.let {
                    return it
                }
            }
        }

        return null
    }

    override fun getCommand(name: String, owner: Any?): Command? = this.commands.find {
        (owner == null || it.owner == owner) && it.command.name.compareTo(name) == 0
    }?.command

    override fun getCommand(path: Array<String>, owner: Any?): Command = path.let {

        var cmd = this.getCommand(it.first(), owner) ?:
                throw NoCommandException("Specified parent command ${it.first()} was not found.")

        if (it.size > 1) {
            for (x in it.copyOfRange(1, it.size)) {
                cmd = this.getSubCommand(cmd, x)
                        ?: throw NoCommandException("Specified parent command $x was not found in command $cmd.")
            }
        }

        cmd
    }

    override fun getOptionalCommand(path: Array<String>, owner: Any?): Command? = path.let {
        if (it.isEmpty()) null else
            try {
                getCommand(path, owner)
            } catch (c: NoCommandException) {
                null
            }

    }

    override fun getOwners(command: Command): Set<Any> =
            this.commands.filter { it.command == command }.toSet()

    override fun getSubCommand(command: Command, name: String): Command? =
            command.getSubCommand(name)

    override fun createCommandsPair(): List<Pair<Command, Any>> =
            this.commands.map { it.command to it.owner }

    override fun createListWithCommands(): List<Command> = this.commands.map { (command, _) -> command }

    override fun createListWithAllCommands(): List<Command> {
        val list = mutableListOf<Command>()

        this.commands.forEach {
            list.add(it.command)
            it.command.allSubCommandsTo(list)
        }

        return list
    }

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

    internal data class RegisteredCommand(val command: Command, val owner: Any)
}