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
package com.github.jonathanxd.kwcommands.command

import com.github.jonathanxd.iutils.text.Text
import com.github.jonathanxd.iutils.text.TextComponent
import com.github.jonathanxd.kwcommands.argument.Argument
import com.github.jonathanxd.kwcommands.argument.Arguments
import com.github.jonathanxd.kwcommands.argument.StaticListArguments
import com.github.jonathanxd.kwcommands.argument.StaticListArgumentsBuilder
import com.github.jonathanxd.kwcommands.information.RequiredInformation
import com.github.jonathanxd.kwcommands.requirement.Requirement

/**
 * Builder of Command
 */
class CommandBuilder {

    private var parent: Command? = null
    private var order = 0
    private lateinit var name: String
    private var description: TextComponent = Text.single("")
    private var handler: Handler? = null
    private var arguments: Arguments = StaticListArguments(emptyList())
    private val requirements = mutableListOf<Requirement<*, *>>()
    private val requiredInfo = mutableSetOf<RequiredInformation>()
    private val alias = mutableListOf<String>()

    /**
     * Sets [Command.parent].
     */
    fun parent(parent: Command?): CommandBuilder {
        this.parent = parent
        return this
    }

    /**
     * Sets [Command.order].
     */
    fun order(order: Int): CommandBuilder {
        this.order = order
        return this
    }

    /**
     * Sets [Command.name].
     */
    fun name(name: String): CommandBuilder {
        this.name = name
        return this
    }

    /**
     * Sets [Command.description].
     */
    fun description(description: TextComponent): CommandBuilder {
        this.description = description
        return this
    }

    /**
     * Sets [Command.handler].
     */
    fun handler(handler: Handler?): CommandBuilder {
        this.handler = handler
        return this
    }

    /**
     * Adds [Command.arguments].
     */
    fun arguments(arguments: Arguments): CommandBuilder {
        this.arguments = arguments
        return this
    }

    /**
     * Creates and adds arguments of builded [StaticListArguments].
     */
    fun staticArguments(): StaticListArgumentsBuilderToCommand =
        StaticListArgumentsBuilderToCommand(this)

    /**
     * Adds [Command.requirements].
     */
    fun addRequirements(requirements: List<Requirement<*, *>>): CommandBuilder {
        this.requirements.addAll(requirements)
        return this
    }

    /**
     * Add a [Requirement][Command.requirements].
     */
    fun addRequirements(requirement: Requirement<*, *>): CommandBuilder {
        this.requirements.add(requirement)
        return this
    }

    /**
     * Removes a [Requirement][Command.requirements].
     */
    fun removeRequirements(requirement: Requirement<*, *>): CommandBuilder {
        this.requirements.remove(requirement)
        return this
    }

    /**
     * Clear [Command.requirements]
     */
    fun clearRequirements(): CommandBuilder {
        this.requirements.clear()
        return this
    }

    /**
     * Adds [Command.requiredInfo].
     */
    fun addRequiredInfo(requiredInfoList: List<RequiredInformation>): CommandBuilder {
        this.requiredInfo.addAll(requiredInfoList)
        return this
    }

    /**
     * Add a [Requirement][Command.requiredInfo].
     */
    fun addRequiredInfo(requiredInfo: RequiredInformation): CommandBuilder {
        this.requiredInfo.add(requiredInfo)
        return this
    }

    /**
     * Removes a [Requirement][Command.requiredInfo].
     */
    fun removeRequiredInfo(requiredInfo: RequiredInformation): CommandBuilder {
        this.requiredInfo.remove(requiredInfo)
        return this
    }

    /**
     * Clear [Command.requiredInfo]
     */
    fun clearRequiredInfo(): CommandBuilder {
        this.requiredInfo.clear()
        return this
    }

    /**
     * Adds [Command.alias].
     */
    fun addAlias(aliases: List<String>): CommandBuilder {
        this.alias.addAll(aliases)
        return this
    }

    /**
     * Adds an [Alias][Command.alias]
     */
    fun addAlias(alias: String): CommandBuilder {
        this.alias.add(alias)
        return this
    }

    /**
     * Removes an [Alias][Command.alias]
     */
    fun removeAlias(alias: String): CommandBuilder {
        this.alias.remove(alias)
        return this
    }

    /**
     * Clear [Command.alias]
     */
    fun clearAlias(): CommandBuilder {
        this.alias.clear()
        return this
    }

    fun build(): Command = Command(
        parent = this.parent,
        order = this.order,
        name = this.name,
        description = this.description,
        handler = this.handler,
        arguments = this.arguments,
        requirements = this.requirements.toList(),
        requiredInfo = this.requiredInfo.toSet(),
        alias = this.alias.toList()
    )

}

class StaticListArgumentsBuilderToCommand(private val cmdBuilder: CommandBuilder) {
    private val builder = StaticListArgumentsBuilder()

    fun addArgument(argument: Argument<*>): StaticListArgumentsBuilderToCommand {
        this.builder.addArgument(argument)
        return this
    }

    fun addArguments(arguments: Iterable<Argument<*>>): StaticListArgumentsBuilderToCommand {
        this.builder.addArguments(arguments)
        return this
    }

    fun removeArgument(argument: Argument<*>): StaticListArgumentsBuilderToCommand {
        this.builder.removeArgument(argument)
        return this
    }

    fun removeArguments(arguments: Iterable<Argument<*>>): StaticListArgumentsBuilderToCommand {
        this.builder.removeArguments(arguments)
        return this
    }

    fun setArguments(arguments: Iterable<Argument<*>>): StaticListArgumentsBuilderToCommand =
        this.clear().addArguments(arguments)

    fun clear(): StaticListArgumentsBuilderToCommand {
        this.builder.clear()
        return this
    }

    fun build(): CommandBuilder {
        return this.cmdBuilder.arguments(this.builder.build())
    }
}