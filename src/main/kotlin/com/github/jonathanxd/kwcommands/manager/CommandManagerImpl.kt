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
package com.github.jonathanxd.kwcommands.manager

import com.github.jonathanxd.iutils.collection.view.ViewCollections
import com.github.jonathanxd.iutils.collection.view.ViewUtils
import com.github.jonathanxd.iutils.kt.get
import com.github.jonathanxd.iutils.recursion.Element
import com.github.jonathanxd.iutils.recursion.ElementUtil
import com.github.jonathanxd.iutils.recursion.Elements
import com.github.jonathanxd.iutils.text.localizer.Localizer
import com.github.jonathanxd.kwcommands.command.Command
import com.github.jonathanxd.kwcommands.exception.NoCommandException
import com.github.jonathanxd.kwcommands.util.allSubCommandsTo
import com.github.jonathanxd.kwcommands.util.localizeMulti
import java.util.function.Function

/**
 * Command manager implementation.
 */
class CommandManagerImpl : CommandManager {

    private val commands = mutableSetOf<RegisteredCommand>()

    override val registeredCommands: Set<Command> =
        ViewCollections.setMapped<RegisteredCommand, Command>(
            this.commands,
            Function { it.command },
            ViewUtils.unmodifiable(),
            ViewUtils.unmodifiable()
        )

    override val commandsWithOwner: Set<Pair<Command, Any>> =
        ViewCollections.setMapped<RegisteredCommand, Pair<Command, Any>>(
            this.commands,
            Function { it.command to it.owner },
            ViewUtils.unmodifiable(),
            ViewUtils.unmodifiable()
        )

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


    override fun isRegistered(command: Command, owner: Any?) =
        this.commands.any { (owner == null || it.owner == owner) && it.command == command }

    override fun findCommand(name: String, owner: Any?, localizer: Localizer?): Command? {
        this.commands.forEach {

            if (owner == null || it.owner == owner) {
                it.command.getCommand(name, localizer)?.let {
                    return it
                }
            }
        }

        return null
    }

    override fun getCommand(name: String, owner: Any?, localizer: Localizer?): Command? =
        this.commands.find {
            (owner == null || it.owner == owner) && it.command.nameMatch(name, localizer)
        }?.command

    override fun getCommand(path: Array<String>, owner: Any?, localizer: Localizer?): Command =
        path.let {

            var cmd = this.getCommand(it.first(), owner, localizer)
                    ?: throw NoCommandException("Specified parent command ${it.first()} was not found.")

            if (it.size > 1) {
                for (x in it.copyOfRange(1, it.size)) {
                    cmd = this.getSubCommand(cmd, x, localizer)
                            ?:
                            throw NoCommandException("Specified parent command $x was not found in command $cmd.")
                }
            }

            cmd
        }

    override fun getOptionalCommand(
        path: Array<String>,
        owner: Any?,
        localizer: Localizer?
    ): Command? =
        path.let {
            if (it.isEmpty()) null else
                try {
                    this.getCommand(path, owner, localizer)
                } catch (c: NoCommandException) {
                    null
                }

        }

    override fun getOwners(command: Command): Set<Any> =
        this.commands.filter { it.command == command }.toSet()

    override fun getSubCommand(
        command: Command,
        name: String,
        localizer: Localizer?
    ): Command? =
        command.subCommands.firstOrNull {
            it.nameMatch(name, localizer)
        }

    override fun createCommandsPair(): List<Pair<Command, Any>> =
        this.commands.map { it.command to it.owner }

    override fun createListWithCommands(): List<Command> =
        this.commands.map { (command, _) -> command }

    override fun createListWithAllCommands(): List<Command> {
        val list = mutableListOf<Command>()

        this.commands.forEach {
            list.add(it.command)
            it.command.allSubCommandsTo(list)
        }

        return list
    }

    private fun Command.getCommand(name: String, localizer: Localizer?): Command? {

        if (this.nameMatch(name, localizer))
            return this

        val elements = Elements<Command>().apply {
            insert(Element(this@getCommand))
        }

        var command = elements.nextElement()?.value

        while (command != null) {
            if (command.nameMatch(name, localizer))
                return command

            elements.insertFromPair(ElementUtil.fromIterable(command.subCommands))

            command = elements.nextElement()?.value
        }

        return null
    }

    private fun Command.nameMatch(name: String, localizer: Localizer?): Boolean =
        this.name == name
                || localizer != null
                && (
                this.alias.any { it == name }
                        || localizer[this.nameComponent] == name
                        || this.aliasComponent?.localizeMulti(localizer)?.any { it == name } == true
                )

    internal data class RegisteredCommand(val command: Command, val owner: Any)
}