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

import com.github.jonathanxd.iutils.opt.Opt
import com.github.jonathanxd.iutils.type.TypeInfo
import com.github.jonathanxd.kwcommands.information.Information

/**
 * Builder of requirements.
 */
class RequirementBuilder<T, R> {

    private var required = Opt.none<R>()
    private lateinit var subject: Information.Id
    private lateinit var infoType: TypeInfo<in T>
    private lateinit var type: TypeInfo<out R>
    private lateinit var tester: RequirementTester<T, R>

    /**
     * Sets [Requirement.required]
     */
    fun required(required: R): RequirementBuilder<T, R> {
        this.required = Opt.some(required)
        return this
    }

    /**
     * Sets [Requirement.subject]
     */
    fun subject(subject: Information.Id): RequirementBuilder<T, R> {
        this.subject = subject
        return this
    }

    /**
     * Sets [Requirement.infoType]
     */
    fun infoType(infoType: TypeInfo<in T>): RequirementBuilder<T, R> {
        this.infoType = infoType
        return this
    }

    /**
     * Sets [Requirement.type]
     */
    fun type(type: TypeInfo<out R>): RequirementBuilder<T, R> {
        this.type = type
        return this
    }

    /**
     * Sets [Requirement.tester]
     */
    fun tester(tester: RequirementTester<T, R>): RequirementBuilder<T, R> {
        this.tester = tester
        return this
    }

    /**
     * Build [Requirement]
     */
    fun build(): Requirement<T, R> {
        if(!this.required.isPresent)
            throw IllegalStateException("Property 'required' should be initialized.")

        return Requirement(
                required = this.required.value,
                subject = this.subject,
                infoType = this.infoType,
                type = this.type,
                tester = this.tester
        )
    }

}