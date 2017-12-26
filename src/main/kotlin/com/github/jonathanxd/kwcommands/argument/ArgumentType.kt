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
package com.github.jonathanxd.kwcommands.argument

import com.github.jonathanxd.iutils.type.TypeInfo
import com.github.jonathanxd.jwiutils.kt.typeInfo
import com.github.jonathanxd.kwcommands.parser.*

abstract class ArgumentType<I : Input, out T>(val defaultValue: T?,
                                              val inputType: InputType<I>,
                                              val type: TypeInfo<out T>) {
    abstract val transformer: Transformer<I, T>
    abstract val validator: Validator<I>
    abstract val possibilities: Possibilities

    @Suppress("UNCHECKED_CAST")
    fun transform(input: Input): T =
            this.transformer.invoke(input as I)

    @Suppress("UNCHECKED_CAST")
    fun validate(input: Input): Validation =
            if (input.type.isCompatible(this.inputType))
                this.validator.invoke(this, input as I)
            else invalid(input, this, this.validator, null)

    fun possibilities(): List<Input> =
            this.possibilities.invoke()

    abstract fun hasType(index: Int): Boolean
    abstract fun getMapKeyType(index: Int): ArgumentType<*, *>
    abstract fun getMapValueType(index: Int): ArgumentType<*, *>
    abstract fun getListType(index: Int): ArgumentType<*, *>

}

/**
 * @property validator Argument value validator.
 * @property transformer Transformer of argument to a object of type [T].
 * @property possibilities Possibilities of argument values.
 */
sealed class BaseArgumentType<I : Input, T>(
        override val transformer: Transformer<I, T>,
        override val validator: Validator<I>,
        override val possibilities: Possibilities,
        defaultValue: T?,
        inputType: InputType<I>,
        type: TypeInfo<out T>) : ArgumentType<I, T>(defaultValue, inputType, type)

class SingleArgumentType<T>(
        transformer: Transformer<SingleInput, T>,
        validator: Validator<SingleInput>,
        possibilities: Possibilities,
        defaultValue: T?,
        type: TypeInfo<out T>)
    : BaseArgumentType<SingleInput, T>(transformer, validator, possibilities, defaultValue, SingleInputType, type) {

    constructor(argumentTypeParser: ArgumentTypeParser<SingleInput, T>, defaultValue: T?, type: TypeInfo<out T>):
            this(argumentTypeParser, argumentTypeParser, argumentTypeParser, defaultValue, type)

    override fun getListType(index: Int): ArgumentType<*, *> = this
    override fun getMapKeyType(index: Int): ArgumentType<*, *> = this
    override fun getMapValueType(index: Int): ArgumentType<*, *> = this
    override fun hasType(index: Int): Boolean = true
}

