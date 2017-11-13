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
package com.github.jonathanxd.kwcommands.parser

import com.github.jonathanxd.iutils.opt.specialized.OptObject
import com.github.jonathanxd.kwcommands.argument.Argument

/**
 * Holds the input of argument. Holds and empty [OptObject] when the argument is multiple
 * and no value was provided to it.
 */
// 1.2 dev note: Do we change ListInput and MapInput values to List<Input> and Map<Input, Input> respectively?
sealed class Input

/**
 * Denotes a single input for argument
 */
data class SingleInput(val input: String): Input()

/**
 * Denotes a collection of elements input for argument marked as [multiple][Argument.isMultiple].
 */
data class ListInput(val input: List<Input>): Input()

/**
 * Denotes a map of elements input for argument marked as [multiple][Argument.isMultiple].
 */
data class MapInput(val input: Map<Input, Input>): Input()

/**
 * Denotes no value input for argument marked as [multiple][Argument.isMultiple].
 */
object EmptyInput: Input()