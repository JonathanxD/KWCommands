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
package com.github.jonathanxd.kwcommands.argument

import com.github.jonathanxd.iutils.kt.get
import com.github.jonathanxd.iutils.text.localizer.Localizer
import com.github.jonathanxd.iutils.text.localizer.TextLocalizer
import com.github.jonathanxd.kwcommands.util.localizeMulti

interface Arguments {

    /**
     * All arguments.
     */
    val all: List<Argument<*>>

    /**
     * Gets all remaining arguments.
     *
     * Commonly returns a list with next arguments to input before dynamic
     * argument handling starts.
     *
     * Example, if first argument is `Type`, and second is `Int`, and the third argument
     * resolution depends on both `Type` and `Int`, this function should return a list with only `Type` and `Int`
     * arguments. Also if second argument depends on the first, only the first should be returned.
     *
     * Unless the command have no arguments, this function must always returns at least one constant argument.
     *
     * If there is no argument that should be dynamic resolved (in other words, if this is a provider of static arguments
     * like [StaticListArguments]), all arguments must be returned.
     */
    fun getRemainingArguments(): List<Argument<*>>

    /**
     * Gets remaining arguments based on [current]. If [current] is empty, the behavior
     * must be the same as [getRemainingArguments()][getRemainingArguments].
     *
     * Even if this is a provider of static arguments (like [StaticListArguments]), this should always
     * return only remaining arguments.
     *
     * Note that [current] **must** only contains arguments that are present in [all] and **always**
     * in the same order as in [all], also the size of [current] **must** be less or equal to size
     * of [all]. Implementations of this function **never** checks if [current] follow these rules, so
     * the caller of the function should always check them.
     */
    fun getRemainingArguments(current: List<ArgumentContainer<*>>): List<Argument<*>>

}

class StaticListArguments(val argumentList: List<Argument<*>>) : Arguments {

    constructor() : this(emptyList())
    constructor(argument: Argument<*>) : this(listOf(argument))

    override val all: List<Argument<*>>
        get() = this.argumentList

    override fun getRemainingArguments(): List<Argument<*>> = this.argumentList
    override fun getRemainingArguments(current: List<ArgumentContainer<*>>): List<Argument<*>> =
        if (current.isEmpty()) this.getRemainingArguments()
        else this.argumentList.filter { curr -> current.none { it.argument == curr } }
}

class StaticListArgumentsBuilder {
    private val arguments = mutableListOf<Argument<*>>()

    fun addArgument(argument: Argument<*>): StaticListArgumentsBuilder {
        this.arguments += argument
        return this
    }

    fun addArguments(arguments: Iterable<Argument<*>>): StaticListArgumentsBuilder {
        this.arguments += arguments
        return this
    }

    fun removeArgument(argument: Argument<*>): StaticListArgumentsBuilder {
        this.arguments -= argument
        return this
    }

    fun removeArguments(arguments: Iterable<Argument<*>>): StaticListArgumentsBuilder {
        this.arguments -= arguments
        return this
    }

    fun setArguments(arguments: Iterable<Argument<*>>): StaticListArgumentsBuilder =
        this.clear().addArguments(arguments)

    fun clear(): StaticListArgumentsBuilder {
        this.arguments.clear()
        return this
    }

    fun build(): StaticListArguments =
        StaticListArguments(this.arguments.toList())
}

inline fun List<Argument<*>>.firstNameMatches(
    localizer: Localizer?,
    matcher: (name: String) -> Boolean
) =
    this.firstOrNull { matcher(it.name) }
            ?: this.firstOrNull {
                it.alias.any(matcher)
                        || (localizer != null && (
                        matcher(localizer[it.nameComponent])
                                || it.aliasComponent?.localizeMulti(localizer)?.any { matcher(it) } == true
                        ))
            }


fun List<Argument<*>>.firstWithName(name: String, localizer: Localizer?) =
    this.firstNameMatches(localizer) { it == name }