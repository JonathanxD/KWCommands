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
package com.github.jonathanxd.kwcommands.reflect.annotation

import com.github.jonathanxd.kwcommands.information.Information
import com.github.jonathanxd.kwcommands.requirement.RequirementTester
import java.util.function.Supplier
import kotlin.reflect.KClass

/**
 * Requires a [subject information][subject] [value][Information.value] to match [required] (or required provided
 * by [requiredProvider]) according to [data tester][testerType].
 *
 * @property subject Subject information id.
 * @property testerType Type of the [RequirementTester] (must be a singleton class).
 * @property required Requirement, for string requirements.
 * @property requiredProvider Provider of required value.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER)
@Suppress("DEPRECATED_JAVA_ANNOTATION")
@java.lang.annotation.Repeatable(Requires::class)
annotation class Require(
    val subject: Id = Id(),
    val testerType: KClass<out RequirementTester<*, *>>,
    val required: String = "",
    val requiredProvider: KClass<out Supplier<*>> = DefaultRequiredProvider::class
)

object DefaultRequiredProvider : Supplier<Any> {
    override fun get(): Any = throw IllegalStateException("Default provider")

}