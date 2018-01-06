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
package com.github.jonathanxd.kwcommands.reflect.annotation

import com.github.jonathanxd.kwcommands.argument.ArgumentHandler
import com.github.jonathanxd.kwcommands.argument.ArgumentType
import com.github.jonathanxd.kwcommands.reflect.NoneArgumentHandler
import com.github.jonathanxd.kwcommands.reflect.NoneArgumentType
import kotlin.reflect.KClass

/**
 * Argument specification.
 *
 * By default, [argumentType] is determined depending on type of annotated element
 * and [handler] is determined by default by the reflection system based on type and in registered.
 *
 * When added to a field, the field value will be replaced by the value parsed from dispatched commands.
 * This annotation only have effects in fields when the enclosing class is annotated with [Cmd] and there is
 * no function annotated with [CmdHandler] in the class.
 *
 * @property value Identification and name of argument. (defaults to annotated element name)
 * @property alias Alias to argument.
 * @property description Argument description.
 * @property optional Whether this argument is optional
 * @property requirements Requirements of the argument.
 * @property argumentType Type of argument. (by default is determined by Reflection Environment)
 * @property handler Argument handler (if this annotated element is a field, an field setter handler will
 * be used as default handler instead of [NoneArgumentHandler]) (**this property overrides default handler**).
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD)
annotation class Arg(val value: String = "",
                     val alias: Array<String> = [],
                     val description: String = "",
                     val optional: Boolean = false,
                     val requirements: Array<Require> = [],
                     val argumentType: KClass<out () -> ArgumentType<*, *>> = NoneArgumentType::class,
                     val handler: KClass<out ArgumentHandler<*>> = NoneArgumentHandler::class)
