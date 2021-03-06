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
package com.github.jonathanxd.kwcommands.reflect

import com.github.jonathanxd.iutils.link.Invokables
import com.github.jonathanxd.iutils.link.Link
import com.github.jonathanxd.iutils.link.Links
import com.github.jonathanxd.kwcommands.argument.ArgumentContainer
import com.github.jonathanxd.kwcommands.argument.ArgumentHandler
import com.github.jonathanxd.kwcommands.command.CommandContainer
import com.github.jonathanxd.kwcommands.command.CommandContext
import com.github.jonathanxd.kwcommands.command.Handler
import com.github.jonathanxd.kwcommands.information.Information
import com.github.jonathanxd.kwcommands.information.InformationProviders
import com.github.jonathanxd.kwcommands.processor.ResultHandler
import com.github.jonathanxd.kwcommands.reflect.element.*
import java.lang.invoke.MethodHandles
import java.lang.reflect.Field
import java.lang.reflect.Modifier

/**
 * Adapt command invocation to a element invocation.
 */
class ReflectionHandler constructor(val element: Element) : Handler, ArgumentHandler<Any> {


    @Suppress("UNCHECKED_CAST")
    private val link: Link<Any?> = when (element) {
        is FieldElement -> linkField(element.field)
        is MethodElement -> Links.ofInvokable(Invokables.fromMethodHandle(LOOKUP.unreflect(element.method)))
        is ConstructorElement -> Links.ofInvokable(
            Invokables.fromMethodHandle(
                LOOKUP.unreflectConstructor(
                    element.ctr
                )
            )
        )
        is InvokableElement -> Links.ofInvokable(element.invokable)
        is EmptyElement -> Links.ofInvokable<Any?> { Unit }
    }.let { if (element.instance != null) it.bind(element.instance) else it }

    @Suppress("UNCHECKED_CAST")
    override fun handle(
        commandContainer: CommandContainer,
        informationProviders: InformationProviders,
        resultHandler: ResultHandler
    ): Any {

        val args = mutableListOf<Any?>()

        element.parameters.forEach { parameter ->
            when (parameter) {
                is ElementParameter.ArgumentParameter<*> -> {
                    args += commandContainer.arguments.find { parameter.argument.name == it.argument.name }
                        ?.value
                }
                is ElementParameter.InformationParameter<*> -> {
                    val information =
                        informationProviders.find(parameter.id/*, parameter.infoComponent*/)

                    if (!parameter.isOptional && information == null) {
                        args.add(null)
                    } else {
                        args += if (parameter.type.typeClass != Information::class.java) {
                            information?.value
                        } else {
                            information ?: Information.EMPTY
                        }
                    }
                }
                is ElementParameter.CtxParameter -> {
                    args += CommandContext(commandContainer, informationProviders, resultHandler)
                }
                is ElementParameter.CmdContainerParameter -> {
                    args += commandContainer
                }
            }
        }

        if (resultHandler.shouldCancel())
            return Unit

        return link(*args.toTypedArray()) ?: Unit
    }

    override fun handle(
        argumentContainer: ArgumentContainer<Any>,
        commandContainer: CommandContainer,
        informationProviders: InformationProviders,
        resultHandler: ResultHandler
    ): Any {

        val parameter = element.parameters.first()

        return when (parameter) {
            is ElementParameter.ArgumentParameter<*> -> {
                argumentContainer.value?.let {
                    link.invoke(argumentContainer.value) ?: Unit
                } ?: Unit
            }
            is ElementParameter.InformationParameter<*> -> {
                val information =
                    informationProviders.find(parameter.id/*, parameter.infoComponent*/)

                if (!parameter.isOptional && information == null) {
                    Unit
                } else {
                    if (parameter.type.typeClass != Information::class.java) {
                        link.invoke(information?.value) ?: Unit
                    } else {
                        link.invoke(information ?: Information.EMPTY) ?: Unit
                    }
                }
            }
            is ElementParameter.CtxParameter -> {
                link.invoke(CommandContext(commandContainer, informationProviders, resultHandler))
                        ?: Unit
            }
            is ElementParameter.CmdContainerParameter -> {
                link.invoke(commandContainer) ?: Unit
            }
        }
    }

    companion object {
        private val LOOKUP = MethodHandles.lookup()

        fun linkField(field: Field): Link<Any?> =
            if (field.isAccessible || Modifier.isPublic(field.modifiers)) {
                Links.ofInvokable(Invokables.fromMethodHandle(LOOKUP.unreflectSetter(field)))
            } else {
                field.declaringClass.getDeclaredMethod("set${field.name.capitalize()}", field.type)
                    .let {
                        if (it == null || (!Modifier.isPublic(it.modifiers) && !it.isAccessible))
                            throw IllegalArgumentException("Accessible setter of field $field was not found!")
                        else {
                            Links.ofInvokable(Invokables.fromMethodHandle<Any?>(LOOKUP.unreflect(it)))
                        }
                    }
            }
    }

}