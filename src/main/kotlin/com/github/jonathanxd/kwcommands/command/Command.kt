/*
 *      KWCommands - New generation of WCommands written in Kotlin <https://github.com/JonathanxD/KWCommands>
 *
 *         The MIT License (MIT)
 *
 *      Copyright (c) 2020 JonathanxD
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

import com.github.jonathanxd.iutils.text.Text
import com.github.jonathanxd.iutils.text.TextComponent
import com.github.jonathanxd.kwcommands.NamedAndAliased
import com.github.jonathanxd.kwcommands.argument.Arguments
import com.github.jonathanxd.kwcommands.information.RequiredInformation
import com.github.jonathanxd.kwcommands.requirement.Requirement
import java.util.*

/**
 * Command definition.
 *
 * The difference between [name] and [nameComponent] is that [nameComponent] is the name
 * used to interface with the user. Also user may use the [name] to dispatch the command too,
 * but cannot use a name that is resolved by a locale rather than the current locale, in
 * other words, if you define that the [name] is `foo`, and that in language Y the name
 * is `bar` and in language Z that the name is `baz` and current language is `Y`, then dispatching
 * command with `bar` and `foo` works, but with `baz` does not work.
 *
 * @property parent Parent command.
 * @property order Command order.
 * @property name Command name.
 * @property nameComponent Command name component.
 * @property description Command description.
 * @property handler Command handler.
 * @property arguments Arguments that this command can receive.
 * @property requirements Command requirements.
 * @property requiredInfo Identifications of required information for this command work.
 * @property alias Aliases to this command.
 */
data class Command(
    val parent: Command?,
    val order: Int,
    override val name: String,
    override val nameComponent: TextComponent,
    override val alias: List<String>,
    override val aliasComponent: TextComponent?,
    override val description: TextComponent,
    val handler: Handler?,
    val arguments: Arguments,
    val requirements: List<Requirement<*, *>>,
    val requiredInfo: Set<RequiredInformation>
) : Comparable<Command>, NamedAndAliased {

    constructor(
        parent: Command?,
        order: Int,
        name: String,
        alias: List<String>,
        description: TextComponent,
        handler: Handler?,
        arguments: Arguments,
        requirements: List<Requirement<*, *>>,
        requiredInfo: Set<RequiredInformation>
    ) : this(
        parent,
        order,
        name,
        Text.of(name),
        alias,
        null,
        description,
        handler,
        arguments,
        requirements,
        requiredInfo
    )

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

        while (superCommand.parent != null) {
            superCommand = superCommand.parent!!
        }

        return@let superCommand
    }


    /**
     * Path/name of this command. The [fullname] of a command is a combination
     * of the [fullname] of [parent] command and the [name] of this command.
     */
    val fullname: String
        get() = if (this.parent != null) "${this.parent.fullname} ${this.name}" else this.name


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
        this.subCommands_.removeAll { it.name == command.name }

    /**
     * Remove all commands with name [subCommandName].
     */
    fun removeSubCommand(subCommandName: String): Boolean =
        this.subCommands_.removeAll { it.name == subCommandName }

    /**
     * Gets the sub-command with specified [name]. This does not compare [nameComponent] because
     * it cannot be resolved from this context.
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
        return "Command(parent: ${this.parent?.name
                ?: "none"}, order: $order, name: $name, nameComponent: $nameComponent, description: $description, alias: $alias, aliasComponent: $aliasComponent, arguments: [${arguments.all.joinToString { "${it.name}: ${it.argumentType}" }}], requirements: [${requirements.joinToString { "${it.subject}: ${it.required}" }}], requiredInformation: [${requiredInfo.joinToString { it.id.toString() }}], subCommands: {${subCommands.joinToString { it.name }}})"
    }

    override fun compareTo(other: Command): Int {
        return if (this.parent == other.parent) this.name.compareTo(other.name) else -1
    }

    companion object {
        @JvmStatic
        fun builder() = CommandBuilder()
    }

}