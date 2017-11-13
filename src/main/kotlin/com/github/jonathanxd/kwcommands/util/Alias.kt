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

import com.github.jonathanxd.kwcommands.argument.*
import com.github.jonathanxd.kwcommands.parser.*


typealias ValidatorAlias = (parsed: List<ArgumentContainer<*>>, current: Argument<*>, value: Input) -> Boolean
typealias TransformerAlias<T> = (parsed: List<ArgumentContainer<*>>, current: Argument<*>, value: Input) -> T
typealias PossibilitiesFuncAlias = (parsed: List<ArgumentContainer<*>>, current: Argument<*>) -> Map<String, List<String>>


inline fun validator(crossinline func: (parsed: List<ArgumentContainer<*>>,
                                        current: Argument<*>,
                                        value: Input) -> Boolean) =
        object : Validator {
            override fun invoke(parsed: List<ArgumentContainer<*>>, current: Argument<*>, value: Input): Boolean =
                    func(parsed, current, value)
        }

inline fun <T> transformer(crossinline func: (parsed: List<ArgumentContainer<*>>,
                                              current: Argument<*>,
                                              value: Input) -> T) =
        object : Transformer<T> {
            override fun invoke(parsed: List<ArgumentContainer<*>>, current: Argument<*>, value: Input): T =
                    func(parsed, current, value)
        }

inline fun possibilitiesFunc(crossinline func: (parsed: List<ArgumentContainer<*>>,
                                                current: Argument<*>) -> Map<String, List<String>>) =
        object : PossibilitiesFunc {
            override fun invoke(parsed: List<ArgumentContainer<*>>, current: Argument<*>): Map<String, List<String>> =
                    func(parsed, current)
        }


@JvmName("stringValidator")
inline fun validator(crossinline func: (parsed: List<ArgumentContainer<*>>,
                                        current: Argument<*>,
                                        value: String) -> Boolean) =
        object : Validator {
            override fun invoke(parsed: List<ArgumentContainer<*>>, current: Argument<*>, value: Input): Boolean =
                    (value as? SingleInput)?.let { func(parsed, current, it.input) } ?: false

        }

@JvmName("stringTransformer")
inline fun <T> transformer(crossinline func: (parsed: List<ArgumentContainer<*>>,
                                              current: Argument<*>,
                                              value: String) -> T) =
        object : Transformer<T> {
            override fun invoke(parsed: List<ArgumentContainer<*>>, current: Argument<*>, value: Input): T =
                    func(parsed, current, (value as SingleInput).input)
        }
