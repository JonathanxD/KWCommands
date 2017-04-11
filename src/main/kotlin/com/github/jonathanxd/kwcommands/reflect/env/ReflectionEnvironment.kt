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
import com.github.jonathanxd.kwcommands.argument.Argument
import com.github.jonathanxd.kwcommands.argument.ArgumentHandler
import com.github.jonathanxd.kwcommands.command.Command
import com.github.jonathanxd.kwcommands.information.Information
import com.github.jonathanxd.kwcommands.manager.CommandManager
import com.github.jonathanxd.kwcommands.reflect.CommandFactoryQueue
import com.github.jonathanxd.kwcommands.reflect.ReflectionHandler
import com.github.jonathanxd.kwcommands.reflect.annotation.Arg
import com.github.jonathanxd.kwcommands.reflect.annotation.Cmd
import com.github.jonathanxd.kwcommands.reflect.annotation.Exclude
import com.github.jonathanxd.kwcommands.reflect.annotation.Info
import com.github.jonathanxd.kwcommands.reflect.annotation.Require
import com.github.jonathanxd.kwcommands.reflect.element.Element
import com.github.jonathanxd.kwcommands.reflect.element.Parameter
import com.github.jonathanxd.kwcommands.reflect.util.*
import java.lang.invoke.MethodHandles
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.*
import kotlin.reflect.KClass

/**
 * Reflection helper environment.
 *
 * Dependency resolution is made via [CommandFactoryQueue].
 *
 * This helper creates commands from Java elements. For classes, it will create a super-command and register
 * all methods as child commands (only if them does not specify parent command) and fields as arguments of class command.
 * In KWCommands Fields can be only arguments, in WCommands arguments are commands that have a single argument of the same type of field,
 * this was changed to make more sense (because fields are not runnable, only functions are runnable). For functions, their arguments
 * must be annotated with [Arg] or [Info].
 *
 * In KWCommands you still can annotate fields with [Require] like in WCommands, but in KWCommands, arguments can have
 * your own handler (that is called before command handler), you can also annotate parameters with [Require], but it does not
 * means that you don't need to annotate parameter with [Arg] or [Info].
 *
 * In KWCommands, command description is mandatory (I prefer that way).
 *
 * @property manager Manager used to resolve parent commands.
 */
class ReflectionEnvironment(val manager: CommandManager) {

    private val argumentTypes = mutableMapOf<TypeInfo<*>, ArgumentType<*>>()

    /**
     * Sets [argument specification][ArgumentType] of arguments of type [type].
     *
     * @param type Type of argument value.
     * @param argumentType Argument specification (if null, remove the specification).
     */
    fun <T> set(type: TypeInfo<T>, argumentType: ArgumentType<T>?) {
        if (argumentType == null)
            this.argumentTypes.remove(type)
        else {
            this.argumentTypes.put(type, argumentType)
            Primitive.unbox(type.typeClass)?.let { this.argumentTypes.put(TypeInfo.of(it), argumentType) }
            Primitive.box(type.typeClass)?.let { this.argumentTypes.put(TypeInfo.of(it), argumentType) }
        }
    }

    /**
     * Gets required argument specification.
     *
     * @param type Type of argument value.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> get(type: TypeInfo<T>): ArgumentType<T> =
            (this.argumentTypes.getArgumentType(type)) ?: getGlobalArgumentType(type)
                    ?: throw IllegalArgumentException("Not registered argument type: $type.")

    private val LOOKUP = MethodHandles.lookup()

    /**
     * Register commands returned by [ReflectionEnvironment] in this manager.
     *
     * Note: Command list returned by [ReflectionEnvironment] requires a special treatment (because it includes sub-commands),
     * this method will not register sub-commands in current manager, example:
     *
     * If command `B` is a sub-command of command `A`, and [list] contains command `B` but does not contains command `A` and the
     * command `A` is not registered in [manager], the command `B` will appear in all [CommandManager]
     * that command `A` is registered, but will not appear in [manager] because `A` is not registered and is
     * not present in [list], to accomplish this task use [registerCommandsAndSuper].
     *
     * @param list Command list
     * @param owner Owner of command.
     */
    fun registerCommands(list: List<Command>, owner: Any) {
        list.prepareCommands().forEach { this.manager.registerCommand(it, owner) }
    }

