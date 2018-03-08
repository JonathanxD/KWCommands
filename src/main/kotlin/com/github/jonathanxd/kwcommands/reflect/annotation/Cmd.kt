/*
 *      KWCommands - New generation of WCommands written in Kotlin <https://github.com/JonathanxD/KWCommands>
 *
 *         The MIT License (MIT)
 *
 *      Copyright (c) 2018 JonathanxD
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

import com.github.jonathanxd.kwcommands.command.Handler
import com.github.jonathanxd.kwcommands.reflect.NoneHandler
import kotlin.reflect.KClass

/**
 * Annotated on command specification elements, such as classes and functions.
 *
 * The function or class can be also annotated with [DynamicArgs] to use a dynamic argument resolver
 * instead of default resolver.
 *
 * When added to classes, all fields are automatically considered [arguments][Arg], [Exclude] can be use to prevent
 * annotated field being considered an argument of command. This only applies to classes where there are no
 * function annotated with [CmdHandler], this annotation prevents fields from being considered arguments,
 * and uses arguments specified in the function.
 *
 * @property order Command order.
 * @property name Command name.
 * @property description Command description.
 * @property alias Aliases to command.
 * @property parents Path to parent command (if this command is a sub command).
 * @property requirements Command requirements.
 * @property handler Command handler (for functions, defaults to function invocation with
 * corresponding [arguments][Arg], [information][Info] and [command context][Ctx], for classes, defaults to a handler
 * that does nothing, if the class have any function annotated with [CmdHandler], the annotated function
 * will be invoked following same principles specified previously).
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class Cmd(
    val order: Int = 0,
    val name: String = "",
    val description: String = "",
    val alias: Array<String> = [],
    val parents: Array<String> = [],
    val requirements: Array<Require> = [],
    val handler: KClass<out Handler> = NoneHandler::class
)