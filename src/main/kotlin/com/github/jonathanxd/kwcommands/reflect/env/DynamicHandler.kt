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
package com.github.jonathanxd.kwcommands.reflect.env

import com.github.jonathanxd.iutils.opt.specialized.OptObject
import com.github.jonathanxd.jwiutils.kt.none
import com.github.jonathanxd.jwiutils.kt.some
import com.github.jonathanxd.kwcommands.argument.Argument
import com.github.jonathanxd.kwcommands.argument.ArgumentContainer
import com.github.jonathanxd.kwcommands.argument.ArgumentHandler
import com.github.jonathanxd.kwcommands.command.Command
import com.github.jonathanxd.kwcommands.command.CommandContainer
import com.github.jonathanxd.kwcommands.command.Handler
import com.github.jonathanxd.kwcommands.manager.InformationManager
import com.github.jonathanxd.kwcommands.processor.ResultHandler
import com.github.jonathanxd.kwcommands.reflect.ReflectionHandler
import com.github.jonathanxd.kwcommands.reflect.element.Parameter
import com.github.jonathanxd.kwcommands.util.nameOrId
import java.lang.reflect.Method

/**
 * Dynamic command and argument handler that can resolve the handler lazily, when it is called at the first time. The resolution
 * can also be triggered through [resolve] method.
 */
class DynamicHandler(val name: String,
                     val handlerType: Type,
                     val instanceProvider: ReflectInstanceProvider,
                     val type: Class<*>,
                     val reflectionEnvironment: ReflectionEnvironment) : Handler, ArgumentHandler<Any?> {

    private var handler: OptObject<Handler> = none()
    private var argumentHandler: OptObject<ArgumentHandler<Any?>> = none()

    override fun handle(commandContainer: CommandContainer,
                        informationManager: InformationManager,
                        resultHandler: ResultHandler): Any {

        if (!handler.isPresent)
            this.resolveHandlers(commandContainer.command)

        return handler.value.handle(commandContainer, informationManager, resultHandler)
    }

    @Suppress("UNCHECKED_CAST")
    fun resolveHandlers(command: Command) {
        if (this.handler.isPresent)
            return

        fun fail(): Nothing =
                throw IllegalArgumentException("Cannot resolve element '$name' of type '$handlerType' in '$type' specified in json of command '$command'.")

        this.handler = some(resolveHandler(command, ::fail) as Handler)
    }

    @Suppress("UNCHECKED_CAST")
    fun resolveHandlers(argument: Argument<*>, command: Command) {
        if (this.argumentHandler.isPresent)
            return

        fun fail(): Nothing =
                throw IllegalArgumentException("Cannot resolve element '$name' of type '$handlerType' in '$type' specified in json of command '$command' and argument '$argument'.")

        val hnd = resolveHandler(command, ::fail) as ReflectionHandler

        val arg = (hnd.element.parameters.filterIsInstance<Parameter.ArgumentParameter<*>>().singleOrNull() ?: fail()).argument

        if (arg.nameOrId != argument.nameOrId)
            throw IllegalArgumentException("Handler for argument specified in json for argument '$argument' must have one argument parameter with the same name as argument. (Command: $command).")

        this.argumentHandler = some(hnd as ArgumentHandler<Any?>)
    }

    private fun resolveHandler(command: Command, fail: () -> Nothing): Any {
        return if (handlerType == Type.FIELD_SETTER) {
            val field = type.declaredFields.firstOrNull { it.name == name } ?: fail()

            this.reflectionEnvironment
                    .createSetterHandler(this.instanceProvider(field.declaringClass), field)
        } else {
            val sameName = type.declaredMethods.filter { it.name == name }

            if (sameName.isEmpty()) fail()

            val method: Method = if (sameName.size == 1) {
                sameName.single()
            } else {
                sameName.firstOrNull { it.parameterCount >= command.arguments.size } ?: fail()
            }

            this.reflectionEnvironment.createHandler(this.instanceProvider(method.declaringClass), method)
        }
    }

    override fun handle(argumentContainer: ArgumentContainer<Any?>,
                        commandContainer: CommandContainer,
                        informationManager: InformationManager,
                        resultHandler: ResultHandler): Any {
        if (!argumentHandler.isPresent)
            this.resolveHandlers(argumentContainer.argument, commandContainer.command)

        return this.argumentHandler.value.handle(argumentContainer, commandContainer, informationManager, resultHandler)
    }

    enum class Type {
        METHOD,
        FIELD_SETTER
    }
}