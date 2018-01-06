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

import com.github.jonathanxd.kwcommands.argument.ArgumentType

@FunctionalInterface
interface Transformer<in I : Input, out T> {
    operator fun invoke(value: I): T
}

@FunctionalInterface
interface ArgumentParser<in I : Input, out T> {
    fun parse(input: I, valueOrValidationFactory: ValueOrValidationFactory): ValueOrValidation<T>
}

interface ValueOrValidationFactory {
    fun <T> invalid(): ValueOrValidation<T>
    fun <T> invalid(validation: Validation): ValueOrValidation<T>
    fun <T> value(value: T): ValueOrValidation<T>
}

class ValueOrValidationFactoryImpl(val input: Input,
                                   val argumentType: ArgumentType<*, *>,
                                   val parser: ArgumentParser<*, *>) : ValueOrValidationFactory {
    override fun <T> invalid(): ValueOrValidation<T> =
            ValueOrValidation.Invalid(invalid(input, argumentType, parser))

    override fun <T> invalid(validation: Validation): ValueOrValidation<T> =
            ValueOrValidation.Invalid(validation)

    override fun <T> value(value: @UnsafeVariance T): ValueOrValidation<T> =
            ValueOrValidation.Value(value)
}

sealed class ValueOrValidation<out T> {
    abstract val isValue: Boolean
    abstract val isInvalid: Boolean
    abstract val value: T
    abstract val validation: Validation
    abstract fun <R> mapIfValue(func: (T) -> R): ValueOrValidation<R>

    data class Value<out T>(override val value: T) : ValueOrValidation<T>() {
        override val isValue: Boolean
            get() = true
        override val isInvalid: Boolean
            get() = false
        override val validation: Validation
            get() = throw IllegalStateException("Cannot get validation from value container!")

        override fun <R> mapIfValue(func: (T) -> R): ValueOrValidation<R> =
                Value(func(this.value))
    }

    data class Invalid<T>(override val validation: Validation) : ValueOrValidation<T>() {
        override val isValue: Boolean
            get() = false
        override val isInvalid: Boolean
            get() = true
        override val value: T
            get() = throw IllegalStateException("Cannot get value from Invalid validation container!")

        operator fun plus(invalid: Invalid<T>): Invalid<T> =
                Invalid(validation + invalid.validation)

        override fun <R> mapIfValue(func: (T) -> R): ValueOrValidation<R> =
                Invalid(validation)
    }
}