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

import com.github.jonathanxd.iutils.type.TypeInfo
import com.github.jonathanxd.kwcommands.argument.ArgumentHandler
import com.github.jonathanxd.kwcommands.argument.ArgumentType
import com.github.jonathanxd.kwcommands.command.Handler
import com.github.jonathanxd.kwcommands.json.DelegatedTypeResolver
import com.github.jonathanxd.kwcommands.json.TypeResolver
import com.github.jonathanxd.kwcommands.parser.Possibilities
import com.github.jonathanxd.kwcommands.parser.Transformer
import com.github.jonathanxd.kwcommands.parser.Validator

/**
 * Adds support to method handler, example:
 *
 * ```json
 * "handler":"method:register"
 * ```
 *
 * The method signature is resolved based on command arguments, when applied to arguments, the handler method
 * must have one parameter of the same type as argument type (or a super type) with same name as argument.
 *
 * @see [DynamicHandler]
 */
internal class ReflectTypeResolver(val type: Class<*>,
                                   val instanceProvider: (Class<*>) -> Any?,
                                   val reflectionEnvironment: ReflectionEnvironment,
                                   delegate: TypeResolver) : DelegatedTypeResolver(delegate) {

    override fun resolveResource(resource: String): String? =
            type.getResourceAsStream(resource)?.let {
                it.readBytes().toString(Charsets.UTF_8)
            } ?: super.resolveResource(resource)

    override fun resolveCommandHandler(input: String): Handler? {
        val (handlerType, sub) = this.getSub(input)

        if (handlerType == null || sub.isEmpty())
            return super.resolveCommandHandler(input)

        return DynamicHandler(sub, handlerType, instanceProvider, type, reflectionEnvironment)
    }

    override fun resolveArgumentHandler(input: String): ArgumentHandler<*>? {
        val (handlerType, sub) = this.getSub(input)

        if (handlerType == null || sub.isEmpty())
            return super.resolveArgumentHandler(input)

        return DynamicHandler(sub, handlerType, instanceProvider, type, reflectionEnvironment)
    }

    override fun resolveArgumentType(type: TypeInfo<*>): ArgumentType<*, *> =
            this.reflectionEnvironment.getOrNull(type)
                    ?: super.resolveArgumentType(type)

    private fun getSub(input: String): Pair<DynamicHandler.Type?, String> {
        val handlerType: DynamicHandler.Type? =
                if (input.startsWith(METHOD_PREFIX) && input.length > METHOD_PREFIX.length)
                    DynamicHandler.Type.METHOD
                else if (input.startsWith(FIELD_PREFIX) && input.length > FIELD_PREFIX.length)
                    DynamicHandler.Type.FIELD_SETTER
                else null


        val sub =
                when (handlerType) {
                    DynamicHandler.Type.METHOD -> input.substring(METHOD_PREFIX.length)
                    DynamicHandler.Type.FIELD_SETTER -> input.substring(FIELD_PREFIX.length)
                    else -> ""
                }

        return handlerType to sub
    }

    companion object {
        private const val METHOD_PREFIX = "method:"
        private const val FIELD_PREFIX = "field:"
    }
}