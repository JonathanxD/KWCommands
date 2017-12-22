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
package com.github.jonathanxd.kwcommands.reflect.util

import com.github.jonathanxd.iutils.reflection.Reflection
import com.github.jonathanxd.iutils.text.TextUtil
import com.github.jonathanxd.iutils.type.TypeInfo
import com.github.jonathanxd.kwcommands.argument.Argument
import com.github.jonathanxd.kwcommands.argument.ArgumentHandler
import com.github.jonathanxd.kwcommands.argument.Arguments
import com.github.jonathanxd.kwcommands.argument.StaticListArguments
import com.github.jonathanxd.kwcommands.command.Command
import com.github.jonathanxd.kwcommands.command.Handler
import com.github.jonathanxd.kwcommands.information.Information
import com.github.jonathanxd.kwcommands.information.RequiredInformation
import com.github.jonathanxd.kwcommands.manager.CommandManager
import com.github.jonathanxd.kwcommands.reflect.None
import com.github.jonathanxd.kwcommands.reflect.ReflectionHandler
import com.github.jonathanxd.kwcommands.reflect.annotation.*
import com.github.jonathanxd.kwcommands.reflect.element.Element
import com.github.jonathanxd.kwcommands.reflect.env.ReflectionEnvironment
import com.github.jonathanxd.kwcommands.requirement.Requirement
import com.github.jonathanxd.kwcommands.requirement.RequirementTester
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Field
import java.lang.reflect.Method
import kotlin.reflect.KClass

/**
 * Gets singleton instance of a [KClass]
 */
@Suppress("UNCHECKED_CAST")
fun <T : Any> KClass<T>.get(): T? =
        if (None::class.java.isAssignableFrom(this.java)) null
        else this.objectInstance ?: try {
            this.java.getDeclaredField("INSTANCE").get(null) as T
        } catch (e: Throwable) {
            Reflection.getInstance(this.java)
                    ?: throw IllegalStateException("Provided class is not a valid singleton class: $this. A Singleton class must be a Kotlin object or a class with a static non-null 'INSTANCE' field.", e)
        }

/**
 * Gets singleton instance of a [KClass]
 */
@Suppress("UNCHECKED_CAST")
fun <T : Any> KClass<out T>.get(base: Class<T>, baseValue: () -> T?): T? =
        if (None::class.java.isAssignableFrom(this.java)) null
        else this.objectInstance ?: try {
            val ctr = this.java.getDeclaredConstructor(base)
            val value = baseValue()
            if (value != null) {
                ctr.newInstance(value)
            } else null
        } catch (e: Throwable) {
            null
        } ?: try {
            this.java.getDeclaredConstructor().newInstance()
        } catch (e: Throwable) {
            null
        } ?: Reflection.getInstance(this.java)
                ?: throw IllegalStateException("Provided class is not a valid singleton class: $this. A Singleton class must be a Kotlin object or a class with a static non-null 'INSTANCE' field.")

/**
 * Resolve parent commands.
 *
 * @param manager Manager to lookup for parent commands.
 * @param owner Owner of owner commands.
 */
fun Cmd.resolveParents(manager: CommandManager, owner: Any?) =
        this.parents.let {
            if (it.isEmpty()) null else {
                var cmd = manager.getCommand(it.first(), owner)
                        ?: return null

                if (it.size > 1) {
                    for (x in it.copyOfRange(1, it.size)) {
                        cmd = manager.getSubCommand(cmd, x) ?: return null
                    }
                }

                cmd
            }
        }

/**
 * Resolve parent commands.
 *
 * @param manager Manager to lookup for parent commands.
 * @param owner Owner of owner commands.
 * @param other List to lookup for parent commands if it is not registered in [manager].
 */
fun Cmd.resolveParents(manager: CommandManager, owner: Any?, other: List<Command>) =
        this.parents.let {
            if (it.isEmpty()) null else {
                var cmd = manager.getCommand(it.first(), owner)
                        ?: other.find { (_, _, cmdName) -> cmdName == it.first() }
                        ?: return null

                if (it.size > 1) {
                    for (x in it.copyOfRange(1, it.size)) {
                        cmd = manager.getSubCommand(cmd, x)
                                ?: return null
                    }
                }

                cmd
            }
        }

/**
 * Create commands instance from [Cmd] annotation.
 */
