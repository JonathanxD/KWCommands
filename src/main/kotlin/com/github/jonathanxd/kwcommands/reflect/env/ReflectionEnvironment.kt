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

import com.github.jonathanxd.iutils.reflection.Invokables
import com.github.jonathanxd.iutils.reflection.Links
import com.github.jonathanxd.iutils.type.Primitive
import com.github.jonathanxd.iutils.type.TypeInfo
import com.github.jonathanxd.iutils.type.TypeUtil
import com.github.jonathanxd.kwcommands.argument.ArgumentHandler
import com.github.jonathanxd.kwcommands.command.CommandName
import com.github.jonathanxd.kwcommands.command.Handler
import com.github.jonathanxd.kwcommands.exception.CommandNotFoundException
import com.github.jonathanxd.kwcommands.information.Information
import com.github.jonathanxd.kwcommands.manager.CommandManager
import com.github.jonathanxd.kwcommands.reflect.CommandFactoryQueue
import com.github.jonathanxd.kwcommands.reflect.None
import com.github.jonathanxd.kwcommands.reflect.ReflectionHandler
import com.github.jonathanxd.kwcommands.reflect.annotation.*
import com.github.jonathanxd.kwcommands.reflect.element.Element
import com.github.jonathanxd.kwcommands.reflect.element.Parameter
import com.github.jonathanxd.kwcommands.reflect.util.getPath
import com.github.jonathanxd.kwcommands.reflect.util.sort
import com.github.jonathanxd.kwcommands.requirement.RequirementTester
import java.lang.invoke.MethodHandles
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.*
import kotlin.reflect.KClass
import com.github.jonathanxd.kwcommands.argument.Argument
import com.github.jonathanxd.kwcommands.command.Command
import com.github.jonathanxd.kwcommands.requirement.Requirement

/**
 * Reflection helper environment.
 *
 * Dependency resolution is made via [CommandFactoryQueue].
 *
 * @property manager Manager used to resolve parent commands.
 */
class ReflectionEnvironment(val manager: CommandManager) {

    private val argumentTypes = mutableMapOf<TypeInfo<*>, ArgumentType<*>>()