    /**
     * Register all commands in [list] including super commands (of sub-commands) that are not in [list] and are not registered.
     *
     * If you don't want super commands (that are not present in [list]) to be registered, use [registerCommands].
     *
     * @param list Command list
     * @param owner Owner of command.
     */
    fun registerCommandsAndSuper(list: List<Command>, owner: Any) {
        list.prepareCommands()

        list.forEach {

            if (it.superCommand != null) {
                it.superCommand.let {
                    if (!this.manager.isRegistered(it, owner))
                        this.manager.registerCommand(it, owner)
                }
            } else {
                this.manager.registerCommand(it, owner)
            }

        }
    }

    /**
     * Create command list from commands of class [klass]
     *
     * @see fromClass
     */
    fun <T : Any> fromClass(klass: KClass<T>, instanceProvider: (Class<*>) -> Any?, owner: Any?): List<Command> {
        return this.fromClass(klass.java, instanceProvider, owner)
    }

    /**
     * Create command list from commands of class [klass]
     *
     * @see fromClass
     */
    fun <T : Any> fromClass(klass: KClass<T>, instanceProvider: (Class<*>) -> Any?, owner: Any?, queue: CommandFactoryQueue): List<Command> {
        return this.fromClass(klass.java, instanceProvider, owner, queue)
    }

    /**
     * Create command list from commands of class [klass]
     *
     * @see fromClass
     */
    fun <T> fromClass(klass: Class<T>, instanceProvider: (Class<*>) -> Any?, owner: Any?): List<Command> {
        val queue = CommandFactoryQueue()

        fromClass(klass, instanceProvider, owner, queue)

        return queue.commands
    }

    /**
     * Create command list from commands of class [klass]
     *
     * @param instanceProvider Provider of class instances (null for static elements).
     * @param owner Owner of commands.
     * @param queue Command Factory Queue.
     * @see CommandFactoryQueue
     * @see ReflectionEnvironment
     */
    fun <T> fromClass(klass: Class<T>, instanceProvider: (Class<*>) -> Any?, owner: Any?, queue: CommandFactoryQueue): List<Command> {

        val instance = klass.cast(instanceProvider(klass))

        val command = klass.getDeclaredAnnotation(Cmd::class.java)!!

        val kCommand = command.toKCommand(manager, command.getHandlerOrNull(), null, klass.declaredFields.filter {
            !it.isAnnotationPresent(Exclude::class.java)
                    && ((Modifier.isStatic(it.modifiers) && instance == null) || (!Modifier.isStatic(it.modifiers) && instance != null))
        }.map { fromField(instance, it) }, owner, klass)

        queue.queueCommand(klass, command.getName(klass), {
            kCommand
        }, Checker(this.manager, owner, command, klass), { command.getPath(klass).joinToString(separator = " ") })

        fromMethodsOfClass(klass, instance, kCommand, owner, queue)

        klass.declaredClasses.forEach {
            if (it.isAnnotationPresent(Cmd::class.java))
                fromClass(it, instanceProvider, owner, queue)
        }

        return queue.commands
    }

    /**
     * Create list of commands from methods of [klass].
     *
     * @param instance Instance of class (null for static methods).
     * @param superCommand Super command of all commands that does not specify a parent command.
     * @param owner Owner of commands.
     */
    fun <T> fromMethodsOfClass(klass: Class<T>, instance: T?, superCommand: Command?, owner: Any?): List<Command> =
            CommandFactoryQueue().apply {
                fromMethodsOfClass(klass, instance, superCommand, owner, this)
            }.commands

    /**
     * Create list of commands from methods of [klass].
     *
     * @param instance Instance of class (null for static methods).
     * @param superCommand Super command of all commands that does not specify a parent command.
     * @param owner Owner of commands.
     * @param queue Command Factory Queue.
     * @see CommandFactoryQueue
     * @see ReflectionEnvironment
     */
    fun <T> fromMethodsOfClass(klass: Class<T>, instance: T?, superCommand: Command?, owner: Any?, queue: CommandFactoryQueue) {
        klass.declaredMethods
                .filter {
                    it.isAnnotationPresent(Cmd::class.java)
                            && ((Modifier.isStatic(it.modifiers) && instance == null) || (!Modifier.isStatic(it.modifiers) && instance != null))
                }
                .map { fromMethod(instance, it, superCommand, owner, queue) }
    }


