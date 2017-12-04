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
import com.github.jonathanxd.iutils.reflection.Link
import com.github.jonathanxd.iutils.reflection.Links
import com.github.jonathanxd.iutils.text.TextUtil
import com.github.jonathanxd.iutils.type.TypeInfo
import com.github.jonathanxd.iutils.type.TypeUtil
import com.github.jonathanxd.kwcommands.argument.*
import com.github.jonathanxd.kwcommands.command.Command
import com.github.jonathanxd.kwcommands.command.Handler
import com.github.jonathanxd.kwcommands.dispatch.DispatchHandler
import com.github.jonathanxd.kwcommands.information.RequiredInformation
import com.github.jonathanxd.kwcommands.json.JsonCommandParser
import com.github.jonathanxd.kwcommands.json.getCommandJsonObj
import com.github.jonathanxd.kwcommands.json.resolveJsonString
import com.github.jonathanxd.kwcommands.manager.CommandManager
import com.github.jonathanxd.kwcommands.manager.InstanceProvider
import com.github.jonathanxd.kwcommands.parser.SingleInput
import com.github.jonathanxd.kwcommands.parser.Transformer
import com.github.jonathanxd.kwcommands.reflect.CommandFactoryQueue
import com.github.jonathanxd.kwcommands.reflect.HandlerResolver
import com.github.jonathanxd.kwcommands.reflect.ReflectionHandler
import com.github.jonathanxd.kwcommands.reflect.annotation.*
import com.github.jonathanxd.kwcommands.reflect.element.Element
import com.github.jonathanxd.kwcommands.reflect.element.FieldElement
import com.github.jonathanxd.kwcommands.reflect.element.MethodElement
import com.github.jonathanxd.kwcommands.reflect.element.Parameter
import com.github.jonathanxd.kwcommands.reflect.util.*
import com.github.jonathanxd.kwcommands.util.*
import java.lang.invoke.MethodHandles
import java.lang.reflect.*
import java.util.*
import kotlin.reflect.KClass

typealias JsonParserResolver = (Class<*>) -> JsonCommandParser

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
class ReflectionEnvironment(val manager: CommandManager) : ArgumentTypeStorage {

    private val argumentTypeProviders = mutableSetOf<ArgumentTypeProvider>()
    private val handlerResolvers = mutableSetOf<HandlerResolver>()

    /**
     * Registers [argumentTypeProvider].
     */
    override fun registerProvider(argumentTypeProvider: ArgumentTypeProvider) =
            this.argumentTypeProviders.add(argumentTypeProvider)


    /**
     * Unregister [provider]
     */
    fun unregisterProvider(provider: ArgumentTypeProvider): Boolean =
            this.argumentTypeProviders.remove(provider)

    fun registerHandlerResolver(resolver: HandlerResolver): Boolean =
            this.handlerResolvers.add(resolver)

    fun unregisterHandlerResolver(resolver: HandlerResolver): Boolean =
            this.handlerResolvers.remove(resolver)

    /**
     * Gets required argument specification.
     *
     * @param type Type of argument value.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> get(type: TypeInfo<T>): ArgumentType<*, T> =
            this.getOrNull(type) ?: throw IllegalArgumentException("No argument type provider for type: $type.")

    override fun <T> getArgumentTypeOrNull(type: TypeInfo<T>): ArgumentType<*, T>? = this.getOrNull(type)
    override fun <T> getArgumentType(type: TypeInfo<T>): ArgumentType<*, T> = this.get(type)

    /**
     * Gets optional argument specification.
     *
     * @param type Type of argument value.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getOrNull(type: TypeInfo<T>): ArgumentType<*, T>? =
            this.argumentTypeProviders.getArgumentType(type) ?: getGlobalArgumentTypeOrNull(type)


    /**
     * Throw exception if argument type is null.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> ArgumentType<*, T>?.require(type: TypeInfo<*>): ArgumentType<*, T> =
            this ?: throw IllegalArgumentException("No argument type provider for type: $type.")

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
     * @return True if all command was registered with success.
     */
    fun registerCommands(list: List<Command>, owner: Any): Boolean =
            list.prepareCommands().all { this.manager.registerCommand(it, owner) }

