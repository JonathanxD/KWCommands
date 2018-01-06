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

data class Validation(val invalids: List<ValidatedElement>) {

    constructor(vararg validations: Validation) :
            this(validations
                    .map { it.invalids }
                    .fold(mutableListOf<ValidatedElement>()) { acc, r -> acc.addAll(r); acc })


    operator fun plus(validation: Validation): Validation =
            Validation(this.invalids + validation.invalids)

    val isValid get() = this.invalids.isEmpty()
    val isInvalid get() = this.invalids.isNotEmpty()
}

data class ValidatedElement(val input: Input,
                            val argumentType: ArgumentType<*, *>,
                            val parser: ArgumentParser<*, *>,
                            val inputType: InputType<*>)

fun valid(): Validation = VALID
fun validation(invalid: ValidatedElement): Validation =
        Validation(listOf(invalid))

fun validation(validations: List<Validation>): Validation =
        Validation(validations
                .map { it.invalids }
                .fold(mutableListOf<ValidatedElement>()) { acc, r -> acc.addAll(r); acc })

fun invalid(input: Input,
            argumentType: ArgumentType<*, *>,
            parser: ArgumentParser<*, *>): Validation =
        Validation(listOf(validatedElement(input, argumentType, parser, argumentType.inputType)))

fun validatedElement(input: Input,
                     argumentType: ArgumentType<*, *>,
                     parser: ArgumentParser<*, *>,
                     supported: InputType<*>): ValidatedElement =
        ValidatedElement(input, argumentType, parser, supported)

val VALID = Validation(emptyList())