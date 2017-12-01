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

import com.github.jonathanxd.iutils.`object`.Either
import com.github.jonathanxd.iutils.text.Text
import com.github.jonathanxd.jwiutils.kt.left
import com.github.jonathanxd.jwiutils.kt.right
import com.github.jonathanxd.kwcommands.argument.ArgumentType
import com.github.jonathanxd.kwcommands.parser.*

fun ListInput.validate(validatorFunc: (Input) -> Validation): Validation =
        this.input.map { validatorFunc(it) }
                .reduce { acc, validation -> acc + validation }


fun MapInput.validate(validatorFunc: (Input) -> Validation): Validation =
        this.input
                .map { (k, v) -> validatorFunc(k) + validatorFunc(v) }
                .reduce { acc, validation -> acc + validation }

fun MapInput.getStringKey(keyName: String): Pair<Input, Input>? =
        this.input.firstOrNull { (k, _) -> k is SingleInput && k.input == keyName }

fun MapInput.get(keyName: String,
                 valueType: InputType<*>,
                 argumentType: ArgumentType<*, *>,
                 validator: Validator<*>): Either<Validation, Pair<Input, Input>> =
        // TODO: Translate
        this.getStringKey(keyName).let {
            return when {
                it == null ->
                    left(invalid(this, argumentType, validator,
                            Text.of("String key '$keyName' not found in map.")))
                it.second.type != valueType ->
                    left(invalid(it.second, argumentType, validator,
                            Text.of("Value of key '$keyName' must be of type $valueType.")))
                else -> right(it)
            }
        }