class CustomArgumentType<out T, I: Input, V>(
        val converter: (V) -> T,
        defaultValue: T?,
        val argumentType: ArgumentType<I, V>,
        type: TypeInfo<T>): ArgumentType<I, T>(defaultValue, argumentType.inputType, type) {
    override val transformer: Transformer<I, T> = ConverterTransformer()
    override val validator: Validator<I> = argumentType.validator
    override val possibilities: Possibilities = argumentType.possibilities

    inner class ConverterTransformer : Transformer<I, T> {
        override fun invoke(value: I): T =
                converter(argumentType.transform(value))
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
    override val validator: Validator<Input> = object : Validator<Input> {
        override fun invoke(argumentType: ArgumentType<Input, *>, value: Input): Validation =
                valid()
    }

    override val transformer: Transformer<Input, Any?> = object : Transformer<Input, Any?> {
        override fun invoke(value: Input): Any? =
                value.toPlain()
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

class ExactListArgumentType<T>(val elementTypes: List<ArgumentType<*, *>>,
                               type: TypeInfo<out List<T>>)
    : ArgumentType<ListInput, List<T>>(emptyList(), ListInputType, type) {
    override val validator: Validator<ListInput> = ExactListValidator(this)
    override val transformer: Transformer<ListInput, List<T>> = ExactListTransformer(this)
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

class ListArgumentType<out T>(val elementType: ArgumentType<*, *>,
                              type: TypeInfo<out List<T>>)
    : ArgumentType<ListInput, List<T>>(emptyList(), ListInputType, type) {
    override val validator: Validator<ListInput> = ListValidator(this)
    override val transformer: Transformer<ListInput, List<T>> = ListTransformer(this)
    override val possibilities: Possibilities = ListPossibilities(this)

    override fun hasType(index: Int): Boolean = true
    override fun getListType(index: Int): ArgumentType<*, *> = elementType
    override fun getMapKeyType(index: Int): ArgumentType<*, *> = elementType
    override fun getMapValueType(index: Int): ArgumentType<*, *> = elementType
}

class MapArgumentType<K, V>(val keyType: ArgumentType<*, *>,
                            val valueType: ArgumentType<*, *>,
                            type: TypeInfo<out Map<K, V>>)
    : ArgumentType<MapInput, Map<K, V>>(emptyMap(), MapInputType, type) {
    override val possibilities: Possibilities = MapPossibilities(this)
    override val transformer: Transformer<MapInput, Map<K, V>> = MapTransformer(this)
    override val validator: Validator<MapInput> = MapValidator(this)

    override fun hasType(index: Int): Boolean = true
    override fun getListType(index: Int): ArgumentType<*, *> = keyType
    override fun getMapKeyType(index: Int): ArgumentType<*, *> = keyType
    override fun getMapValueType(index: Int): ArgumentType<*, *> = valueType

}

class PairArgumentType<A, B>(val aPairType: ArgumentType<*, A>,
                             val bPairType: ArgumentType<*, B>,
                             val required: Boolean,
                             type: TypeInfo<out Pair<A, B>>)
    : ArgumentType<MapInput, Pair<A, B>>(null, MapInputType, type) {

    constructor(aPairType: ArgumentType<*, A>,
                bPairType: ArgumentType<*, B>,
                type: TypeInfo<out Pair<A, B>>) : this(aPairType, bPairType, true, type)

    override val transformer: Transformer<MapInput, Pair<A, B>> = PairTransformer(this)
    override val validator: Validator<MapInput> = PairValidator(this)
    override val possibilities: Possibilities = PairPossibilities(this)

    override fun hasType(index: Int): Boolean = true
    override fun getListType(index: Int): ArgumentType<*, *> = aPairType
    override fun getMapKeyType(index: Int): ArgumentType<*, *> = aPairType
    override fun getMapValueType(index: Int): ArgumentType<*, *> = bPairType
}

class ComplexMapArgumentType<K, V>(val types: List<PairArgumentType<*, *>>,
                                   type: TypeInfo<out Map<K, V>>)
    : ArgumentType<MapInput, Map<K, V>>(emptyMap(), MapInputType, type) {
    override val transformer: Transformer<MapInput, Map<K, V>> = ComplexMapTransformer(this)
    override val validator: Validator<MapInput> = ComplexMapValidator(this)
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

class ExactListValidator(val listArgumentType: ExactListArgumentType<*>) : Validator<ListInput> {
    override fun invoke(argumentType: ArgumentType<ListInput, *>, value: ListInput): Validation {
        val input = value.input
        val elementTypes = listArgumentType.elementTypes

        if (input.size < elementTypes.size) {
            return invalid(value, listArgumentType, this, null)
        }

        val invalids = input.mapIndexed { index_, input_ ->
            elementTypes[index_].validate(input_).invalids
        }.flatMap { it }

        if (invalids.isNotEmpty())
            return Validation(invalids)

        return valid()
    }
}

class ExactListTransformer<out T>(val listArgumentType: ExactListArgumentType<*>) : Transformer<ListInput, List<T>> {

    @Suppress("UNCHECKED_CAST")
    override fun invoke(value: ListInput): List<T> {
        val vInput = value.input
        val elements = listArgumentType.elementTypes

        return vInput.mapIndexed { index, input ->
            elements[index].transform(input)
        } as List<T>
    }
}

class ExactListPossibilities(val listArgumentType: ExactListArgumentType<*>) : Possibilities {
    override fun invoke(): List<Input> =
            listArgumentType.elementTypes.map { ListInput(it.possibilities()) }

}

class ListValidator(val listArgumentType: ListArgumentType<*>) : Validator<ListInput> {
    override fun invoke(argumentType: ArgumentType<ListInput, *>, value: ListInput): Validation {
        val validations = value.input.map {
            listArgumentType.elementType.validate(it)
        }.filter { it.isInvalid }.flatMap { it.invalids }

        if (validations.isNotEmpty())
            return Validation(validations)

        return valid()
    }
}

class ListTransformer<out T>(val listArgumentType: ListArgumentType<*>) : Transformer<ListInput, List<T>> {

    @Suppress("UNCHECKED_CAST")
    override fun invoke(value: ListInput): List<T> =
            value.input.map { this.listArgumentType.elementType.transform(it) } as List<T>
}

class ListPossibilities(val listArgumentType: ListArgumentType<*>) : Possibilities {
    override fun invoke(): List<Input> =
            listOf(ListInput(this.listArgumentType.elementType.possibilities()))
}

class MapValidator(val mapArgumentType: MapArgumentType<*, *>) : Validator<MapInput> {
    override fun invoke(argumentType: ArgumentType<MapInput, *>, value: MapInput): Validation {

        val validations = value.input.map { (k, v) ->
            mapArgumentType.keyType.validate(k) +
                    mapArgumentType.valueType.validate(v)
        }.filter { it.isInvalid }.flatMap { it.invalids }

        if (validations.isNotEmpty())
            return Validation(validations)

        return valid()
    }
}

class MapTransformer<K, V>(val mapArgumentType: MapArgumentType<*, *>) : Transformer<MapInput, Map<K, V>> {
    @Suppress("UNCHECKED_CAST")
    override fun invoke(value: MapInput): Map<K, V> =
            value.input.map { (k, v) ->
                this.mapArgumentType.keyType.transform(k) to
                        this.mapArgumentType.valueType.transform(v)
            }.toMap() as Map<K, V>

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

class PairValidator<A, B>(val pairArgumentType: PairArgumentType<A, B>) : Validator<MapInput> {
    override fun invoke(argumentType: ArgumentType<MapInput, *>, value: MapInput): Validation =
            value.input.single().let { (a, b) ->
                this.pairArgumentType.aPairType.validate(a) +
                        this.pairArgumentType.bPairType.validate(b)
            }

}

class PairTransformer<A, B>(val pairArgumentType: PairArgumentType<A, B>) : Transformer<MapInput, Pair<A, B>> {
    override fun invoke(value: MapInput): Pair<A, B> =
            value.input.single().let { (a, b) ->
                this.pairArgumentType.aPairType.transform(a) to
                        this.pairArgumentType.bPairType.transform(b)
            }
}

class PairPossibilities(val pairArgumentType: PairArgumentType<*, *>) : Possibilities {
    override fun invoke(): List<Input> =
            this.pairArgumentType.aPairType.possibilities() +
                    this.pairArgumentType.bPairType.possibilities()
}

class ComplexMapValidator(val complexMapArgumentType: ComplexMapArgumentType<*, *>) : Validator<MapInput> {
    override fun invoke(argumentType: ArgumentType<MapInput, *>, value: MapInput): Validation {

        val validations = value.input.mapIndexed { index, (k, v) ->
            val type = this.complexMapArgumentType.types[index]
            type.aPairType.validate(k) + type.bPairType.validate(v)
        }/*.flatMap { it }*/.filter { it.isInvalid }.flatMap { it.invalids }

        /*
         { (k, v) ->
            val m = this.complexMapArgumentType.types.map {
                it.aPairType.validate(k) + it.bPairType.validate(v)
            }

            if (m.none { it.isValid })
                m
            else emptyList()
        }
         */

        if (validations.isNotEmpty())
            return Validation(validations)

        return valid()
    }
}

class ComplexMapTransformer<K, V>(val complexMapArgumentType: ComplexMapArgumentType<*, *>)
    : Transformer<MapInput, Map<K, V>> {
    private fun getTransformer(value: Input): PairArgumentType<*, *> =
            complexMapArgumentType.types.first { it.validate(value).isValid }

    @Suppress("UNCHECKED_CAST")
    override fun invoke(value: MapInput): Map<K, V> =
            value.input.mapIndexed { index, (k, v) ->
                complexMapArgumentType.types[index].transform(MapInput(listOf(k to v)))
                //getTransformer(k).transform(MapInput(listOf(k to v)))
            }.toMap() as Map<K, V>

    /*
     { (k, v) ->
                getTransformer(k).transform(MapInput(listOf(k to v)))
            }.toMap()
     */
}

class ComplexMapPossibilities(val complexMapArgumentType: ComplexMapArgumentType<*, *>) : Possibilities {
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