    /**
     * Register all commands in [list] including super commands (of sub-commands) that are not in [list] and are not registered.
     *
     * If you don't want super commands (that are not present in [list]) to be registered, use [registerCommands].
     *
     * @param list Command list
     * @param owner Owner of command.
     * @return True if all command was registered with success.
     */
    fun registerCommandsAndSuper(list: List<Command>, owner: Any): Boolean {
        list.prepareCommands()

        var success = true

        list.forEach {

            if (it.superCommand != null) {
                it.superCommand.let {
                    if (!this.manager.isRegistered(it, owner))
                        if (!this.manager.registerCommand(it, owner))
                            success = false
                }
            } else {
                if (!this.manager.registerCommand(it, owner))
                    success = false
            }

        }

        return success
    }

    // Json

    /**
     * Creates a list of commands from json command annotations present in [klass]. This includes
     * annotations in fields, methods, and inner classes inside [klass].
     */
    fun <T : Any> fromJsonClass(klass: Class<T>,
                                instanceProvider: InstanceProvider,
                                jsonCommandParserResolver: JsonParserResolver): List<Command> {
        val commands = mutableListOf<Command>()

        commands += this.fromJsonAnnotated(klass, klass, instanceProvider, jsonCommandParserResolver)

        klass.declaredFields.forEach {
            commands += this.fromJsonAnnotated(it, klass, instanceProvider, jsonCommandParserResolver)
        }

        klass.declaredMethods.forEach {
            commands += this.fromJsonAnnotated(it, klass, instanceProvider, jsonCommandParserResolver)
        }

        klass.classes.forEach {
            commands += this.fromJsonClass(it, instanceProvider) {
                jsonCommandParserResolver(it).let {
                    it.factory.create(ReflectTypeResolver(klass, instanceProvider,
                            this, it.typeResolver))
                }
            }
        }

        return commands
    }

    /**
     * Creates a list of [Command] from json annotations in [annotatedElement], this does not include annotations
     * present inside the [annotatedElement].
     */
    fun fromJsonAnnotated(annotatedElement: AnnotatedElement,
                          klass: Class<*>,
                          instanceProvider: InstanceProvider,
                          jsonCommandParserResolver: JsonParserResolver): List<Command> {

        val jsonObj = annotatedElement.getCommandJsonObj() ?: return emptyList()

        val parser = jsonCommandParserResolver(jsonObj.parser).let {
            it.factory.create(ReflectTypeResolver(klass, instanceProvider, this, it.typeResolver))
        }
        try {
            val cmd = parser.parseCommand(jsonObj.resolveJsonString(klass))


            (cmd.handler as? DynamicHandler)?.resolveHandlers(cmd)

            cmd.arguments.forEach {
                (it.handler as? DynamicHandler)?.resolveHandlers(it, cmd)
            }

            return listOf(cmd)
        } catch (e: Exception) {
            throw IllegalArgumentException("Exception occurred while parsing json of obj '$jsonObj'", e)
        }
    }

    /**
     * Creates a list of [Command] from json annotations in [member], this does not include annotations
     * present inside the [member].
     */
    fun <T> fromJsonMember(member: T,
                           instanceProvider: InstanceProvider,
                           jsonCommandParserResolver: JsonParserResolver) where T : Member, T : AnnotatedElement =
            this.fromJsonAnnotated(member, member.declaringClass, instanceProvider, jsonCommandParserResolver)

    // Reflection

    /**
     * Create list with all [dispatch handlers][DispatchHandler] of [klass].
     */
    @JvmOverloads
    fun <T : Any> dispatchHandlersFrom(klass: Class<T>,
                                       instanceProvider: InstanceProvider,
                                       includeInner: Boolean = true): List<DispatchHandler> =
            mutableListOf<DispatchHandler>().also {
                dispatchHandlersFrom(klass, instanceProvider, includeInner, it)
            }

    fun <T : Any> dispatchHandlersFrom(klass: Class<T>,
                                       instanceProvider: InstanceProvider,
                                       includeInner: Boolean,
                                       handlers: MutableList<DispatchHandler>) {


        val methods = klass.methods.filter {
            it.isAnnotationPresent(AfterDispatch::class.java)
        }

        if (methods.isNotEmpty()) {
            val instance = instanceProvider(klass)
            methods.forEach {
                val afterDispatch = it.getDeclaredAnnotation(AfterDispatch::class.java)

                handlers += createDispatchHandler(instance, it, afterDispatch.filter.map { it.java })
            }

        }

        klass.classes.forEach {
            dispatchHandlersFrom(it, instanceProvider, includeInner, handlers)
        }
    }

