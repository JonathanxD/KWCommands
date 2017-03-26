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
package com.github.jonathanxd.kwcommands.reflect

import com.github.jonathanxd.kwcommands.command.CommandContainer
import com.github.jonathanxd.kwcommands.command.Handler
import com.github.jonathanxd.kwcommands.exception.InformationMissingException
import com.github.jonathanxd.kwcommands.manager.InformationManager
import com.github.jonathanxd.kwcommands.reflect.element.Element
import com.github.jonathanxd.kwcommands.reflect.element.Parameter

class ReflectionHandler(val element: Element) : Handler {

    override fun handle(commandContainer: CommandContainer, informationManager: InformationManager): Any {
        val link = element.elementLink
        val args = mutableListOf<Any?>()

        element.parameters.forEach { parameter ->
            when(parameter) {
                is Parameter.ArgumentParameter<*> -> {
                    args += commandContainer.arguments.find { parameter.argument.id == it.argument.id }?.value
                }
                is Parameter.InformationParameter<*> -> {
                    val information = informationManager.find(parameter.id, parameter.type)

                    if(!parameter.isOptional && information == null)
                        throw InformationMissingException("Required information with id ${parameter.id} and of type ${parameter.type} is missing!")
                    args += information?.value
                }
            }
        }

        return link(*args.toTypedArray())
    }

}