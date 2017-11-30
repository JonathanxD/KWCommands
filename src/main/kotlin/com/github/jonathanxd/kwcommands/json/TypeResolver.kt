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
package com.github.jonathanxd.kwcommands.json

import com.github.jonathanxd.iutils.reflection.Reflection
import com.github.jonathanxd.iutils.type.TypeInfo
import com.github.jonathanxd.iutils.type.TypeInfoUtil
import com.github.jonathanxd.kwcommands.argument.ArgumentHandler
import com.github.jonathanxd.kwcommands.argument.ArgumentType
import com.github.jonathanxd.kwcommands.command.Handler
import com.github.jonathanxd.kwcommands.reflect.env.ReflectionEnvironment
import java.util.function.Function

interface TypeResolver {
    /**
     * Resolves the singleton instance [T] of [type].
     */
    fun <T> getSingletonInstance(type: Class<T>): T

    /**
     * Resolves the [resource] content. The way this is resolved is determined by the implementation,
     * this behavior should be explained in implementation documentation.
     */
    fun resolveResource(resource: String): String?

    /**
     * Resolves the [TypeInfo] that [input] refers to.
     *
     * This function may be called with class name, aliases to classes, [TypeInfo] literals (See [TypeInfoUtil]).
     */
    fun resolve(input: String): TypeInfo<*>?

    /**
     * Resolves the [Handler] based on [input].
     *
     * Likely [resolve], the function may be called with some variances of input, and when called with class literals,
     * this commonly resolves the singleton instance using [getSingletonInstance].
     */
    fun resolveCommandHandler(input: String): Handler?

    /**
     * Resolves the [ArgumentHandler] based on [input].
     *
     * @see [resolveCommandHandler]
     */
    fun resolveArgumentHandler(input: String): ArgumentHandler<*>?

    /**
     * Resolves the [ArgumentType] based on [type].
     *
     * Commonly resolves the singleton instance using [getSingletonInstance].
     */
    fun resolveArgumentType(type: TypeInfo<*>): ArgumentType<*, *>

}

/**
 * A delegated type resolver, used by [ReflectionEnvironment] to implement some additional features.
 */
abstract class DelegatedTypeResolver(delegate: TypeResolver) : TypeResolver by delegate

/**
 * Register default resolvers in [MapTypeResolver], this includes resolvers for common Java types, such as
 * [String], [Integer], [Long], [Enum]s and others.
 */
fun MapTypeResolver.registerDefaults() {
    this.argumentTypeResolvers.add {
        ReflectionEnvironment.getGlobalArgumentTypeOrNull(it)
    }
}

/**
 * A type resolver that use [Map], [Set] and a [TypeInfoUtil.ClassLoaderClassResolver] to allow
 * insertion and removal of custom resolvers, custom class resolvers, aliases, and class loaders. This also implements
 * a logic to resolve singleton instances, through [Reflection.getInstance], and ability to resolve
 * java classes of `java.lang` package without qualified name.
 *
 * The resolution of resource is made through [loaders].
 */
class MapTypeResolver @JvmOverloads constructor(val appendJavaLang: Boolean = true) :
        TypeResolver, Function<String, Class<*>?> {

    private val loaderResolver = TypeInfoUtil.ClassLoaderClassResolver()
    private val map = mutableMapOf<String, Class<*>>()
    val singletonInstances = mutableMapOf<Class<*>, Any?>()
    val commandHandlerResolvers = mutableSetOf<(input: String) -> Handler?>()
    val argumentHandlerResolvers = mutableSetOf<(input: String) -> ArgumentHandler<*>?>()
    val argumentTypeResolvers = mutableSetOf<(type: TypeInfo<*>) -> ArgumentType<*, *>?>()
    /*val possibilitiesResolvers = mutableSetOf<(type: TypeInfo<*>) -> Possibilities?>()
    val transformerResolvers = mutableSetOf<(type: TypeInfo<*>) -> Transformer<*, Any?>?>()
    val validatorResolvers = mutableSetOf<(type: TypeInfo<*>) -> Validator<*>?>()
    val defaultValueResolvers = mutableSetOf<(type: TypeInfo<*>) -> Any?>()*/

    val loaders: MutableList<ClassLoader>
        get() = this.loaderResolver.classLoadersList

    operator fun set(alias: String, info: Class<*>) {
        this.map[alias] = info
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> getSingletonInstance(type: Class<T>): T {
        return (singletonInstances[type] as? T) ?: Reflection.getInstance(type).also {
            singletonInstances[type] = it
        }
    }

    override fun resolveResource(resource: String): String? {
        this.loaders.forEach {
            it.getResourceAsStream(resource)?.readBytes()?.toString(Charsets.UTF_8)?.let {
                return@resolveResource it
            }
        }

        return null
    }

    override fun resolve(input: String): TypeInfo<*>? =
            TypeInfoUtil.fromFullString(input, this).firstOrNull()

    override fun apply(t: String): Class<*>? =
            this.map[t]
                    ?: this.loaderResolver.apply(t)
                    ?: (if (this.appendJavaLang && !t.contains(".")) this.loaderResolver.apply("java.lang.$t") else null)

    override fun resolveCommandHandler(input: String): Handler? {
        this.commandHandlerResolvers.forEach {
            it(input)?.let {
                return it
            }
        }

        return this.apply(input)?.let { this.getSingletonInstance(it) as Handler }
    }

    override fun resolveArgumentHandler(input: String): ArgumentHandler<*>? {
        this.argumentHandlerResolvers.forEach {
            it(input)?.let {
                return it
            }
        }

        return this.apply(input)?.let { this.getSingletonInstance(it) as ArgumentHandler<*> }
    }

    override fun resolveArgumentType(type: TypeInfo<*>): ArgumentType<*, *> {
        this.argumentTypeResolvers.forEach {
            it(type)?.let {
                return it
            }
        }

        throw IllegalArgumentException("Can't resolve argument type '$type'!")
    }

}