    /**
     * Create command list from commands of class [klass]
     *
     * @see fromClass
     */
    fun <T : Any> fromClass(klass: KClass<T>, instanceProvider: InstanceProvider, owner: Any?): List<Command> =
            this.fromClass(klass.java, instanceProvider, owner)

    /**
     * Create command list from commands of class [klass]
     *
     * @see fromClass
     */
    fun <T : Any> fromClass(klass: KClass<T>, instanceProvider: InstanceProvider,
                            owner: Any?, queue: CommandFactoryQueue): List<Command> =
            this.fromClass(klass.java, instanceProvider, owner, queue, true)

    /**
     * Create command list from commands of class [klass]
     *
     * @see fromClass
     */
    fun <T : Any> fromClass(klass: Class<T>, instanceProvider: InstanceProvider,
                            owner: Any?, queue: CommandFactoryQueue): List<Command> =
            this.fromClass(klass, instanceProvider, owner, queue, true)

    /**
     * Create command list from commands of class [klass]
     *
     * @see fromClass
     */
    fun <T> fromClass(klass: Class<T>, instanceProvider: InstanceProvider, owner: Any?): List<Command> {
        val queue = CommandFactoryQueue()

        fromClass(klass, instanceProvider, owner, queue, true)

        return queue.commands
    }

    /**
     * Create command list from commands of class [klass].
     *
     * @see fromClass
     */
    fun <T> fromClass(klass: Class<T>, instanceProvider: InstanceProvider,
                      owner: Any?, includeInner: Boolean): List<Command> {
        val queue = CommandFactoryQueue()

        fromClass(klass, instanceProvider, owner, queue, includeInner)

        return queue.commands
    }

    /**
     * Create command list from commands of class [klass].
     *
     * @param instanceProvider Provider of class instances (null for static elements).
     * @param owner Owner of commands, used to fetch parent command.
     * @param queue Command Factory Queue.
     * @param includeInner Include inner class commands.
     * @see CommandFactoryQueue
     * @see ReflectionEnvironment
     */
    fun <T> fromClass(klass: Class<T>,
                      instanceProvider: InstanceProvider,
                      owner: Any?,
                      queue: CommandFactoryQueue,
                      includeInner: Boolean): List<Command> =
            fromClass(klass, instanceProvider, null, owner, queue, includeInner)


    /**
     * Create command list from commands of class [klass].
     *
     * @param instanceProvider Provider of class instances (null for static elements).
     * @param superCommand Super command.
     * @param owner Owner of commands, used to fetch parent command.
     * @param queue Command Factory Queue.
     * @param includeInner Include inner class commands.
     * @see CommandFactoryQueue
     * @see ReflectionEnvironment
     */
    fun <T> fromClass(klass: Class<T>,
                      instanceProvider: InstanceProvider,
                      superCommand: Command?,
                      owner: Any?,
                      queue: CommandFactoryQueue,
                      includeInner: Boolean): List<Command> {
        this.fromClassToQueue(klass, instanceProvider, superCommand, owner, queue, includeInner)
        return queue.commands
    }

