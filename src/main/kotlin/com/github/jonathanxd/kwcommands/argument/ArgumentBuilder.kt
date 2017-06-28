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

import com.github.jonathanxd.iutils.type.TypeInfo
import com.github.jonathanxd.kwcommands.requirement.Requirement

/**
 * Builder of [Argument].
 */
class ArgumentBuilder<T> {

    private lateinit var id: Any
    private var isOptional: Boolean = false
    private lateinit var type: TypeInfo<out T>
    private var defaultValue: T? = null
    private lateinit var validator: (String) -> Boolean
    private lateinit var transformer: (String) -> T
    private val possibilities = mutableListOf<String>()
    private val requirements = mutableListOf<Requirement<*, *>>()
    private var handler: ArgumentHandler<out T>? = null

    /**
     * Sets [Argument.id]
     */
    fun id(id: Any): ArgumentBuilder<T> {
        this.id = id
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
     * Sets [Argument.type]
     */
    fun type(type: TypeInfo<out T>): ArgumentBuilder<T> {
        this.type = type
        return this
    }


    /**
     * Sets [Argument.defaultValue]
     */
    fun defaultValue(defaultValue: T?): ArgumentBuilder<T> {
        this.defaultValue = defaultValue
        return this
    }

    /**
     * Sets [Argument.validator]
     */
    fun validator(validator: (String) -> Boolean): ArgumentBuilder<T> {
        this.validator = validator
        return this
    }

    /**
     * Sets [Argument.transformer]
     */
    fun transformer(transformer: (String) -> T): ArgumentBuilder<T> {
        this.transformer = transformer
        return this
    }

    /**
     * Adds [Argument.possibilities]
     */
    fun addPossibilities(possibilities: List<String>): ArgumentBuilder<T> {
        this.possibilities.addAll(possibilities)
        return this
    }

    /**
     * Adds a [Possibility][Argument.possibilities]
     */
    fun addPossibility(possibility: String): ArgumentBuilder<T> {
        this.possibilities.add(possibility)
        return this
    }

    /**
     * Removes a [Possibility][Argument.possibilities]
     */
    fun removePossibility(possibility: String): ArgumentBuilder<T> {
        this.possibilities.remove(possibility)
        return this
    }

    /**
     * Clear [Argument.possibilities]
     */
    fun clearPossibilities(): ArgumentBuilder<T> {
        this.possibilities.clear()
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
            id = this.id,
            isOptional = this.isOptional,
            type = this.type,
            defaultValue = this.defaultValue,
            validator = this.validator,
            transformer = this.transformer,
            possibilities = this.possibilities,
            requirements = this.requirements,
            handler = this.handler
    )

}