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
import com.github.jonathanxd.kwcommands.information.RequiredInformation
import com.github.jonathanxd.kwcommands.requirement.Requirement

/**
 * A command argument.
 *
 * @property id Id of the argument.
 * @property name Argument name to be used in definition, empty string means that argument cannot be defined by name.
 * @property description Argument description.
 * @property isOptional Is optional argument.
 * @property type Type of argument value.
 * @property isVarargs Whether the argument is a varargs argument or not. Varargs arguments takes values until
 * [validator] returns `false` to a value, the transformer should return mutable collection, there is a predefined
 * [Transformer] for lists: [com.github.jonathanxd.kwcommands.util.ListTransformer].
 * @property validator Argument value validator.
 * @property transformer Transformer of argument to a object of type [T].
 * @property requirements Requirements of this argument.
 * @property requiredInfo Identifications of required information for this argument work.
 * @property possibilities Possibilities of argument values.
 */
data class Argument<out T>(val id: Any,
                           val name: String,
                           val description: String,
                           val isOptional: Boolean,
                           val type: TypeInfo<out T>,
                           val defaultValue: T?,
                           val isVarargs: Boolean,
                           val validator: Validator,
                           val transformer: Transformer<T>,
                           val possibilities: PossibilitiesFunc,
                           val requirements: List<Requirement<*, *>>,
                           val requiredInfo: Set<RequiredInformation>,
                           val handler: ArgumentHandler<out T>? = null) {
    companion object {
        @JvmStatic
        fun <T> builder() = ArgumentBuilder<T>()
    }

}