fun Cmd.toKCommand(manager: CommandManager,
                   handler: Handler?,
                   superCommand: Command?,
                   arguments: List<Argument<*>>,
                   reqInfo: Set<RequiredInformation>,
                   owner: Any?,
                   annotatedElement: AnnotatedElement): Command {
    val order = this.order
    val name = this.getName(annotatedElement)
    val alias = this.alias
    val description = this.description
    val parent = this.resolveParents(manager, owner)
    val argumentsInstance =
            annotatedElement.getDeclaredAnnotation(DynamicArgs::class.java)?.value?.get() as? Arguments
                    ?: StaticListArguments(arguments)

    val cmd = Command(parent = parent ?: superCommand,
            order = order,
            name = name,
            description = TextUtil.parse(description),
            handler = handler,
            arguments = argumentsInstance,
            requirements = this.getRequirements(),
            requiredInfo = reqInfo,
            alias = alias.toList())

    cmd.parent?.addSubCommand(cmd)
    return cmd
}

/**
 * Convert [Require] annotation to [Requirement] specification.
 */
fun Array<out Require>.toSpecs(): List<Requirement<*, *>> =
        this.map {
            @Suppress("UNCHECKED_CAST")
            Requirement(it.data,
                    it.subject.let { Information.Id(it.typeInfo, it.tags) },
                    TypeInfo.of(String::class.java),
                    it.testerType.get() as RequirementTester<Any, String>)
        }

/**
 * Gets requirements of [Cmd].
 */
fun Cmd.getRequirements(): List<Requirement<*, *>> = this.requirements.toSpecs()

/**
 * Gets handler of [Cmd] (or null if default)
 */
fun Cmd.getHandlerOrNull(): Handler? =
        this.handler.get()

/**
 * Gets handler of [Cmd] (or null if default)
 */
fun Cmd.getClassHandlerOrNull(klass: Class<*>,
                              reflectionEnvironment: ReflectionEnvironment,
                              elementFactory: (method: Method) -> Element): Handler? =
        this.getHandlerOrNull() ?: klass.methods.firstOrNull { it.isAnnotationPresent(CmdHandler::class.java) }?.let {
            reflectionEnvironment.resolveHandler(elementFactory(it)) as Handler
        }

/**
 * Gets handler of [Cmd] or create a [ReflectionHandler] if not present.
 */
fun Cmd.getHandler(element: Element, reflectionEnvironment: ReflectionEnvironment): Handler =
        this.handler.get() ?: reflectionEnvironment.resolveHandler(element) as Handler

/**
 * Gets handler of [Arg] (or null if default)
 */
fun Arg.getHandlerOrNull(): ArgumentHandler<*>? =
        this.handler.get()

/**
 * Gets handler of [Arg] or create a [ReflectionHandler] if not present.
 */
fun Arg.getHandler(element: Element, reflectionEnvironment: ReflectionEnvironment): ArgumentHandler<*> =
        this.handler.get() ?: reflectionEnvironment.resolveHandler(element) as ArgumentHandler<*>

/**
 * Prepare commands to registration.
 *
 * This function fixes missing sub-commands (only if the sub-command is in the list).
 *
 * If the super command of a [KCommand] is already registered (not the copy of the command)
 * the `sub-command fix` will affect the registered command and the sub-command will be available
 * in all [CommandManager] that the super command is registered.
 *
 * `Sub-commands fix` is the term that we use to say that all sub-commands that are not registered
 * in their [parent command][com.github.jonathanxd.kwcommands.command.Command.parent] will be registered
 * using [com.github.jonathanxd.kwcommands.command.Command.addSubCommand] function of [parent command][com.github.jonathanxd.kwcommands.command.Command.parent].
 *
 * @return A list with main commands only.
 */
fun List<Command>.prepareCommands(): List<Command> =
        this.filter {
            // We can split it in a onEach operation, but I want to avoid the overhead.
            // if this code becomes bigger and complex, move it to onEach operation (before filter operation).
            val parent = it.parent
            if (parent != null && !parent.subCommands.contains(it))
                parent.addSubCommand(it)
            // /onEach
            it.parent == null
        }

/**
 * Gets path of command.
 */
fun Cmd.getPath(annotatedElement: AnnotatedElement): Array<String> {
    return this.parents + this.getName(annotatedElement)
}

fun Cmd.getName(annotatedElement: AnnotatedElement) =
        if (this.name.isNotEmpty()) this.name else
            when (annotatedElement) {
                is Class<*> -> annotatedElement.simpleName.decapitalize()
                is Field -> annotatedElement.name
                is Method -> annotatedElement.name
                else -> throw IllegalArgumentException("@Cmd requires a name if the annotated element is not a class, field or method.")
            }