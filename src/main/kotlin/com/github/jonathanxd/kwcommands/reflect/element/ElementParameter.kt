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
package com.github.jonathanxd.kwcommands.reflect.element

import com.github.jonathanxd.iutils.type.TypeInfo
import com.github.jonathanxd.kwcommands.argument.Argument
import com.github.jonathanxd.kwcommands.command.CommandContainer
import com.github.jonathanxd.kwcommands.command.CommandContext
import com.github.jonathanxd.kwcommands.information.Information

/**
 * Parameter specification.
 *
 * @property type Type of parameter value.
 */
sealed class ElementParameter<T>(val type: TypeInfo<T>) {

    /**
     * Argument parameter. An parameter that receives a [command argument][Argument] value.
     *
     * @property argument Backing command argument.
     */
    class ArgumentParameter<T>(val argument: Argument<T>, type: TypeInfo<T>) :
        ElementParameter<T>(type) {
        override fun toString(): String = "ArgumentParameter(argument=$argument, type=$type)"
    }

    /**
     * Information parameter. An parameter that receives a [Information] value.
     *
     * @property id Id of information to receive.
     * @property isOptional If the [Information] is optional, if true, a [Information.EMPTY] will be passed if the
     * information is not present.
     */
    class InformationParameter<T>(
        val id: Information.Id<T>,
        val isOptional: Boolean,
        type: TypeInfo<T>
    ) : ElementParameter<T>(type) {

        /**
         * Component of information.
         */
        val infoComponent: TypeInfo<*>
            get() = this.type.infoComponent

        override fun toString(): String =
            "InformationParameter(id=$id, isOptional=$isOptional, type=$type)"
    }

    /**
     * Context parameter
     */
    object CtxParameter : ElementParameter<CommandContext>(TypeInfo.of(CommandContext::class.java))

    /**
     * Context parameter
     */
    object CmdContainerParameter : ElementParameter<CommandContainer>(TypeInfo.of(CommandContainer::class.java))
}

val TypeInfo<*>.infoComponent: TypeInfo<*>
    get() =
        if (this.typeClass == Information::class.java && this.typeParameters.size == 1)
            this.typeParameters.first()
        else this