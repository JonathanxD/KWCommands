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
package com.github.jonathanxd.kwcommands.information

import com.github.jonathanxd.iutils.type.TypeInfo
import java.util.*

/**
 * Information holds a [value] of (reified) type [T] and provides a description. KWCommands has two types
 * of information, the first type is the `static information`, a `static information` must be provided before
 * command handling. The second type of information is `provided information`, the provided information is
 * provided by the [InformationProvider] and this information is requested during command handling.
 * [InformationProviders] will always lookup for static information first, if no static information is found, it will call
 * [InformationProvider.provide] of each registered provider and will return the first non-null provided information.
 *
 *
 * @param T Type of information
 * @property id Identification of information
 * @property value Stored value
 * @property description Optional description of this information.
 */
data class Information<out T>(val id: Information.Id<T>, val value: T, val description: String?) {

    /**
     * True if information is [EMPTY]
     */
    val isEmpty get() = this === EMPTY

    /**
     * True if information is not [EMPTY]
     */
    val isNotEmpty get() = this !== EMPTY

    override fun hashCode(): Int {
        return this.id.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return if (other != null && other is Information<*>) this.id == other.id else super.equals(
            other
        )
    }

    /**
     * Consumes the [value] if `this` [Information is not empty][Information.isNotEmpty]
     */
    fun consume(func: (T) -> Unit) {
        if (this.isNotEmpty)
            func(this.value)
    }

    /**
     * Identification of information
     *
     * @property type Type of identification value.
     * @property tags Identification tags.
     */
    data class Id<out T>(val type: TypeInfo<out T>, val tags: Array<out String>) {
        override fun hashCode(): Int =
            Objects.hash(type.hashCode(), Arrays.hashCode(tags))

        override fun equals(other: Any?): Boolean =
            if (other != null && other is Information.Id<*>) this.type == other.type
                    && Arrays.equals(this.tags, other.tags)
            else super.equals(other)

    }

    companion object {
        @JvmField
        val EMPTY = Information(Id(TypeInfo.of(Unit::class.java), emptyArray()), Unit, null)

        @Suppress("UNCHECKED_CAST")
        fun <T> empty(): Information<T> =
            EMPTY as Information<T>

        @JvmStatic
        fun <T> builder() = InformationBuilder<T>()
    }

}