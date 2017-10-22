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
package com.github.jonathanxd.kwcommands.requirement

import com.github.jonathanxd.iutils.type.AbstractTypeInfo
import com.github.jonathanxd.iutils.type.TypeInfo
import com.github.jonathanxd.kwcommands.information.Information

/**
 * Requirement of a information.
 *
 * @param T Information value type.
 * @param R Required value type.
 */
data class Requirement<T, R>(val required: R,
                             val subject: Information.Id<T>,
                             val type: TypeInfo<out R>,
                             val tester: RequirementTester<T, R>) {

    /**
     * Calls the [RequirementTester.test] method.
     */
    fun test(information: Information<T>) = this.tester.test(this, information)

    companion object {
        inline fun <reified T, reified R> create(required: R, subject: Information.Id<T>, tester: RequirementTester<T, R>) =
                Requirement(required, subject, object : AbstractTypeInfo<R>() {}, tester)

        inline fun <reified T, reified R> create(required: R, tags: Array<String>, tester: RequirementTester<T, R>) =
                Requirement(required, Information.Id(object : AbstractTypeInfo<T>() {}, tags),
                        object : AbstractTypeInfo<R>() {}, tester)

        @JvmStatic
        fun <T, R> builder() = RequirementBuilder<T, R>()
    }
}