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

sealed class ArgumentType<I : Input, out T>(val defaultValue: T?,
                                            val inputType: InputType<I>,
                                            val type: TypeInfo<out T>) {
    protected abstract val transformer: Transformer<I, T>
    protected abstract val validator: Validator<I>
    protected abstract val possibilities: Possibilities

    @Suppress("UNCHECKED_CAST")
    fun transform(input: Input): T =
            this.transformer.invoke(input as I)

    @Suppress("UNCHECKED_CAST")
    fun validate(input: Input): Validation =
            this.validator.invoke(this, input as I)

    fun possibilities(): List<Input> =
            this.possibilities.invoke()
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
    : BaseArgumentType<SingleInput, T>(transformer, validator, possibilities, defaultValue, SingleInputType, type)

// TODO: Any argument type and handle ComplexMapArgumentType
// And add ExactListArgumentType to parse elements by respective ArgumentType

class AnyArgumentType: ArgumentType<Input, Any?>(null, AnyInputType, typeInfo()) {
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

}

class ExactListArgumentType<T>(val elementTypes: List<ArgumentType<*, *>>,
                               type: TypeInfo<out List<T>>)
    : ArgumentType<ListInput, List<T>>(emptyList(), ListInputType, type) {
    override val validator: Validator<ListInput> = ExactListValidator(this)
    override val transformer: Transformer<ListInput, List<T>> = ExactListTransformer(this)
    override val possibilities: Possibilities = ExactListPossibilities(this)
}

class ListArgumentType<T>(val elementType: ArgumentType<*, *>,
                          type: TypeInfo<out List<T>>)
    : ArgumentType<ListInput, List<T>>(emptyList(), ListInputType, type) {
    override val validator: Validator<ListInput> = ListValidator(this)
    override val transformer: Transformer<ListInput, List<T>> = ListTransformer(this)
    override val possibilities: Possibilities = ListPossibilities(this)
}

class MapArgumentType<K, V>(val keyType: ArgumentType<*, *>,
                            val valueType: ArgumentType<*, *>,
                            type: TypeInfo<out Map<K, V>>)
    : ArgumentType<MapInput, Map<K, V>>(emptyMap(), MapInputType, type) {
    override val possibilities: Possibilities = MapPossibilities(this)
    override val transformer: Transformer<MapInput, Map<K, V>> = MapTransformer(this)
    override val validator: Validator<MapInput> = MapValidator(this)
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
}

class ComplexMapArgumentType<K, V>(val types: List<PairArgumentType<*, *>>,
                                   type: TypeInfo<out Map<K, V>>)
    : ArgumentType<MapInput, Map<K, V>>(emptyMap(), MapInputType, type) {
    override val transformer: Transformer<MapInput, Map<K, V>> = ComplexMapTransformer(this)
    override val validator: Validator<MapInput> = ComplexMapValidator(this)
    override val possibilities: Possibilities = ComplexMapPossibilities(this)
}

class ExactListValidator(val listArgumentType: ExactListArgumentType<*>) : Validator<ListInput> {
    override fun invoke(argumentType: ArgumentType<ListInput, *>, value: ListInput): Validation {
        val input = value.input
        val elementTypes = listArgumentType.elementTypes

        if (input.size < elementTypes.size) {
            val missing = elementTypes.subList(input.size, elementTypes.size)

            return invalid(value, listArgumentType, this, null,
                    missing.map { it.inputType })
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

        val validations = value.input.map { (k, v) ->
            val m = this.complexMapArgumentType.types.map {
                it.aPairType.validate(k) + it.bPairType.validate(v)
            }

            if (m.none { it.isValid })
                m
            else emptyList()
        }.flatMap { it }.filter { it.isInvalid }.flatMap { it.invalids }

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
            value.input.map { (k, v) ->
                getTransformer(k).transform(MapInput(listOf(k to v)))
            }.toMap() as Map<K, V>

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

/*
@Suppress("UNCHECKED_CAST")
fun Validator<*>.invokeAsInputCapable(argumentType: ArgumentType<Input, *>, input: Input): Validation =
        (this as Validator<Input>).invoke(argumentType, input)
*/

@Suppress("UNCHECKED_CAST")
fun <I : Input> Validator<I>.invokeAsInputCapable(argumentType: ArgumentType<I, *>, input: Input): Validation =
        this.invoke(argumentType, input as I)

@Suppress("UNCHECKED_CAST")
fun Transformer<*, *>.invokeAsInputCapable(input: Input): Any? =
        (this as Transformer<Input, Any?>).invoke(input)

@Suppress("UNCHECKED_CAST")
fun <T> Transformer<*, T>.invokeAsInputCapableChecked(input: Input): T =
        (this as Transformer<Input, T>).invoke(input)