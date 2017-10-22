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
package com.github.jonathanxd.kwcommands.information

import com.github.jonathanxd.iutils.opt.Opt
import com.github.jonathanxd.iutils.opt.specialized.OptObject
import com.github.jonathanxd.iutils.type.TypeInfo

/**
 * Builder of [Information]
 */
class InformationBuilder<T> {

    private lateinit var id: Information.Id<T>
    private var value: OptObject<T> = Opt.none()
    private var description: String? = null

    /**
     * Sets [Information.id]
     */
    fun id(id: Information.Id<T>): InformationBuilder<T> {
        this.id = id
        return this
    }

    /**
     * Sets [Information.value]
     */
    fun value(value: T): InformationBuilder<T> {
        this.value = Opt.some(value)
        return this
    }

    /**
     * Sets [Information.description]
     */
    fun description(description: String?): InformationBuilder<T> {
        this.description = description
        return this
    }

    /**
     * Build [Information]
     */
    fun build(): Information<T> {
        if(value.isPresent)
            throw IllegalStateException("Property 'value' was not initialized")

        return Information(
                id = this.id,
                value = this.value.value,
                description = this.description
        )
    }
}