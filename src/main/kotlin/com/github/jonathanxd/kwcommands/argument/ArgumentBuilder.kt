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
import com.github.jonathanxd.iutils.type.TypeInfo
import com.github.jonathanxd.kwcommands.command.Command
import com.github.jonathanxd.kwcommands.information.RequiredInformation
import com.github.jonathanxd.kwcommands.parser.*
import com.github.jonathanxd.kwcommands.requirement.Requirement
import com.github.jonathanxd.kwcommands.util.possibilitiesFunc
import com.github.jonathanxd.kwcommands.util.simpleArgumentType

/**
 * Builder of [Argument].
 */
class ArgumentBuilder<I: Input, T> {

    private lateinit var id: Any
    private var name: String = ""
    private var description: TextComponent = Text.single("")
    private var isOptional: Boolean = false
    private lateinit var typeInfo: TypeInfo<out T>
    private var type: ArgumentType<*, T>? = null
    private var isMultiple: Boolean = false
    private var defaultValue: T? = null
    private lateinit var validator: Validator<I>
    private lateinit var transformer: Transformer<I, T>
    private var possibilities: Possibilities = possibilitiesFunc { emptyList() }
    private val requirements = mutableListOf<Requirement<*, *>>()
    private val requiredInfo = mutableSetOf<RequiredInformation>()
    private var handler: ArgumentHandler<out T>? = null

    /**
     * Sets [Argument.id]
     */
    fun id(id: Any): ArgumentBuilder<I, T> {
        this.id = id
        return this
    }

    /**
     * Sets [Argument.name]
     */
    fun name(name: String): ArgumentBuilder<I, T> {
        this.name = name
        return this
    }

    /**
     * Sets [Argument.description]
     */
    fun description(description: TextComponent): ArgumentBuilder<I, T> {
        this.description = description
        return this
    }

    /**
     * Sets [Argument.isOptional]
     */
    fun optional(isOptional: Boolean): ArgumentBuilder<I, T> {
        this.isOptional = isOptional
        return this
    }

    /**
     * Sets [Argument.type]
     */
    fun argumentType(type: ArgumentType<*, T>): ArgumentBuilder<I, T> {
        this.type = type
        return this
    }

    /**
     * Sets [Argument.type]
     */
    fun type(type: TypeInfo<out T>): ArgumentBuilder<I, T> {
        this.typeInfo = type
        return this
    }

    /**
     * Sets [Argument.type]
     */
    fun multiple(isMultiple: Boolean): ArgumentBuilder<I, T> {
        this.isMultiple = isMultiple
        return this
    }

    /**
     * Sets [Argument.defaultValue]
     */
    fun defaultValue(defaultValue: T?): ArgumentBuilder<I, T> {
        this.defaultValue = defaultValue
        return this
    }

    /**
     * Sets [Argument.validator]
     */
    fun validator(validator: Validator<I>): ArgumentBuilder<I, T> {
        this.validator = validator
        return this
    }

    /**
     * Sets [Argument.transformer]
     */
    fun transformer(transformer: Transformer<I, T>): ArgumentBuilder<I, T> {
        this.transformer = transformer
        return this
    }

    /**
     * Adds [Argument.possibilities]
     */
    fun possibilities(possibilities: Possibilities): ArgumentBuilder<I, T> {
        this.possibilities = possibilities
        return this
    }

    /**
     * Adds [Argument.requirements]
     */
    fun addRequirements(requirements: List<Requirement<*, *>>): ArgumentBuilder<I, T> {
        this.requirements.addAll(requirements)
        return this
    }

    /**
     * Adds a [Requirement][Argument.requirements]
     */
    fun addRequirement(requirement: Requirement<*, *>): ArgumentBuilder<I, T> {
        this.requirements.add(requirement)
        return this
    }

    /**
     * Removes a [Requirement][Argument.requirements]
     */
    fun removeRequirement(requirement: Requirement<*, *>): ArgumentBuilder<I, T> {
        this.requirements.remove(requirement)
        return this
    }

    /**
     * Clear [Argument.requirements]
     */
    fun clearRequirements(): ArgumentBuilder<I, T> {
        this.requirements.clear()
        return this
    }

    /**
     * Adds [Command.requiredInfo].
     */
    fun addRequiredInfo(requiredInfoList: List<RequiredInformation>): ArgumentBuilder<I, T> {
        this.requiredInfo.addAll(requiredInfoList)
        return this
    }

    /**
     * Add a [Requirement][Command.requiredInfo].
     */
    fun addRequiredInfo(requiredInfo: RequiredInformation): ArgumentBuilder<I, T> {
        this.requiredInfo.add(requiredInfo)
        return this
    }

    /**
     * Removes a [Requirement][Command.requiredInfo].
     */
    fun removeRequiredInfo(requiredInfo: RequiredInformation): ArgumentBuilder<I, T> {
        this.requiredInfo.remove(requiredInfo)
        return this
    }

    /**
     * Clear [Command.requiredInfo]
     */
    fun clearRequiredInfo(): ArgumentBuilder<I, T> {
        this.requiredInfo.clear()
        return this
    }

    /**
     * Sets [Argument.handler]
     */
    fun handler(handler: ArgumentHandler<out T>?): ArgumentBuilder<I, T> {
        this.handler = handler
        return this
    }

    @Deprecated(message = "Will be removed before 1.3 release.")
    private fun buildArgumentType(): ArgumentType<*, out T> {
        if (this.type != null)
            return this.type!!

        return simpleArgumentType(
                transformer as Transformer<SingleInput, T>,
                validator as Validator<SingleInput>,
                possibilities,
                this.typeInfo)
    }

    /**
     * Builds argument.
     */
    fun build(): Argument<T> = Argument(
            id = this.id,
            name = this.name,
            description = this.description,
            isOptional = this.isOptional,
            type = this.buildArgumentType(),
            isMultiple = this.isMultiple,
            defaultValue = this.defaultValue,
            requirements = this.requirements.toList(),
            requiredInfo = this.requiredInfo.toSet(),
            handler = this.handler
    )

}