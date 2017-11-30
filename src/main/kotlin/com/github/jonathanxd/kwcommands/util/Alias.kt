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
package com.github.jonathanxd.kwcommands.util

import com.github.jonathanxd.kwcommands.argument.ArgumentType
import com.github.jonathanxd.kwcommands.parser.*


typealias ValidatorAlias<I> = (argumentType: ArgumentType<I, *>, value: I) -> Validation
typealias TransformerAlias<I, T> = (value: I) -> T
typealias PossibilitiesFuncAlias = () -> List<Input>

inline fun <I : Input> validator(crossinline func: (argumentType: ArgumentType<I, *>, value: I) -> Validation) =
        object : Validator<I> {
            override fun invoke(argumentType: ArgumentType<I, *>, value: I): Validation = func(argumentType, value)
        }

inline fun <I : Input, T> transformer(crossinline func: (value: I) -> T) =
        object : Transformer<I, T> {
            override fun invoke(value: I): T = func(value)

        }

inline fun possibilitiesFunc(crossinline func: () -> List<Input>) =
        object : Possibilities {
            override fun invoke(): List<Input> =
                    func()
        }


@JvmName("stringValidator")
inline fun validator(crossinline func: (argumentType: ArgumentType<*, *>, value: String) -> Boolean) =
        object : Validator<Input> {
            override fun invoke(argumentType: ArgumentType<Input, *>, value: Input): Validation =
                    (value as? SingleInput)?.let {
                        if (func(argumentType, it.input)) valid()
                        else invalid(value, argumentType, this, null)
                    } ?: invalid(value, argumentType, this, null)

        }

@JvmName("stringTransformer")
inline fun <T> transformer(crossinline func: (value: String) -> T) =
        object : Transformer<SingleInput, T> {
            override fun invoke(value: SingleInput): T = func(value.input)
        }
