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
package com.github.jonathanxd.kwcommands.command

import com.github.jonathanxd.kwcommands.argument.Argument
import com.github.jonathanxd.kwcommands.information.Information
import com.github.jonathanxd.kwcommands.information.RequiredInformation
import com.github.jonathanxd.kwcommands.requirement.Requirement
import java.util.*

/**
 * A command.
 *
 * @property parent Parent command.
 * @property order Command order.
 * @property name Command name (string or regex).
 * @property description Command description.
 * @property handler Command handler.
 * @property arguments Arguments that this command can receive.
 * @property requirements Command requirements.
 * @property requiredInfo Identifications of required information for this command work.
 * @property alias Aliases to this command.
 */
data class Command(val parent: Command?,
                   val order: Int,
                   val name: CommandName,
                   val description: String,
                   val handler: Handler?,
                   val arguments: List<Argument<*>>,
                   val requirements: List<Requirement<*, *>>,
                   val requiredInfo: Set<RequiredInformation>,
                   val alias: List<CommandName>) : Comparable<Command> {

    /**
     * Sub commands
     */
    private val subCommands_ = mutableSetOf<Command>()
    val subCommands: Set<Command> = Collections.unmodifiableSet(this.subCommands_)

    /**
     * Super command. The super command is the highest [parent] command in command hierarchy.
     *
     * Super command of commands that are not sub-commands is `null`.
     */
    val superCommand: Command? = parent.let {
        var superCommand = parent ?: return@let null

        while(superCommand.parent != null) {
            superCommand = superCommand.parent!!
        }

        return@let superCommand
    }


    /**
     * Path/name of this command. The [fullname] of a command is a combination
     * of the [fullname] of [parent] command and the [name] of this command.
     */
    val fullname: String
        get() = if (this.parent != null) "${this.parent.fullname} ${this.name}" else this.name.toString()


    /**
     * Adds a copy of [command] to sub-command list.
     */
    fun addSubCommand(command: Command): Boolean {
        return this.subCommands_.add(command.copy(parent = this))
    }

    /**
     * Adds a copy of [commands] to sub-command list.
     */
    fun addSubCommands(commands: Iterable<Command>): Boolean {
        return this.subCommands_.addAll(commands)
    }

    /**
     * Remove all commands with same name as [command] from sub-command list.
     */
    fun removeSubCommand(command: Command): Boolean =
            this.subCommands_.removeIf { it.name == command.name }

    /**
     * Remove all commands with name [commandName].
     */
    fun removeSubCommand(commandName: CommandName): Boolean =
            this.subCommands_.removeIf { it.name == commandName }

    /**
     * Gets the sub-command with specified [name].
     */
    fun getSubCommand(name: String) = this.subCommands_.find { it.name.compareTo(name) == 0 }

    override fun hashCode(): Int {
        var result = 1

        result = 31 * result + if (this.parent == null) 0 else this.parent.hashCode()
        result = 31 * result + name.hashCode()

        return result
    }

    override fun equals(other: Any?): Boolean {
        return if (other != null && other is Command) this.parent == other.parent && this.name == other.name
        else super.equals(other)
    }

    override fun toString(): String {
        return "Command(parent: ${this.parent?.name ?: "none"}, order: $order, name: $name, description: $description, alias: $alias, arguments: [${arguments.joinToString { "${it.id}: ${it.type}" }}], requirements: [${requirements.joinToString { "${it.subject}: ${it.required}" }}], requiredInformation: [${requiredInfo.joinToString { it.id.toString() }}], subCommands: {${subCommands.joinToString { it.name.toString() }}})"
    }

    override fun compareTo(other: Command): Int {
        return if (this.parent == other.parent) this.name.compareTo(other.name.toString()) else -1
    }

    companion object {
        @JvmStatic
        fun builder() = CommandBuilder()
    }

}