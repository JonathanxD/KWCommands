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
package com.github.jonathanxd.kwcommands.argument

import com.github.jonathanxd.iutils.text.Text
import com.github.jonathanxd.iutils.text.TextComponent
import com.github.jonathanxd.kwcommands.command.Command
import com.github.jonathanxd.kwcommands.information.RequiredInformation
import com.github.jonathanxd.kwcommands.parser.*
import com.github.jonathanxd.kwcommands.requirement.Requirement

/**
 * Builder of [Argument].
 */
class ArgumentBuilder<T> {

    private var name: String = ""
    private var alias = mutableListOf<String>()
    private var description: TextComponent = Text.single("")
    private var isOptional: Boolean = false
    private lateinit var argumentType: ArgumentType<*, T>
    private var isMultiple: Boolean = false
    private var defaultValue: T? = null
    private val requirements = mutableListOf<Requirement<*, *>>()
    private val requiredInfo = mutableSetOf<RequiredInformation>()
    private var handler: ArgumentHandler<out T>? = null

    /**
     * Sets [Argument.name]
     */
    fun name(name: String): ArgumentBuilder<T> {
        this.name = name
        return this
    }

    /**
     * Sets [Argument.alias]
     */
    fun alias(alias: List<String>): ArgumentBuilder<T> {
        this.alias = alias.toMutableList()
        return this
    }

    /**
     * Adds [Argument.alias].
     */
    fun addAlias(aliases: List<String>): ArgumentBuilder<T> {
        this.alias.addAll(aliases)
        return this
    }

    /**
     * Adds an [Alias][Argument.alias]
     */
    fun addAlias(alias: String): ArgumentBuilder<T> {
        this.alias.add(alias)
        return this
    }

    /**
     * Removes an [Alias][Argument.alias]
     */
    fun removeAlias(alias: String): ArgumentBuilder<T> {
        this.alias.remove(alias)
        return this
    }

    /**
     * Clear [Argument.alias]
     */
    fun clearAlias(): ArgumentBuilder<T> {
        this.alias.clear()
        return this
    }

    /**
     * Sets [Argument.description]
     */
    fun description(description: TextComponent): ArgumentBuilder<T> {
        this.description = description
        return this
    }

    /**
     * Sets [Argument.isOptional]
     */
    fun optional(isOptional: Boolean): ArgumentBuilder<T> {
        this.isOptional = isOptional
        return this
    }

    /**
     * Sets [Argument.argumentType]
     */
    fun argumentType(type: ArgumentType<*, T>): ArgumentBuilder<T> {
        this.argumentType = type
        return this
    }

    /**
     * Sets [Argument.argumentType]
     */
    fun multiple(isMultiple: Boolean): ArgumentBuilder<T> {
        this.isMultiple = isMultiple
        return this
    }

    /**
     * Adds [Argument.requirements]
     */
    fun addRequirements(requirements: List<Requirement<*, *>>): ArgumentBuilder<T> {
        this.requirements.addAll(requirements)
        return this
    }

    /**
     * Adds a [Requirement][Argument.requirements]
     */
    fun addRequirement(requirement: Requirement<*, *>): ArgumentBuilder<T> {
        this.requirements.add(requirement)
        return this
    }

    /**
     * Removes a [Requirement][Argument.requirements]
     */
    fun removeRequirement(requirement: Requirement<*, *>): ArgumentBuilder<T> {
        this.requirements.remove(requirement)
        return this
    }

    /**
     * Clear [Argument.requirements]
     */
    fun clearRequirements(): ArgumentBuilder<T> {
        this.requirements.clear()
        return this
    }

    /**
     * Adds [Command.requiredInfo].
     */
    fun addRequiredInfo(requiredInfoList: List<RequiredInformation>): ArgumentBuilder<T> {
        this.requiredInfo.addAll(requiredInfoList)
        return this
    }

    /**
     * Add a [Requirement][Command.requiredInfo].
     */
    fun addRequiredInfo(requiredInfo: RequiredInformation): ArgumentBuilder<T> {
        this.requiredInfo.add(requiredInfo)
        return this
    }

    /**
     * Removes a [Requirement][Command.requiredInfo].
     */
    fun removeRequiredInfo(requiredInfo: RequiredInformation): ArgumentBuilder<T> {
        this.requiredInfo.remove(requiredInfo)
        return this
    }

    /**
     * Clear [Command.requiredInfo]
     */
    fun clearRequiredInfo(): ArgumentBuilder<T> {
        this.requiredInfo.clear()
        return this
    }

    /**
     * Sets [Argument.handler]
     */
    fun handler(handler: ArgumentHandler<out T>?): ArgumentBuilder<T> {
        this.handler = handler
        return this
    }

    /**
     * Builds argument.
     */
    fun build(): Argument<T> = Argument(
            name = this.name,
            alias = this.alias,
            description = this.description,
            isOptional = this.isOptional,
            argumentType = this.argumentType,
            requirements = this.requirements.toList(),
            requiredInfo = this.requiredInfo.toSet(),
            handler = this.handler
    )

}