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
package com.github.jonathanxd.kwcommands.util

import com.github.jonathanxd.iutils.type.TypeInfo
import com.github.jonathanxd.kwcommands.argument.*
import com.github.jonathanxd.kwcommands.dsl.stringParser
import com.github.jonathanxd.kwcommands.parser.ArgumentParser
import com.github.jonathanxd.kwcommands.parser.Input
import com.github.jonathanxd.kwcommands.parser.Possibilities
import com.github.jonathanxd.kwcommands.parser.SingleInput
import java.util.*


val charArgumentType = SingleArgumentType<Char>(
    CharParser,
    CharPossibilities,
    null,
    TypeInfo.of(Char::class.java)
)

val byteArgumentType = SingleArgumentType<Byte>(
    ByteParser,
    BytePossibilities,
    null,
    TypeInfo.of(Byte::class.java)
)

val shortArgumentType = SingleArgumentType<Short>(
    ShortParser,
    ShortPossibilities,
    null,
    TypeInfo.of(Short::class.java)
)

val intArgumentType = SingleArgumentType<Int>(
    IntParser,
    IntPossibilities,
    null,
    TypeInfo.of(Int::class.java)
)

val longArgumentType = SingleArgumentType<Long>(
    LongParser,
    LongPossibilities,
    null,
    TypeInfo.of(Long::class.java)
)

val doubleArgumentType = SingleArgumentType<Double>(
    DoubleParser,
    DoublePossibilities,
    null,
    TypeInfo.of(Double::class.java)
)

val floatArgumentType = SingleArgumentType<Float>(
    FloatParser,
    FloatPossibilities,
    null,
    TypeInfo.of(Float::class.java)
)

val booleanArgumentType = SingleArgumentType<Boolean>(
    BooleanParser,
    BooleanPossibilities,
    null,
    TypeInfo.of(Boolean::class.java)
)

val stringArgumentType = SingleArgumentType<String>(
    stringParser,
    StringPossibilities,
    null,
    TypeInfo.of(String::class.java)
)

val anyArgumentType = AnyArgumentType

fun <I : Input, T> optArgumentType(argumentType: ArgumentType<I, T>): ArgumentType<I, Optional<T>> =
    CustomArgumentType(
        { Optional.of(it) }, Optional.empty(), argumentType,
        TypeInfo.builderOf(Optional::class.java).of(argumentType.type).buildGeneric()
    )

fun <I : Input> optIntArgumentType(argumentType: ArgumentType<I, Int>): ArgumentType<I, OptionalInt> =
    CustomArgumentType(
        { OptionalInt.of(it) }, OptionalInt.empty(), argumentType,
        TypeInfo.builderOf(OptionalInt::class.java).build()
    )

fun <I : Input> optDoubleArgumentType(argumentType: ArgumentType<I, Double>): ArgumentType<I, OptionalDouble> =
    CustomArgumentType(
        { OptionalDouble.of(it) }, OptionalDouble.empty(), argumentType,
        TypeInfo.builderOf(OptionalDouble::class.java).build()
    )

fun <I : Input> optLongArgumentType(argumentType: ArgumentType<I, Long>): ArgumentType<I, OptionalLong> =
    CustomArgumentType(
        { OptionalLong.of(it) }, OptionalLong.empty(), argumentType,
        TypeInfo.builderOf(OptionalLong::class.java).build()
    )

fun stringArgumentType() = SingleArgumentType<String>(
    stringParser,
    StringPossibilities,
    null,
    TypeInfo.of(String::class.java)
)

fun stringArgumentType(str: String) = SingleArgumentType<String>(
    ExactStringParser(str),
    ExactStringPossibilities(str),
    null,
    TypeInfo.of(String::class.java)
)

fun <T> enumArgumentType(enumType: Class<T>) = SingleArgumentType<T>(
    EnumParser(enumType),
    EnumPossibilities(enumType),
    null,
    TypeInfo.of(enumType)
)

fun <T> simpleArgumentType(
    parser: ArgumentParser<SingleInput, T>,
    possibilities: Possibilities,
    typeInfo: TypeInfo<out T>
): ArgumentType<SingleInput, T> =
    SingleArgumentType(parser, possibilities, null, typeInfo)

fun <T> simpleArgumentType(
    helper: ArgumentTypeHelper<SingleInput, T>,
    defaultValue: T?,
    typeInfo: TypeInfo<out T>
): ArgumentType<SingleInput, T> =
    SingleArgumentType(helper, defaultValue, typeInfo)

fun <T> simpleArgumentType(
    helper: ArgumentTypeHelper<SingleInput, T>,
    typeInfo: TypeInfo<out T>
): ArgumentType<SingleInput, T> =
    SingleArgumentType(helper, null, typeInfo)

fun <T> simpleArgumentType(
    parser: ArgumentParser<SingleInput, T>,
    possibilities: Possibilities,
    defaultValue: T?,
    typeInfo: TypeInfo<out T>
): ArgumentType<SingleInput, T> =
    SingleArgumentType(parser, possibilities, defaultValue, typeInfo)