    /**
     * Creates a list of arguments from a [field].
     *
     * @param instance Instance of field declaring class (null for static).
     * @param field Field to create argument.
     * @see ReflectionEnvironment
     */
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
                    requirements = annotated!!.getDeclaredAnnotationsByType(Require::class.java).orEmpty().toSpecs(),
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
            if (it.handler == null)
                it.copy(handler = ReflectionHandler(Element(link, listOf(parameters))))
            else it
        }
    }

    /**
     * Create a [Command] instance from a [method].
     *
     * @param instance Instance of method declaring class (null for static).
     * @param method Method to create [Command].
     * @param superCommand Super command of this [Command] if no one parent is specified.
     * @param owner Owner of command
     * @see ReflectionEnvironment
     */
    fun fromMethod(instance: Any?, method: Method, superCommand: Command?, owner: Any?): Command {
        val queue = CommandFactoryQueue()

        fromMethod(instance, method, superCommand, owner, queue)

        return queue.commands.first()
    }

    /**
     * Create a [Command] instance from a [method].
     *
     * @param instance Instance of method declaring class (null for static).
     * @param method Method to create [Command].
     * @param superCommand Super command of this [Command] if no one parent is specified.
     * @param owner Owner of command
     * @param queue Command Factory queue.
     * @see CommandFactoryQueue
     * @see ReflectionEnvironment
     */
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
                        requirements = it.getDeclaredAnnotationsByType(Require::class.java).orEmpty().toSpecs(),
                        type = type)

                arguments += argument

                @Suppress("UNCHECKED_CAST")
                return@map Parameter.ArgumentParameter(argument, type as TypeInfo<Any>)
            } else {
                throw IllegalStateException("Missing annotation for parameter: $it")
            }
        }

        queue.queueCommand(method, command.getName(method), { cmds ->
            val superCmd = command.resolveParents(this.manager, owner, method, cmds) ?: superCommand

            command.toKCommand(manager, command.getHandler(Element(link, parameters)), superCmd, arguments, owner, method)
        }, Checker(this.manager, owner, command, method), { command.getPath(method).joinToString(separator = " ") })

    }

    /**
     * Creates a copy of this [ReflectionEnvironment].
     *
     * @param manager New manager.
     */
    fun copy(manager: CommandManager = this.manager) = ReflectionEnvironment(manager).also {
        it.argumentTypes.putAll(this.argumentTypes)
    }

    companion object {
        private val GLOBAL = mutableMapOf<TypeInfo<*>, ArgumentType<*>>()

        /**
         * Helper method of [type] to [argumentType] registration.
         */
        fun <T> MutableMap<TypeInfo<*>, ArgumentType<*>>.set(type: TypeInfo<T>, argumentType: ArgumentType<T>?) =
                if (argumentType == null)
                    this.remove(type)
                else {
                    val put = this.put(type, argumentType)
                    Primitive.unbox(type.typeClass)?.let { this.put(TypeInfo.of(it), argumentType) }
                    Primitive.box(type.typeClass)?.let { this.put(TypeInfo.of(it), argumentType) }
                    put
                }

        /**
         * Helper method of [ArgumentType] fetching from a [type] key.
         */
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

        /**
         * Sets [argument specification][ArgumentType] of arguments of type [type] in global specification map.
         *
         * @param type Type of argument value.
         * @param argumentType Argument specification (if null, remove the specification).
         */
        fun <T> setGlobal(type: TypeInfo<T>, argumentType: ArgumentType<T>?) = GLOBAL.set(type, argumentType)

        /**
         * Gets optional global argument specification.
         *
         * @param type Type of argument value.
         */
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

class Checker(val manager: CommandManager, val owner: Any?, val command: Cmd, val annotatedElement: AnnotatedElement) : (List<Command>) -> Boolean {
    override fun invoke(p1: List<Command>): Boolean =
            if (command.parents.isEmpty()) true else command.resolveParents(manager, owner, annotatedElement, p1) != null
}