    fun <T> set(type: TypeInfo<T>, argumentType: ArgumentType<T>?) {
        if (argumentType == null)
            this.argumentTypes.remove(type)
        else {
            this.argumentTypes.put(type, argumentType)
            Primitive.unbox(type.typeClass)?.let { this.argumentTypes.put(TypeInfo.of(it), argumentType) }
            Primitive.box(type.typeClass)?.let { this.argumentTypes.put(TypeInfo.of(it), argumentType) }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> get(type: TypeInfo<T>): ArgumentType<T> =
            (this.argumentTypes.getArgumentType(type)) ?: getGlobalArgumentType(type)
                    ?: throw IllegalArgumentException("Not registered argument type: $type.")

    private val LOOKUP = MethodHandles.lookup()

    fun registerCommands(list: List<Command>, owner: Any) {
        list.prepareCommands().forEach { this.manager.registerCommand(it, owner) }
    }

    fun <T : Any> fromClass(klass: KClass<T>, instanceProvider: (Class<*>) -> Any, owner: Any?): List<Command> {
        return this.fromClass(klass.java, instanceProvider, owner)
    }

    fun <T : Any> fromClass(klass: KClass<T>, instanceProvider: (Class<*>) -> Any, owner: Any?, queue: CommandFactoryQueue): List<Command> {
        return this.fromClass(klass.java, instanceProvider, owner, queue)
    }

    fun <T> fromClass(klass: Class<T>, instanceProvider: (Class<*>) -> Any, owner: Any?): List<Command> {
        val queue = CommandFactoryQueue()

        fromClass(klass, instanceProvider, owner, queue)

        return queue.commands
    }

    fun <T> fromClass(klass: Class<T>, instanceProvider: (Class<*>) -> Any?, owner: Any?, queue: CommandFactoryQueue): List<Command> {

        val instance = klass.cast(instanceProvider(klass))

        val command = klass.getDeclaredAnnotation(Cmd::class.java)!!

        val kCommand = command.toKCommand(manager, command.getHandlerOrNull(), null, klass.declaredFields.filter {
            !it.isAnnotationPresent(Exclude::class.java)
                    && ((Modifier.isStatic(it.modifiers) && instance == null) || (!Modifier.isStatic(it.modifiers) && instance != null))
        }.map { fromField(instance, it) }, owner)

        queue.queueCommand(klass, command.name, {
            kCommand
        }, Checker(this.manager, owner, command), { command.getPath().joinToString(separator = " ") })

        fromMethodsOfClass(klass, instance, kCommand, owner, queue)

        klass.declaredClasses.forEach {
            if (it.isAnnotationPresent(Cmd::class.java))
                fromClass(it, instanceProvider, owner, queue)
        }

        return queue.commands
    }

    fun <T> fromMethodsOfClass(klass: Class<T>, instance: T?, superCommand: Command?, owner: Any?): List<Command> =
            CommandFactoryQueue().apply {
                fromMethodsOfClass(klass, instance, superCommand, owner, this)
            }.commands

    fun <T> fromMethodsOfClass(klass: Class<T>, instance: T?, superCommand: Command?, owner: Any?, queue: CommandFactoryQueue) {
        klass.declaredMethods
                .filter {
                    it.isAnnotationPresent(Cmd::class.java)
                            && ((Modifier.isStatic(it.modifiers) && instance == null) || (!Modifier.isStatic(it.modifiers) && instance != null))
                }
                .sort()
                .map { fromMethod(instance, it, superCommand, owner, queue) }
    }


    // Fields are only translated to arguments
    fun fromField(instance: Any?, field: Field): Argument<*> {
        val type = TypeUtil.toTypeInfo(field.genericType)

        var annotated: AnnotatedElement? = null

        val link = if (field.isAccessible || Modifier.isPublic(field.modifiers)) {
            annotated = field
            Links.ofInvokable(Invokables.fromMethodHandle<Any?>(LOOKUP.unreflectSetter(field)))
        } else {
            field.declaringClass.getDeclaredMethod("set${field.name.capitalize()}", field.type).let {
                if (it == null || (!Modifier.isPublic(it.modifiers) && !it.isAccessible))
                    throw IllegalArgumentException("Accessible setter of field $field was not found!")
                else {
                    annotated = it
                    Links.ofInvokable(Invokables.fromMethodHandle<Any?>(LOOKUP.unreflect(it)))
                }
            }
        }.let {
            if (instance != null) it.bind(instance) else it
        }

        var karg: Argument<Any>? = null

        val parameters = field.let {
            val argumentAnnotation: Arg? = field.getDeclaredAnnotation(Arg::class.java)
            val infoAnnotation = it.getDeclaredAnnotation(Info::class.java)

            val id = argumentAnnotation?.value ?: it.name
            val typeIsOpt = type.classLiteral == TypeInfo.of(Optional::class.java).classLiteral
            val isOptional = argumentAnnotation?.optional ?: typeIsOpt

            val argumentType by lazy {
                this.get(type)
            }

            val possibilities = argumentAnnotation?.possibilities?.get()?.invoke() ?: if (this.argumentTypes.containsKey(type)) argumentType.possibilities else emptyList()
            val transformer = argumentAnnotation?.transformer?.get() ?: argumentType.transformer
            val validator = argumentAnnotation?.validator?.get() ?: argumentType.validator
            val defaultValue: Any? = if (this.argumentTypes.containsKey(type))
                argumentType.defaultValue
            else if (typeIsOpt) Optional.empty<Any>() else null

            @Suppress("UNCHECKED_CAST")
            karg = Argument(
                    id = id,
                    isOptional = isOptional,
                    possibilities = possibilities,
                    transformer = transformer,
                    validator = validator,
                    defaultValue = defaultValue,
                    type = type,
                    handler = argumentAnnotation?.getHandlerOrNull() as? ArgumentHandler<out Any>
            )

            if (infoAnnotation != null) {
                val infoIsOptional = infoAnnotation.isOptional
                val infoId = infoAnnotation.value.let { Information.Id(it.value.java, it.tags) }

                return@let Parameter.InformationParameter(
                        id = infoId,
                        isOptional = infoIsOptional,
                        type = type
                )
            } else {

                @Suppress("UNCHECKED_CAST")
                return@let Parameter.ArgumentParameter(karg!!, type as TypeInfo<Any>)
            }

        }

        return karg!!.let {
            if(it.handler == null)
                it.copy(handler = ReflectionHandler(Element(link, listOf(parameters))))
            else it
        }
    }

    fun fromMethod(instance: Any?, method: Method, superCommand: Command?, owner: Any?): Command {
        val queue = CommandFactoryQueue()

        fromMethod(instance, method, superCommand, owner, queue)

        return queue.commands.first()
    }

    fun fromMethod(instance: Any?, method: Method, superCommand: Command?, owner: Any?, queue: CommandFactoryQueue) {
        val command = method.getDeclaredAnnotation(Cmd::class.java)

        val link = Links.ofInvokable(Invokables.fromMethodHandle<Any?>(LOOKUP.unreflect(method))).let {
            if (instance != null) it.bind(instance) else it
        }

        val arguments = mutableListOf<Argument<*>>()

        val parameters = method.parameters.map {
            val argumentAnnotation = it.getDeclaredAnnotation(Arg::class.java)
            val infoAnnotation = it.getDeclaredAnnotation(Info::class.java)

            val type = TypeUtil.toTypeInfo(it.parameterizedType)

            if (infoAnnotation != null) {

                val isOptional = infoAnnotation.isOptional
                val id = infoAnnotation.value.let { Information.Id(it.value.java, it.tags) }


                return@map Parameter.InformationParameter(
                        id = id,
                        isOptional = isOptional,
                        type = type
                )
            } else if (argumentAnnotation != null) {

                val id = argumentAnnotation.value
                val isOptional = argumentAnnotation.optional

                val argumentType by lazy {
                    this.get(type)
                }

                val possibilities = argumentAnnotation.possibilities.get()?.invoke() ?: if (this.argumentTypes.containsKey(type)) argumentType.possibilities else emptyList()
                val transformer = argumentAnnotation.transformer.get() ?: argumentType.transformer
                val validator = argumentAnnotation.validator.get() ?: argumentType.validator
                val defaultValue = if (this.argumentTypes.containsKey(type)) argumentType.defaultValue else null

                val argument = Argument(
                        id = id,
                        isOptional = isOptional,
                        possibilities = possibilities,
                        transformer = transformer,
                        validator = validator,
                        defaultValue = defaultValue,
                        type = type)

                arguments += argument

                @Suppress("UNCHECKED_CAST")
                return@map Parameter.ArgumentParameter(argument, type as TypeInfo<Any>)
            } else {
                throw IllegalStateException("Missing annotation for parameter: $it")
            }
        }

        queue.queueCommand(method, command.name, { cmds ->
            val superCmd = command.resolveParents(this.manager, owner, cmds) ?: superCommand

            command.toKCommand(manager, command.getHandler(Element(link, parameters)), superCmd, arguments, owner)
        }, Checker(this.manager, owner, command), { command.getPath().joinToString(separator = " ") })

    }

    fun copy(manager: CommandManager = this.manager) = ReflectionEnvironment(manager).also {
        it.argumentTypes.putAll(this.argumentTypes)
    }

    companion object {
        private val GLOBAL = mutableMapOf<TypeInfo<*>, ArgumentType<*>>()

        fun <T> MutableMap<TypeInfo<*>, ArgumentType<*>>.set(type: TypeInfo<T>, argumentType: ArgumentType<T>?) =
                if (argumentType == null)
                    this.remove(type)
                else {
                    val put = this.put(type, argumentType)
                    Primitive.unbox(type.typeClass)?.let { this.put(TypeInfo.of(it), argumentType) }
                    Primitive.box(type.typeClass)?.let { this.put(TypeInfo.of(it), argumentType) }
                    put
                }

        @Suppress("UNCHECKED_CAST")
        fun <T> MutableMap<TypeInfo<*>, ArgumentType<*>>.getArgumentType(type: TypeInfo<T>): ArgumentType<T>? =
                (if (this.containsKey(type))
                    this[type] as? ArgumentType<T>
                else this.let {
                    it.entries.forEach { (k, v) ->
                        if (k.related.isEmpty() && k.classLiteral == type.classLiteral)
                            return@let v as? ArgumentType<T>
                    }
                    null
                })

        fun <T> setGlobal(type: TypeInfo<T>, argumentType: ArgumentType<T>?) = GLOBAL.set(type, argumentType)
        fun <T> getGlobalArgumentType(type: TypeInfo<T>) = GLOBAL.getArgumentType(type)

        init {
            // Data types
            GLOBAL.set(TypeInfo.of(Short::class.java), ArgumentType({ it.toShortOrNull() != null }, String::toShort, emptyList(), 0))
            GLOBAL.set(TypeInfo.of(Char::class.java), ArgumentType({ it.length == 1 }, { it[0] }, emptyList(), 0.toChar()))
            GLOBAL.set(TypeInfo.of(Byte::class.java), ArgumentType({ it.toByteOrNull() != null }, String::toByte, emptyList(), 0))
            GLOBAL.set(TypeInfo.of(Int::class.java), ArgumentType({ it.toIntOrNull() != null }, String::toInt, emptyList(), 0))
            GLOBAL.set(TypeInfo.of(Float::class.java), ArgumentType({ it.toFloatOrNull() != null }, String::toFloat, emptyList(), 0.0F))
            GLOBAL.set(TypeInfo.of(Double::class.java), ArgumentType({ it.toDoubleOrNull() != null }, String::toDouble, emptyList(), 0.0))
            GLOBAL.set(TypeInfo.of(Long::class.java), ArgumentType({ it.toLongOrNull() != null }, String::toLong, emptyList(), 0L))
            GLOBAL.set(TypeInfo.of(Boolean::class.java), ArgumentType({ it == "true" || it == "false" }, String::toBoolean, emptyList(), false))
            // String literal
            GLOBAL.set(TypeInfo.of(String::class.java), ArgumentType({ true }, { it }, emptyList(), ""))
        }
    }

}


@Suppress("UNCHECKED_CAST")
fun <T : Any> KClass<T>.get(): T? =
        if (None::class.java.isAssignableFrom(this.java)) null
        else this.objectInstance ?: try {
            this.java.getDeclaredField("INSTANCE").get(null) as T
        } catch (e: Throwable) {
            throw IllegalStateException("Provided class is not a valid singleton class: $this. A Singleton class must be a Kotlin object or a class with a static non-null 'INSTANCE' field.", e)
        }


fun Cmd.resolveParents(manager: CommandManager, owner: Any?) =
        this.parents.let {
            if (it.isEmpty()) null else {
                var cmd = manager.getCommand(it.first(), owner) ?: throw CommandNotFoundException("Specified parent command ${it.first()} was not found.")

                if (it.size > 1) {
                    for (x in it.copyOfRange(1, it.size)) {
                        cmd = cmd.getSubCommand(x) ?: throw CommandNotFoundException("Specified parent command $x was not found in command $cmd.")
                    }
                }

                cmd
            }
        }

fun Cmd.resolveParents(manager: CommandManager, owner: Any?, other: List<Command>) =
        this.parents.let {
            if (it.isEmpty()) null else {
                var cmd = manager.getCommand(it.first(), owner)
                        ?: other.find { (_, _, cmdName) -> cmdName.compareTo(name) == 0 }
                        ?: return null/*throw CommandNotFoundException("Specified parent command ${it.first()} was not found.")*/

                if (it.size > 1) {
                    for (x in it.copyOfRange(1, it.size)) {
                        cmd = cmd.getSubCommand(x)
                                ?: return null/*throw CommandNotFoundException("Specified parent command $x was not found in command $cmd.")*/
                    }
                }

                cmd
            }
        }

fun Cmd.toKCommand(manager: CommandManager, handler: Handler?, superCommand: Command?, arguments: List<Argument<*>>, owner: Any?): Command {
    val order = this.order
    val name = this.name
    val alias = this.alias
    val description = this.description
    val parent = this.resolveParents(manager, owner)

    return Command(parent = parent ?: superCommand,
            order = order,
            name = CommandName.StringName(name),
            description = description,
            handler = handler,
            arguments = arguments,
            requirements = this.getRequirements(),
            alias = alias.map { CommandName.StringName(it) }.toList())

}

class Checker(val manager: CommandManager, val owner: Any?, val command: Cmd) : (List<Command>) -> Boolean {
    override fun invoke(p1: List<Command>): Boolean =
            if(command.parents.isEmpty()) true else command.resolveParents(manager, owner, p1) != null
}

fun Array<Require>.toSpecs(): List<Requirement<*, *>> =
        this.map {
            @Suppress("UNCHECKED_CAST")
            Requirement(it.data,
                    it.subject.let { Information.Id(it.value.java, it.tags) },
                    TypeInfo.of(it.infoType.java) as TypeInfo<Any>,
                    TypeInfo.of(String::class.java), it.testerType.get() as RequirementTester<Any, String>)
        }

fun Cmd.getRequirements(): List<Requirement<*, *>> = this.requirements.toSpecs()

fun Cmd.getHandlerOrNull(): Handler? =
        this.handler.get()

fun Cmd.getHandler(element: Element): Handler =
        this.handler.get() ?: ReflectionHandler(element)

fun Arg.getHandlerOrNull(): ArgumentHandler<*>? =
        this.handler.get()

fun Arg.getHandler(element: Element): ArgumentHandler<*> =
        this.handler.get() ?: ReflectionHandler(element)

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