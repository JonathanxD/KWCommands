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
package com.github.jonathanxd.kwcommands.interceptor

import com.github.jonathanxd.kwcommands.command.CommandContainer
import com.github.jonathanxd.kwcommands.processor.Result

/**
 * Intercept command handling. A command interceptor can intercept, block and modify a [CommandContainer] and
 * change its handler.
 *
 * Obs: Changing the [CommandContainer.handler] does not guarantee that the new handler will be
 * called instead of the original handler.
 */
interface CommandInterceptor {

    /**
     * Called before command handling.
     *
     * @param original Original command container.
     * @param current Current command container (modified or not).
     * @return New [CommandContainer], if this method return a null container, the command will not be
     * processed and subsequent interceptors will not be called.
     */
    fun pre(original: CommandContainer, current: CommandContainer): CommandContainer?

    /**
     * Called after command handling.
     *
     * @param original Original command container
     * @param final Final command container.
     * @param result Result of handler invocation.
     */
    fun post(original: CommandContainer, final: CommandContainer, result: Result)

}