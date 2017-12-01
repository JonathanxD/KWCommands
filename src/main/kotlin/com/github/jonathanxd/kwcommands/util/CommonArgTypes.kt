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
package com.github.jonathanxd.kwcommands.util

import com.github.jonathanxd.iutils.type.TypeInfo
import com.github.jonathanxd.jwiutils.kt.typeInfo
import com.github.jonathanxd.kwcommands.argument.AnyArgumentType
import com.github.jonathanxd.kwcommands.argument.ArgumentType
import com.github.jonathanxd.kwcommands.argument.CustomArgumentType
import com.github.jonathanxd.kwcommands.argument.SingleArgumentType
import com.github.jonathanxd.kwcommands.dsl.stringTransformer
import com.github.jonathanxd.kwcommands.dsl.stringValidator
import com.github.jonathanxd.kwcommands.parser.*
import java.util.*


val charArgumentType = SingleArgumentType<Char>(
        CharTransformer,
        CharValidator,
        CharPossibilities,
        null,
        TypeInfo.of(Char::class.java)
)

val byteArgumentType = SingleArgumentType<Byte>(
        ByteTransformer,
        ByteValidator,
        BytePossibilities,
        null,
        TypeInfo.of(Byte::class.java)
)

val shortArgumentType = SingleArgumentType<Short>(
        ShortTransformer,
        ShortValidator,
        ShortPossibilities,
        null,
        TypeInfo.of(Short::class.java)
)

val intArgumentType = SingleArgumentType<Int>(
        IntTransformer,
        IntValidator,
        IntPossibilities,
        null,
        TypeInfo.of(Int::class.java)
)

val longArgumentType = SingleArgumentType<Long>(
        LongTransformer,
        LongValidator,
        LongPossibilities,
        null,
        TypeInfo.of(Long::class.java)
)

val doubleArgumentType = SingleArgumentType<Double>(
        DoubleTransformer,
        DoubleValidator,
        DoublePossibilities,
        null,
        TypeInfo.of(Double::class.java)
)

val floatArgumentType = SingleArgumentType<Float>(
        FloatTransformer,
        FloatValidator,
        FloatPossibilities,
        null,
        TypeInfo.of(Float::class.java)
)

val booleanArgumentType = SingleArgumentType<Boolean>(
        BooleanTransformer,
        BooleanValidator,
        BooleanPossibilities,
        null,
        TypeInfo.of(Boolean::class.java)
)

val stringArgumentType = SingleArgumentType<String>(
        stringTransformer,
        stringValidator,
        StringPossibilities,
        null,
        TypeInfo.of(String::class.java)
)

val anyArgumentType = AnyArgumentType

fun <I: Input, T> optArgumentType(argumentType: ArgumentType<I, T>): ArgumentType<I, Optional<T>> =
        CustomArgumentType({ Optional.of(it) }, Optional.empty(), argumentType,
                TypeInfo.builderOf(Optional::class.java).of(argumentType.type).buildGeneric())

fun <I: Input> optIntArgumentType(argumentType: ArgumentType<I, Int>): ArgumentType<I, OptionalInt> =
        CustomArgumentType({ OptionalInt.of(it) }, OptionalInt.empty(), argumentType,
                TypeInfo.builderOf(OptionalInt::class.java).build())

fun <I: Input> optDoubleArgumentType(argumentType: ArgumentType<I, Double>): ArgumentType<I, OptionalDouble> =
        CustomArgumentType({ OptionalDouble.of(it) }, OptionalDouble.empty(), argumentType,
                TypeInfo.builderOf(OptionalDouble::class.java).build())

fun <I: Input> optLongArgumentType(argumentType: ArgumentType<I, Long>): ArgumentType<I, OptionalLong> =
        CustomArgumentType({ OptionalLong.of(it) }, OptionalLong.empty(), argumentType,
                TypeInfo.builderOf(OptionalLong::class.java).build())

fun stringArgumentType(str: String) = SingleArgumentType<String>(
        stringTransformer,
        ExactStringValidator(str),
        ExactStringPossibilities(str),
        null,
        TypeInfo.of(String::class.java)
)

fun <T> enumArgumentType(enumType: Class<T>) = SingleArgumentType<T>(
        EnumTransformer(enumType),
        EnumValidator(enumType),
        EnumPossibilitiesFunc(enumType),
        null,
        TypeInfo.of(enumType)
)

fun <T> simpleArgumentType(transformer: Transformer<SingleInput, T>,
                           validator: Validator<SingleInput>,
                           possibilitiesFunc: Possibilities,
                           typeInfo: TypeInfo<out T>): ArgumentType<SingleInput, T> =
        SingleArgumentType(transformer, validator, possibilitiesFunc, null, typeInfo)

fun <T> simpleArgumentType(transformer: Transformer<SingleInput, T>,
                           validator: Validator<SingleInput>,
                           possibilitiesFunc: Possibilities,
                           defaultValue: T?,
                           typeInfo: TypeInfo<out T>): ArgumentType<SingleInput, T> =
        SingleArgumentType(transformer, validator, possibilitiesFunc, defaultValue, typeInfo)

object InputTypeValidator : Validator<Input> {
    override fun invoke(argumentType: ArgumentType<Input, *>, value: Input): Validation =
            TODO("dummy")

}