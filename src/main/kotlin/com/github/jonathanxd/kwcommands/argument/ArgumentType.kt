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
package com.github.jonathanxd.kwcommands.argument

import com.github.jonathanxd.iutils.kt.typeInfo
import com.github.jonathanxd.iutils.type.TypeInfo
import com.github.jonathanxd.kwcommands.parser.*

abstract class ArgumentType<I : Input, out T>(
        val defaultValue: T?,
        val inputType: InputType<I>,
        val type: TypeInfo<out T>
) {
    abstract val parser: ArgumentParser<I, T>
    abstract val possibilities: Possibilities

    @Suppress("UNCHECKED_CAST")
    fun parse(input: Input): ValueOrValidation<T> =
            if (input.type.isCompatible(this.inputType))
                this.parser.parse(input as I, ValueOrValidationFactoryImpl(input, this, this.parser))
            else
                ValueOrValidation.Invalid(invalid(input, this, this.parser))

    fun possibilities(): List<Input> =
            this.possibilities.invoke()

    abstract fun hasType(index: Int): Boolean
    abstract fun getMapKeyType(index: Int): ArgumentType<*, *>
    abstract fun getMapValueType(index: Int): ArgumentType<*, *>
    abstract fun getListType(index: Int): ArgumentType<*, *>

}

/**
 * @property parser Argument value parser.
 * @property possibilities Possibilities of argument values.
 */
sealed class BaseArgumentType<I : Input, T>(
        override val parser: ArgumentParser<I, T>,
        override val possibilities: Possibilities,
        defaultValue: T?,
        inputType: InputType<I>,
        type: TypeInfo<out T>
) : ArgumentType<I, T>(defaultValue, inputType, type)

class SingleArgumentType<T>(
        parser: ArgumentParser<SingleInput, T>,
        possibilities: Possibilities,
        defaultValue: T?,
        type: TypeInfo<out T>
) : BaseArgumentType<SingleInput, T>(parser, possibilities, defaultValue, SingleInputType, type) {

    constructor(
            argumentTypeParser: ArgumentTypeHelper<SingleInput, T>,
            defaultValue: T?,
            type: TypeInfo<out T>
    ) :
            this(argumentTypeParser, argumentTypeParser, defaultValue, type)

    override fun getListType(index: Int): ArgumentType<*, *> = this
    override fun getMapKeyType(index: Int): ArgumentType<*, *> = this
    override fun getMapValueType(index: Int): ArgumentType<*, *> = this
    override fun hasType(index: Int): Boolean = true
}

class CustomArgumentType<out T, I : Input, V>(
        val converter: (V) -> T,
        defaultValue: T?,
        val argumentType: ArgumentType<I, V>,
        type: TypeInfo<T>
) : ArgumentType<I, T>(defaultValue, argumentType.inputType, type) {
    override val parser: ArgumentParser<I, T> = ConverterParser()
    override val possibilities: Possibilities = argumentType.possibilities

    inner class ConverterParser : ArgumentParser<I, T> {
        override fun parse(
                input: I,
                valueOrValidationFactory: ValueOrValidationFactory
        ): ValueOrValidation<T> =
                argumentType.parse(input).mapIfValue(converter)
    }

    override fun hasType(index: Int): Boolean =
            argumentType.hasType(index)

    override fun getListType(index: Int): ArgumentType<*, *> =
            argumentType.getListType(index)

    override fun getMapKeyType(index: Int): ArgumentType<*, *> =
            argumentType.getMapKeyType(index)

    override fun getMapValueType(index: Int): ArgumentType<*, *> =
            argumentType.getMapValueType(index)

}

object AnyArgumentType : ArgumentType<Input, Any?>(null, AnyInputType, typeInfo()) {
    override val parser: ArgumentParser<Input, Any?> = object : ArgumentParser<Input, Any?> {
        override fun parse(
                input: Input,
                valueOrValidationFactory: ValueOrValidationFactory
        ): ValueOrValidation<Any?> =
                valueOrValidationFactory.value(input.toPlain())

    }

    override val possibilities: Possibilities = object : Possibilities {
        override fun invoke(): List<Input> =
                emptyList()
    }

    override fun hasType(index: Int): Boolean = true
    override fun getListType(index: Int): ArgumentType<*, *> = this
    override fun getMapKeyType(index: Int): ArgumentType<*, *> = this
    override fun getMapValueType(index: Int): ArgumentType<*, *> = this

}

class ExactListArgumentType<T>(
        val elementTypes: List<ArgumentType<*, *>>,
        type: TypeInfo<out List<T>>
) : ArgumentType<ListInput, List<T>>(emptyList(), ListInputType, type) {
    override val parser: ArgumentParser<ListInput, List<T>> = ExactListParser(this)
    override val possibilities: Possibilities = ExactListPossibilities(this)

    override fun hasType(index: Int): Boolean =
            index < this.elementTypes.size

    override fun getListType(index: Int): ArgumentType<*, *> =
            elementTypes.getOrNull(index) ?: AnyArgumentType

    override fun getMapKeyType(index: Int): ArgumentType<*, *> =
            elementTypes.getOrNull(index) ?: AnyArgumentType

    override fun getMapValueType(index: Int): ArgumentType<*, *> =
            elementTypes.getOrNull(index) ?: AnyArgumentType
}