    /**
     * Add commands of [klass] to queue. This function should be used instead of [fromClass] only
     * if a parent command should be resolved later because depends on commands which is not present in
     * [klass] (this does not include commands in inner classes).
     *
     * @param instanceProvider Provider of class instances (null for static elements).
     * @param superCommand Super command.
     * @param owner Owner of commands, used to fetch parent command.
     * @param queue Command Factory Queue.
     * @param includeInner Include inner class commands.
     * @see CommandFactoryQueue
     * @see ReflectionEnvironment
     */
    fun <T> fromClassToQueue(klass: Class<T>,
                             instanceProvider: InstanceProvider,
                             superCommand: Command?,
                             owner: Any?,
                             queue: CommandFactoryQueue,
                             includeInner: Boolean) {

        val instance = klass.cast(instanceProvider.checked(klass))

        val command = klass.getDeclaredAnnotation(Cmd::class.java)

        var args: List<Argument<*>> = emptyList()
        val requiredInfo = mutableSetOf<RequiredInformation>()

        val handler = command?.getClassHandlerOrNull(klass, this) {
            val (lElement, lArgs, lreqs) = createElement(instance, it)
            args = lArgs
            requiredInfo += lreqs
            lElement
        }

        if (command != null && handler == null) {
            args = klass.declaredFields.filter {
                !it.isAnnotationPresent(Exclude::class.java)
                        && ((Modifier.isStatic(it.modifiers) && instance == null) || (!Modifier.isStatic(it.modifiers) && instance != null))
            }.map {
                fromField(instance, it).also {
                    val fhandler = it.handler
                    (fhandler as? ReflectionHandler)?.element?.parameters?.forEach {
                        if (it is Parameter.InformationParameter) {
                            if (!it.isOptional) {
                                requiredInfo += RequiredInformation(it.id/*, it.infoComponent*/)
                            }
                        }
                    }
                }
            }
        }

        val kCommand = command?.toKCommand(manager,
                handler,
                superCommand,
                args,
                requiredInfo,
                owner,
                klass)

        kCommand?.let { k ->
            queue.queueCommand(klass, command.getName(klass), {
                k
            }, Checker(this.manager, owner, command, klass), { command.getPath(klass).joinToString(separator = " ") })
        }

        fromMethodsOfClass(klass, instance, kCommand, owner, queue)

        if (includeInner) {
            klass.declaredClasses.forEach {
                if (it.isAnnotationPresent(Cmd::class.java))
                    fromClassToQueue(it, instanceProvider, kCommand, owner, queue, includeInner)
            }
        }

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

    fun resolveHandler(element: Element): Any {
        for (handlerResolver in this.handlerResolvers) {
            handlerResolver.resolve(element)?.let {
                return it
            }
        }

        return ReflectionHandler(element)
    }


    fun linkMethod(instance: Any?, method: Method): Link<Any?> =
            Links.ofInvokable(Invokables.fromMethodHandle<Any?>(LOOKUP.unreflect(method))).let {
                if (instance != null) it.bind(instance) else it
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

        var karg: Argument<Any?>? = null

        val parameters = field.let {
            val argumentAnnotation: Arg? = field.getDeclaredAnnotation(Arg::class.java)
            val infoAnnotation = it.getDeclaredAnnotation(Info::class.java)

            val name = argumentAnnotation?.value.let { arg ->
                if (arg == null || arg.isEmpty())
                    it.name
                else arg
            }

            val typeIsOpt = type.classLiteral == TypeInfo.of(Optional::class.java).classLiteral
            val typeIsOptInt = type.classLiteral == TypeInfo.of(OptionalInt::class.java).classLiteral
            val typeIsOptDouble = type.classLiteral == TypeInfo.of(OptionalDouble::class.java).classLiteral
            val typeIsOptLong = type.classLiteral == TypeInfo.of(OptionalLong::class.java).classLiteral
            val isOptional = argumentAnnotation?.optional ?: typeIsOpt || typeIsOptInt || typeIsOptDouble || typeIsOptLong
            val argumentType0 = this.getOrNull(type)
                    ?: argumentAnnotation?.argumentType?.get()?.invoke()
                    ?: stringArgumentType

            val description = argumentAnnotation?.description ?: ""

            val def = argumentType0.defaultValue
            @Suppress("UNCHECKED_CAST")
            val argumentType = when {
                def != null -> argumentType0
                typeIsOpt -> optArgumentType(argumentType0)
                typeIsOptInt -> optIntArgumentType(argumentType0 as ArgumentType<*, Int>)
                typeIsOptDouble -> optDoubleArgumentType(argumentType0 as ArgumentType<*, Double>)
                typeIsOptLong -> optLongArgumentType(argumentType0 as ArgumentType<*, Long>)
                else -> argumentType0
            }

            @Suppress("UNCHECKED_CAST")
            karg = Argument(
                    name = name,
                    alias = argumentAnnotation?.alias?.toList().orEmpty(),
                    description = TextUtil.parse(description),
                    isOptional = isOptional,
                    argumentType = argumentType,
                    requiredInfo = emptySet(),
                    requirements = argumentAnnotation?.requirements.orEmpty().toSpecs(),
                    handler = argumentAnnotation?.getHandlerOrNull() as? ArgumentHandler<out Any>
            )

            if (infoAnnotation != null) {
                val infoIsOptional = infoAnnotation.isOptional
                val infoId = infoAnnotation.createId(type)

                @Suppress("UNCHECKED_CAST")
                return@let Parameter.InformationParameter(
                        id = infoId,
                        isOptional = infoIsOptional,
                        type = type as TypeInfo<Any?>
                )
            } else {

                @Suppress("UNCHECKED_CAST")
                return@let Parameter.ArgumentParameter(karg!!, type as TypeInfo<Any?>)
            }

        }

        return karg!!.let {
            if (it.handler == null)
                it.copy(handler = resolveHandler(FieldElement(field, instance, listOf(parameters), field.declaringClass))
                        as ArgumentHandler<out Any?>)
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
        val (element, arguments, lreqs) = createElement(instance, method)

        queue.queueCommand(method, command.getName(method), { cmds ->
            val superCmd = command.resolveParents(this.manager, owner, cmds) ?: superCommand

            command.toKCommand(manager, command.getHandler(element, this), superCmd, arguments, lreqs, owner, method)
        }, Checker(this.manager, owner, command, method), { command.getPath(method).joinToString(separator = " ") })

    }

    /**
     * Creates a handler for [method].
     */
    fun createDispatchHandler(instance: Any?, method: Method, filter: List<Class<*>>): DispatchHandler {
        for (handlerResolver in this.handlerResolvers) {
            handlerResolver.resolveDispatchHandler(instance, method, filter)?.let {
                return it
            }
        }

        val link: Link<Any?> = Links.ofInvokable<Any?>(Invokables.fromMethodHandle(LOOKUP.unreflect(method))).let {
            if (instance != null) it.bind(instance) else it
        }

        return if (filter.isEmpty())
            ReflectDispatchHandler(link)
        else
            ReflectFilterDispatchHandler(link, filter)
    }

    /**
     * Creates a handler for [method].
     */
    fun createHandler(instance: Any?, method: Method): Handler =
            this.resolveHandler(this.createElement(instance, method).element) as Handler

    /**
     * Creates a handler for [method].
     */
    fun createArgumentHandler(instance: Any?, method: Method): ArgumentHandler<*> =
            this.resolveHandler(this.createElement(instance, method).element) as ArgumentHandler<*>

    /**
     * Create a [Element] instance from a [method].
     *
     * @param instance Instance of method declaring class (null for static).
     * @param method Method to create [Command].
     * @see ReflectionEnvironment
     */
    private fun createElement(instance: Any?, method: Method): RMethodElement {

        val arguments = mutableListOf<Argument<*>>()
        val requiredInfo = mutableSetOf<RequiredInformation>()

        val parameters = method.parameters.map {
            val argumentAnnotation = it.getDeclaredAnnotation(Arg::class.java)
            val infoAnnotation = it.getDeclaredAnnotation(Info::class.java)

            val type = TypeUtil.toTypeInfo(it.parameterizedType)

            when {
                infoAnnotation != null -> {

                    val isOptional = infoAnnotation.isOptional
                    val id = infoAnnotation.createId(type)

                    @Suppress("UNCHECKED_CAST")
                    val param = Parameter.InformationParameter(
                            id = id,
                            isOptional = isOptional,
                            type = type as TypeInfo<Any?>
                    )

                    requiredInfo += RequiredInformation(id/*, param.infoComponent*/)

                    return@map param
                }
                argumentAnnotation != null -> {

                    val name = argumentAnnotation.value.let { arg ->
                        if (arg.isEmpty())
                            it.name
                        else arg
                    }

                    val description = argumentAnnotation.description

                    val typeIsOpt = type.classLiteral == TypeInfo.of(Optional::class.java).classLiteral
                    val typeIsOptInt = type.classLiteral == TypeInfo.of(OptionalInt::class.java).classLiteral
                    val typeIsOptDouble = type.classLiteral == TypeInfo.of(OptionalDouble::class.java).classLiteral
                    val typeIsOptLong = type.classLiteral == TypeInfo.of(OptionalLong::class.java).classLiteral
                    val isOptional = argumentAnnotation.optional
                            || typeIsOpt
                            || typeIsOptInt
                            || typeIsOptDouble
                            || typeIsOptLong

                    val argumentType0 = this.getOrNull(type)
                            ?: argumentAnnotation.argumentType.get()?.invoke()
                            ?: stringArgumentType

                    val def = argumentType0.defaultValue
                    @Suppress("UNCHECKED_CAST")
                    val argumentType = when {
                        def != null -> argumentType0
                        typeIsOpt -> optArgumentType(argumentType0)
                        typeIsOptInt -> optIntArgumentType(argumentType0 as ArgumentType<*, Int>)
                        typeIsOptDouble -> optDoubleArgumentType(argumentType0 as ArgumentType<*, Double>)
                        typeIsOptLong -> optLongArgumentType(argumentType0 as ArgumentType<*, Long>)
                        else -> argumentType0
                    }
                    val requirements = argumentAnnotation.requirements.toSpecs()

                    @Suppress("UNCHECKED_CAST")
                    val argument = Argument(
                            name = name,
                            alias = argumentAnnotation.alias.toList(),
                            description = TextUtil.parse(description),
                            isOptional = isOptional,
                            requirements = requirements,
                            requiredInfo = emptySet(),
                            argumentType = argumentType
                    )

                    arguments += argument

                    @Suppress("UNCHECKED_CAST")
                    return@map Parameter.ArgumentParameter(argument, type as TypeInfo<Any?>)
                }
                else -> throw IllegalStateException("Missing annotation for parameter: $it")
            }
        }

        return RMethodElement(MethodElement(method, instance, parameters, method.declaringClass), arguments, requiredInfo)
    }

    /**
     * Creates a copy of this [ReflectionEnvironment].
     *
     * @param manager New manager.
     */
    fun copy(manager: CommandManager = this.manager) = ReflectionEnvironment(manager).also {
        it.argumentTypeProviders.addAll(this.argumentTypeProviders)
    }

    companion object {
        private val GLOBAL = object : ArgumentTypeStorage {
            private val set = mutableSetOf<ArgumentTypeProvider>()

            override fun registerProvider(argumentTypeProvider: ArgumentTypeProvider): Boolean {
                return this.set.add(argumentTypeProvider)
            }

            override fun <T> getArgumentTypeOrNull(type: TypeInfo<T>): ArgumentType<*, T>? {
                return this.set.getArgumentType(type)
            }

            override fun <T> getArgumentType(type: TypeInfo<T>): ArgumentType<*, T> {
                return this.set.getArgumentType(type) ?: throw IllegalArgumentException("No argument type provider for type: $type.")
            }

        }


        @Suppress("UNCHECKED_CAST")
        fun <T> Set<ArgumentTypeProvider>.getArgumentType(type: TypeInfo<T>): ArgumentType<*, T>? =
                (this.getFirstOrNull({ it.provide(type) }, { it != null }) ?: this.let {
                    it.filterIsInstance<ConcreteProvider>().forEach {
                        if (it.argumentType.type.typeParameters.isEmpty()
                                && it.argumentType.type.classLiteral == type.classLiteral)
                            return@let it.argumentType as? ArgumentType<*, T>
                    }
                    null
                })


        /**
         * Registers global [ArgumentTypeProvider].
         */
        fun registerGlobal(argumentTypeProvider: ArgumentTypeProvider) = GLOBAL.registerProvider(argumentTypeProvider)

        /**
         * Gets required global argument specification.
         *
         * @param type Type of argument value.
         */
        fun <T> getGlobalArgumentType(type: TypeInfo<T>) = GLOBAL.getArgumentType(type)

        /**
         * Gets optional global argument specification.
         *
         * @param type Type of argument value.
         */
        fun <T> getGlobalArgumentTypeOrNull(type: TypeInfo<T>) = GLOBAL.getArgumentTypeOrNull(type)

        init {
            // Data types
            registerGlobal(DefaultProvider)
            registerGlobal(CollectionProvider(GLOBAL))
        }

        object DefaultProvider : ArgumentTypeProvider {

            override fun <T> provide(type: TypeInfo<T>): ArgumentType<*, T>? {

                if (type.isResolved || type.canResolve()) {
                    val typeClass = type.typeClass

                    if (typeClass == List::class.java
                            && type.typeParameters.size == 1) {
                        val component = type.typeParameters.single()

                        provide(component)?.let { provided ->
                            @Suppress("UNCHECKED_CAST")
                            return ListArgumentType<Any?>(provided, type.cast()) as ArgumentType<*, T>
                            /*return ArgumentType(
                                    ListValidator(provided.validator as Validator<Input>),
                                    ListTransformer(provided.transformer as Transformer<Input, Any?>),
                                    provided.possibilities,
                                    listOf(provided.defaultValue)
                            ) as ArgumentType<*, T>*/
                        }
                    }

                    if (typeClass == Map::class.java
                            && type.typeParameters.size == 2) {
                        val (keyComponent, valueComponent) = type.typeParameters

                        val keyType = provide(keyComponent)
                        val valueType = provide(valueComponent)

                        if (keyType != null && valueType != null) {
                            @Suppress("UNCHECKED_CAST")
                            return MapArgumentType<Any?, Any?>(keyType, valueType, type.cast()) as ArgumentType<*, T>
                            /*ArgumentType(
                                    MapValidator(keyType, valueType),
                                    MapTransformer(keyType, valueType),
                                    MapPossibilitiesFunc(keyType, valueType),
                                    keyType.defaultValue?.let { k ->
                                        valueType.defaultValue?.let { v ->
                                            mapOf(k to v)
                                        }
                                    }
                            ) as ArgumentType<*, T>*/

                        }
                    }

                    if (typeClass.isEnum) {
                        return simpleArgumentType(
                                EnumTransformer(typeClass),
                                EnumValidator(typeClass),
                                EnumPossibilitiesFunc(typeClass),
                                type
                        ).cast(type)

                    }
                }

                return when (type) {
                    TypeInfo.of(Short::class.javaObjectType),
                    TypeInfo.of(Short::class.javaPrimitiveType) -> shortArgumentType

                    TypeInfo.of(Char::class.javaObjectType),
                    TypeInfo.of(Char::class.javaPrimitiveType) -> charArgumentType

                    TypeInfo.of(Byte::class.javaObjectType),
                    TypeInfo.of(Byte::class.javaPrimitiveType) -> byteArgumentType

                    TypeInfo.of(Int::class.javaObjectType),
                    TypeInfo.of(Int::class.javaPrimitiveType) -> intArgumentType

                    TypeInfo.of(Float::class.javaObjectType),
                    TypeInfo.of(Float::class.javaPrimitiveType) -> floatArgumentType

                    TypeInfo.of(Double::class.javaObjectType),
                    TypeInfo.of(Double::class.javaPrimitiveType) -> doubleArgumentType

                    TypeInfo.of(Long::class.javaObjectType),
                    TypeInfo.of(Long::class.javaPrimitiveType) -> longArgumentType

                    TypeInfo.of(Boolean::class.javaObjectType),
                    TypeInfo.of(Boolean::class.javaPrimitiveType) -> booleanArgumentType

                    TypeInfo.of(String::class.java) -> stringArgumentType
                    else -> null
                }?.cast(type)

            }

        }

        class CollectionProvider(val storage: ArgumentTypeStorage) : ArgumentTypeProvider {
            override fun <T> provide(type: TypeInfo<T>): ArgumentType<*, T>? {
                val component = type.typeParameters.singleOrNull() ?: TypeInfo.of(String::class.java)

                if (type.typeClass == List::class.java)
                    return simpleArgumentType(
                            ReflectListTransform(storage, component) as Transformer<SingleInput, *>,
                            ReflectListValidator(storage, component),
                            possibilitiesFunc { emptyList() },
                            mutableListOf<Any?>(),
                            type).cast(type)

                return null
            }

        }
    }

    private data class RMethodElement(val element: Element, val arguments: List<Argument<*>>, val requiredInfo: Set<RequiredInformation>)
}

class Checker(val manager: CommandManager, val owner: Any?, val command: Cmd, val annotatedElement: AnnotatedElement) : (List<Command>) -> Boolean {
    override fun invoke(p1: List<Command>): Boolean =
            if (command.parents.isEmpty()) true
            else command.resolveParents(manager, owner, p1) != null
}

interface ArgumentTypeStorage {
    /**
     * Registers global [ArgumentTypeProvider].
     */
    fun registerProvider(argumentTypeProvider: ArgumentTypeProvider): Boolean

    /**
     * Gets optional argument specification.
     *
     * @param type Type of argument value.
     */
    fun <T> getArgumentTypeOrNull(type: TypeInfo<T>): ArgumentType<*, T>?

    /**
     * Gets required argument specification.
     *
     * @param type Type of argument value.
     */
    fun <T> getArgumentType(type: TypeInfo<T>): ArgumentType<*, T>
}

val InstanceProvider.checked: InstanceProvider
    get() = (this as? CheckedInstanceProvider) ?: CheckedInstanceProvider(this)

internal class CheckedInstanceProvider(private val original: InstanceProvider) : InstanceProvider {
    override fun invoke(type: Class<*>): Any? {
        val instance = original(type)

        if (!type.isInstance(instance))
            throw IllegalArgumentException("Provided value '$instance' is not an instance of base input type" +
                    " '${type.canonicalName}'. Value provided by '$original'.")

        return instance
    }

}