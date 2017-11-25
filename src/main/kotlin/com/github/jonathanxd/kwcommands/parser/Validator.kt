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

import com.github.jonathanxd.iutils.text.Text
import com.github.jonathanxd.iutils.text.TextComponent
import com.github.jonathanxd.kwcommands.argument.Argument
import com.github.jonathanxd.kwcommands.argument.ArgumentContainer
import java.util.*

/**
 * Validator of command inputs. The [Validation] instance specifies which values are invalid and why
 * are invalid.
 */
interface Validator<in I: Input> {
    val name: TextComponent
        get() = Text.of(this::class.java.simpleName)

    operator fun invoke(value: I): Validation
}
/*

interface MapValidator : Validator {
    fun get()
}

abstract class SingleValidator(val expectedText: TextComponent,
                               val invalidText: TextComponent) : Validator {

    abstract fun validateSingle(parsed: List<ArgumentContainer<*>>,
                                current: Argument<*>,
                                value: SingleInput): Boolean

    override fun invoke(parsed: List<ArgumentContainer<*>>,
                        current: Argument<*>,
                        value: Input): Validation {
        if (value !is SingleInput)
            return invalid(value, this, expectedText, SINGLE_TYPE)

        if (!this.validateSingle(parsed, current, value))
            return invalid(value, this, invalidText, SINGLE_TYPE)

        return valid()
    }

    companion object {
        private val SINGLE_TYPE = Collections.unmodifiableList(listOf(SingleInputType))
    }
}*/