class ExactListParser<out T>(val listArgumentType: ExactListArgumentType<*>) :
        ArgumentParser<ListInput, List<T>> {

    override fun parse(input: ListInput, valueOrValidationFactory: ValueOrValidationFactory):
            ValueOrValidation<List<T>> {
        val vInput = input.input
        val elementTypes = listArgumentType.elementTypes
        val parsed =
                vInput.mapIndexed { index, inputToMap -> elementTypes[index].parse(inputToMap) }

        if (parsed.any { it.isInvalid })
            return parsed.flatToValidation()

        @Suppress("UNCHECKED_CAST")
        return valueOrValidationFactory.value(parsed.map { it.value } as List<T>)
    }
}

class ExactListPossibilities(val listArgumentType: ExactListArgumentType<*>) : Possibilities {
    override fun invoke(): List<Input> =
            listArgumentType.elementTypes.map { ListInput(it.possibilities()) }

}

class ListArgumentType<out T>(
        val elementType: ArgumentType<*, *>,
        type: TypeInfo<out List<T>>
) : ArgumentType<ListInput, List<T>>(emptyList(), ListInputType, type) {
    override val parser: ArgumentParser<ListInput, List<T>> = ListParser(this)
    override val possibilities: Possibilities = ListPossibilities(this)

    override fun hasType(index: Int): Boolean = true
    override fun getListType(index: Int): ArgumentType<*, *> = elementType
    override fun getMapKeyType(index: Int): ArgumentType<*, *> = elementType
    override fun getMapValueType(index: Int): ArgumentType<*, *> = elementType
}

class ListParser<out T>(val listArgumentType: ListArgumentType<*>) :
        ArgumentParser<ListInput, List<T>> {

    override fun parse(
            input: ListInput,
            valueOrValidationFactory: ValueOrValidationFactory
    ): ValueOrValidation<List<T>> {
        val parsed = input.input.map { this.listArgumentType.elementType.parse(it) }

        if (parsed.any { it.isInvalid })
            return parsed.flatToValidation()

        @Suppress("UNCHECKED_CAST")
        return valueOrValidationFactory.value(parsed.map { it.value } as List<T>)
    }
}

class ListPossibilities(val listArgumentType: ListArgumentType<*>) : Possibilities {
    override fun invoke(): List<Input> =
            listOf(ListInput(this.listArgumentType.elementType.possibilities()))
}

class MapArgumentType<K, V>(
        val keyType: ArgumentType<*, *>,
        val valueType: ArgumentType<*, *>,
        type: TypeInfo<out Map<K, V>>
) : ArgumentType<MapInput, Map<K, V>>(emptyMap(), MapInputType, type) {
    override val parser: ArgumentParser<MapInput, Map<K, V>> = MapParser(this)
    override val possibilities: Possibilities = MapPossibilities(this)

    override fun hasType(index: Int): Boolean = true
    override fun getListType(index: Int): ArgumentType<*, *> = keyType
    override fun getMapKeyType(index: Int): ArgumentType<*, *> = keyType
    override fun getMapValueType(index: Int): ArgumentType<*, *> = valueType

}

class MapParser<K, V>(val mapArgumentType: MapArgumentType<*, *>) :
        ArgumentParser<MapInput, Map<K, V>> {

    override fun parse(
            input: MapInput,
            valueOrValidationFactory: ValueOrValidationFactory
    ): ValueOrValidation<Map<K, V>> {
        val parsed = input.input.map {
            mapArgumentType.keyType.parse(it.first) to mapArgumentType.valueType.parse(it.second)
        }

        if (parsed.any { it.first.isInvalid || it.second.isInvalid })
            return parsed.map { listOf(it.first, it.second) }.flatMap { it }.flatToValidation()

        @Suppress("UNCHECKED_CAST")
        return valueOrValidationFactory.value(parsed.map { it.first.value to it.second.value }.toMap() as Map<K, V>)
    }
}


class MapPossibilities(val mapArgumentType: MapArgumentType<*, *>) : Possibilities {
    override fun invoke(): List<Input> =
            (this.mapArgumentType.keyType.possibilities() to
                    this.mapArgumentType.valueType.possibilities()).let { p ->
                val map = mutableListOf<Pair<Input, Input>>()

                for (input in p.first) {
                    for (input2 in p.second) {
                        map += input to input2
                    }
                }

                listOf(MapInput(map))
            }
}


class PairArgumentType<A, B>(
        val aPairType: ArgumentType<*, A>,
        val bPairType: ArgumentType<*, B>,
        val required: Boolean,
        type: TypeInfo<out Pair<A, B>>
) : ArgumentType<MapInput, Pair<A, B>>(null, MapInputType, type) {

    constructor(
            aPairType: ArgumentType<*, A>,
            bPairType: ArgumentType<*, B>,
            type: TypeInfo<out Pair<A, B>>
    ) : this(aPairType, bPairType, true, type)

    override val parser: ArgumentParser<MapInput, Pair<A, B>> = PairParser(this)
    override val possibilities: Possibilities = PairPossibilities(this)

    override fun hasType(index: Int): Boolean = true
    override fun getListType(index: Int): ArgumentType<*, *> = aPairType
    override fun getMapKeyType(index: Int): ArgumentType<*, *> = aPairType
    override fun getMapValueType(index: Int): ArgumentType<*, *> = bPairType
}

