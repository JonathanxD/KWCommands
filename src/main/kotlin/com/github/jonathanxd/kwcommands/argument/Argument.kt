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
package com.github.jonathanxd.kwcommands.argument

import com.github.jonathanxd.iutils.text.Text
import com.github.jonathanxd.iutils.text.TextComponent
import com.github.jonathanxd.kwcommands.NamedAndAliased
import com.github.jonathanxd.kwcommands.information.RequiredInformation
import com.github.jonathanxd.kwcommands.parser.Input
import com.github.jonathanxd.kwcommands.requirement.Requirement

/**
 * A command argument.
 *
 * To understand difference between [name] and [nameComponent] see
 * [Command][com.github.jonathanxd.kwcommands.command.Command].
 *
 * @property name Argument name to be used in definition, empty string means that argument cannot be defined by name.
 * @property nameComponent Argument name component to be used in definition, empty string means that argument
 * cannot be defined by name.
 * @property alias Aliases to argument.
 * @property description Argument description.
 * @property isOptional Is optional argument.
 * @property argumentType Type of argument value.
 * @property requirements Requirements of this argument.
 * @property requiredInfo Identifications of required information for this argument work.
 */
data class Argument<out T>(
    override val name: String,
    override val nameComponent: TextComponent,
    override val alias: List<String>,
    override val aliasComponent: TextComponent?,
    override val description: TextComponent,
    val isOptional: Boolean,
    val argumentType: ArgumentType<*, T>,
    val requirements: List<Requirement<*, *>>,
    val requiredInfo: Set<RequiredInformation>,
    val handler: ArgumentHandler<out T>? = null
) : NamedAndAliased {

    constructor(
        name: String,
        alias: List<String>,
        description: TextComponent,
        isOptional: Boolean,
        argumentType: ArgumentType<*, T>,
        requirements: List<Requirement<*, *>>,
        requiredInfo: Set<RequiredInformation>,
        handler: ArgumentHandler<out T>? = null
    ) : this(
        name,
        Text.of(name),
        alias,
        null,
        description,
        isOptional,
        argumentType,
        requirements,
        requiredInfo,
        handler
    )

    fun parse(input: Input) = this.argumentType.parse(input)

    companion object {
        @JvmStatic
        fun <T> builder() = ArgumentBuilder<T>()
    }

}