/*
 *      KWCommands - New generation of WCommands written in Kotlin <https://github.com/JonathanxD/KWCommands>
 *
 *         The MIT License (MIT)
 *
 *      Copyright (c) 2018 JonathanxD
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

import com.github.jonathanxd.iutils.text.TextUtil
import com.github.jonathanxd.iutils.type.TypeInfo
import com.github.jonathanxd.iutils.type.TypeUtil
import com.github.jonathanxd.kwcommands.argument.Argument
import com.github.jonathanxd.kwcommands.argument.ArgumentHandler
import com.github.jonathanxd.kwcommands.argument.ArgumentType
import com.github.jonathanxd.kwcommands.reflect.annotation.Arg
import com.github.jonathanxd.kwcommands.reflect.annotation.Ctx
import com.github.jonathanxd.kwcommands.reflect.annotation.Info
import com.github.jonathanxd.kwcommands.reflect.annotation.createId
import com.github.jonathanxd.kwcommands.reflect.element.ElementParameter
import com.github.jonathanxd.kwcommands.reflect.util.get
import com.github.jonathanxd.kwcommands.reflect.util.getHandlerOrNull
import com.github.jonathanxd.kwcommands.reflect.util.getRequirements
import com.github.jonathanxd.kwcommands.util.*
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Field
import java.lang.reflect.Parameter
import java.util.*

fun AnnotatedElement.type(): TypeInfo<*> =
    TypeUtil.toTypeInfo((this as? Field)?.genericType ?: (this as Parameter).parameterizedType)

fun AnnotatedElement.createElement(
    reflectionEnvironment: ReflectionEnvironment,
    type_: TypeInfo<*>? = null,
    arg: Argument<*>? = null
): ElementParameter<*> {
    val argumentAnnotation = this.getDeclaredAnnotation(Arg::class.java)
    val infoAnnotation = this.getDeclaredAnnotation(Info::class.java)
    val ctxAnnotation = this.getDeclaredAnnotation(Ctx::class.java)

    val type = type_ ?: this.type()

    @Suppress("UNCHECKED_CAST")
    return when {
        ctxAnnotation != null -> ElementParameter.CtxParameter
        infoAnnotation != null -> {
            val infoIsOptional = infoAnnotation.isOptional
            val infoId = infoAnnotation.createId(type)

            @Suppress("UNCHECKED_CAST")
            ElementParameter.InformationParameter(
                id = infoId,
                isOptional = infoIsOptional,
                type = type as TypeInfo<Any?>
            )
        }
        argumentAnnotation != null || arg != null -> ElementParameter.ArgumentParameter(
            arg ?: this.createArg(reflectionEnvironment),
            type as TypeInfo<Any?>
        )
        else -> throw IllegalStateException("Missing annotation for parameter: $this")
    }
}

fun AnnotatedElement.createArg(
    reflectionEnvironment: ReflectionEnvironment,
    type_: TypeInfo<*>? = null
): Argument<*> {
    val argumentAnnotation: Arg? = this.getDeclaredAnnotation(Arg::class.java)

    val name = argumentAnnotation?.value.let {
        if (it == null || it.isEmpty()) (this as? Field)?.name ?: (this as Parameter).name
        else it
    }

    val type = type_ ?: this.type()

    val typeIsOpt = type.classLiteral == TypeInfo.of(Optional::class.java).classLiteral
    val typeIsOptInt = type.classLiteral == TypeInfo.of(OptionalInt::class.java).classLiteral
    val typeIsOptDouble = type.classLiteral == TypeInfo.of(OptionalDouble::class.java).classLiteral
    val typeIsOptLong = type.classLiteral == TypeInfo.of(OptionalLong::class.java).classLiteral
    val isOptional =
        argumentAnnotation?.optional ?: typeIsOpt || typeIsOptInt || typeIsOptDouble || typeIsOptLong
    val argumentType0 = reflectionEnvironment.getOrNull(type)
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
    return Argument(
        name = name,
        alias = argumentAnnotation?.alias?.toList().orEmpty(),
        description = TextUtil.parse(description),
        isOptional = isOptional,
        argumentType = argumentType,
        requiredInfo = emptySet(),
        requirements = argumentAnnotation?.getRequirements(this).orEmpty(),
        handler = argumentAnnotation?.getHandlerOrNull() as? ArgumentHandler<out Any>
    )
}

fun AnnotatedElement.anyArgAnnotation() =
    this.isAnnotationPresent(Arg::class.java)
            || this.isAnnotationPresent(Info::class.java)
            || this.isAnnotationPresent(Ctx::class.java)