class PairParser<A, B>(val pairArgumentType: PairArgumentType<A, B>) :
        ArgumentParser<MapInput, Pair<A, B>> {
    override fun parse(
            input: MapInput,
            valueOrValidationFactory: ValueOrValidationFactory
    ): ValueOrValidation<Pair<A, B>> {
        val parsed = input.input.single().let { (a, b) ->
            this.pairArgumentType.aPairType.parse(a) to
                    this.pairArgumentType.bPairType.parse(b)
        }

        return when {
            parsed.first.isInvalid && parsed.second.isInvalid ->
                valueOrValidationFactory.invalid(parsed.first.validation + parsed.second.validation)
            parsed.first.isInvalid -> valueOrValidationFactory.invalid(parsed.first.validation)
            parsed.second.isInvalid -> valueOrValidationFactory.invalid(parsed.second.validation)
            else -> valueOrValidationFactory.value(parsed.first.value to parsed.second.value)
        }

    }
}

class PairPossibilities(val pairArgumentType: PairArgumentType<*, *>) : Possibilities {
    override fun invoke(): List<Input> =
            this.pairArgumentType.aPairType.possibilities() +
                    this.pairArgumentType.bPairType.possibilities()
}


class ComplexMapArgumentType<K, V>(
        val types: List<PairArgumentType<*, *>>,
        type: TypeInfo<out Map<K, V>>
) : ArgumentType<MapInput, Map<K, V>>(emptyMap(), MapInputType, type) {
    override val parser: ArgumentParser<MapInput, Map<K, V>> = ComplexMapParser(this)
    override val possibilities: Possibilities = ComplexMapPossibilities(this)

    override fun hasType(index: Int): Boolean =
            index < this.types.size

    override fun getListType(index: Int): ArgumentType<*, *> =
            types.getOrNull(index) ?: AnyArgumentType

    override fun getMapKeyType(index: Int): ArgumentType<*, *> =
            types.getOrNull(index)?.aPairType ?: AnyArgumentType

    override fun getMapValueType(index: Int): ArgumentType<*, *> =
            types.getOrNull(index)?.bPairType ?: AnyArgumentType
}

class ComplexMapParser<K, V>(val complexMapArgumentType: ComplexMapArgumentType<*, *>) :
        ArgumentParser<MapInput, Map<K, V>> {

    override fun parse(input: MapInput, valueOrValidationFactory: ValueOrValidationFactory):
            ValueOrValidation<Map<K, V>> {
        val parsed = input.input.mapIndexed { index, (k, v) ->
            complexMapArgumentType.types[index].parse(MapInput(listOf(k to v)))
        }

        if (parsed.any { it.isInvalid })
            return parsed.flatToValidation()

        @Suppress("UNCHECKED_CAST")
        return valueOrValidationFactory.value(parsed.map { it.value }.toMap() as Map<K, V>)
    }
}

class ComplexMapPossibilities(val complexMapArgumentType: ComplexMapArgumentType<*, *>) :
        Possibilities {
    override fun invoke(): List<Input> =
            this.complexMapArgumentType.types.map {
                it.possibilities() to it.possibilities()
            }.flatMap { p ->
                val list = mutableListOf<Pair<Input, Input>>()

                p.first.forEach { a ->
                    p.second.forEach { b ->
                        list += a to b
                    }
                }

                list
            }.let { listOf(MapInput(it)) }

}

fun <T, U> List<ValueOrValidation<T>>.flatToValidation(): ValueOrValidation<U> =
        this.filterIsInstance<ValueOrValidation.Invalid<T>>()
                .map { it.validation }
                .reduce { acc, either -> acc + either }
                .let { ValueOrValidation.Invalid(it) }


@JvmOverloads
fun <A, B> pairArgumentType(firstElementType: ArgumentType<*, A>,
                            secondElementType: ArgumentType<*, B>,
                            isRequired: Boolean = true): PairArgumentType<A, B> =
        PairArgumentType(firstElementType, secondElementType, isRequired, TypeInfo.builderOf(Pair::class.java).of(firstElementType.type, secondElementType.type).buildGeneric())

fun <T> listArgumentType(elementType: ArgumentType<*, T>): ListArgumentType<T> =
        ListArgumentType(elementType, TypeInfo.builderOf(List::class.java).of(elementType.type).buildGeneric());

fun <K, V> mapArgumentType(keyType: ArgumentType<*, K>, valueType: ArgumentType<*, V>): MapArgumentType<K, V> =
        MapArgumentType(keyType, valueType, TypeInfo.builderOf(Map::class.java).of(keyType.type, valueType.type).buildGeneric());