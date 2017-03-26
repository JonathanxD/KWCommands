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

import com.github.jonathanxd.iutils.type.TypeInfo
import com.github.jonathanxd.kwcommands.command.CommandContainer
import com.github.jonathanxd.kwcommands.information.Information
import com.github.jonathanxd.kwcommands.manager.InformationManager

/**
 * Requirement of a information. This class test a subject [Information] against requirements and throws [RequirementNotSatisfiedException]
 * if the information does not satisfies this requirement.
 */
@FunctionalInterface
interface Requirement<in T> {

    /**
     * Value type of requirement
     */
    val type: TypeInfo<in T>

    /**
     * Test the [subject] against this [requirement][Requirement] and throws [RequirementNotSatisfiedException]
     * if the [subject] does not satisfies this requirement.
     *
     * @param required Required value.
     * @param subject Subject to test requirements.
     * @param commandContainer Fired command.
     * @param informationManager Information manager.
     * @throws RequirementNotSatisfiedException If [subject] does not satisfies this requirement.
     */
    fun test(required: T, subject: Information<T>, commandContainer: CommandContainer, informationManager: InformationManager)

}