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
package com.github.jonathanxd.kwcommands.test

import com.github.jonathanxd.iutils.text.Text
import com.github.jonathanxd.jwiutils.kt.ifLeftSide
import com.github.jonathanxd.kwcommands.argument.Argument
import com.github.jonathanxd.kwcommands.argument.ArgumentContainer
import com.github.jonathanxd.kwcommands.parser.*
import com.github.jonathanxd.kwcommands.util.EnumPossibilitiesFunc
import com.github.jonathanxd.kwcommands.util.EnumValidator
import com.github.jonathanxd.kwcommands.util.get

class MyMapValidator : Validator {

    val languagesValidator = EnumValidator(Languages::class.java)

    override fun invoke(parsed: List<ArgumentContainer<*>>, current: Argument<*>, value: Input): Validation {
        if (value !is MapInput)
            return invalid(value, this, Text.of("Expected map"), MapInputType)

        val input = value.input

        if (input.isEmpty())
            invalid(value, this, Text.of("Input is empty"), MapInputType)

        value.get("name", SingleInputType, this).also {
            if (it.isLeft)
                return it.left
        }

        val getValues = value.get("values", MapInputType, this)
        getValues.ifLeftSide {
            return it
        }

        val getLanguages = getValues.mapRight { it.second as MapInput }.right
                .get("languages", ListInputType, this)

        if (getLanguages.isRight) {
            val languages = getLanguages.right.second as ListInput

            val validate = languagesValidator.invoke(parsed, current, languages)

            if (validate.isInvalid)
                return validate
        }

        return invalid(value, this, Text.of("Invalid map"), MapInputType)
    }

}

class MyMapPossibilities : Possibilities {

    val enumPossibilities = EnumPossibilitiesFunc(Languages::class.java)

    override fun invoke(parsed: List<ArgumentContainer<*>>,
                        current: Argument<*>): List<Input> {
        return listOf(MapInput(listOf(
                SingleInput("name") to SingleInput(""),
                SingleInput("values") to MapInput(listOf(
                        SingleInput("age") to SingleInput(""),
                        SingleInput("languages") to ListInput(enumPossibilities.invoke(parsed, current))
                ))
        )))

    }

}

class MyMapTransformer : Transformer<Map<String, Any>> {
    override fun invoke(parsed: List<ArgumentContainer<*>>, current: Argument<*>, value: Input): Map<String, Any> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}

enum class Languages {
    Java,
    Kotlin,
    JavaScript,
    Go,
    Rust
}