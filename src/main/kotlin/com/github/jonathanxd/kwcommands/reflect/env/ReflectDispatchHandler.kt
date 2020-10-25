/*
 *      KWCommands - New generation of WCommands written in Kotlin <https://github.com/JonathanxD/KWCommands>
 *
 *         The MIT License (MIT)
 *
 *      Copyright (c) 2020 JonathanxD
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
package com.github.jonathanxd.kwcommands.reflect.env

import com.github.jonathanxd.iutils.link.Link
import com.github.jonathanxd.kwcommands.argument.ArgumentContainer
import com.github.jonathanxd.kwcommands.command.CommandContainer
import com.github.jonathanxd.kwcommands.dispatch.DispatchHandler
import com.github.jonathanxd.kwcommands.processor.CommandResult
import com.github.jonathanxd.kwcommands.reflect.ReflectionHandler

class ReflectDispatchHandler(private val link: Link<Any?>) : DispatchHandler {
    override fun handle(result: List<CommandResult>) {
        link.invoke(result)
    }
}

class ReflectFilterDispatchHandler(
    private val link: Link<Any?>,
    private val filter: List<Class<*>>
) : DispatchHandler {
    override fun handle(result: List<CommandResult>) {
        val filtered = result.filter {
            val container = it.container
            val handler: Any? = when (container) {
                is CommandContainer -> container.handler
                is ArgumentContainer<*> -> container.handler
                else -> null
            }

            when (handler) {
                is ReflectionHandler -> handler.element.owner.let { owner -> filter.any { it == owner } }
                is DynamicHandler -> handler.type.let { owner -> filter.any { it == owner } }
                else -> false
            }
        }

        if (filtered.isNotEmpty()) {
            link.invoke(filtered)
        }
    }
}