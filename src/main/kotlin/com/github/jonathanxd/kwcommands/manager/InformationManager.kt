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
package com.github.jonathanxd.kwcommands.manager

import com.github.jonathanxd.iutils.type.TypeInfo
import com.github.jonathanxd.kwcommands.information.Information
import com.github.jonathanxd.kwcommands.information.InformationProvider

/**
 * Register and provide information.
 */
interface InformationManager {

    /**
     * Static information set
     */
    val informationSet: Set<Information<*>>

    /**
     * Information providers.
     */
    val informationProviders: Set<InformationProvider>

    /**
     * Register a [static information][Information] with [id] and [description] with [value].
     */
    fun <T> registerInformation(id: Information.Id, value: T, valueType: TypeInfo<T>, description: String? = null): Boolean

    /**
     * Register a [static information][information].
     */
    fun registerInformation(information: Information<*>): Boolean

    /**
     * Unregister information with id [id].
     */
    fun unregisterInformation(id: Information.Id): Boolean

    /**
     * Register [informationProvider].
     *
     * @see InformationProvider
     */
    fun registerInformationProvider(informationProvider: InformationProvider): Boolean

    /**
     * Unregister [informationProvider].
     *
     * @see InformationProvider
     */
    fun unregisterInformationProvider(informationProvider: InformationProvider): Boolean

    /**
     * Find a information by [id] and [type], this method will first lookup for `static information`, if no
     * one information is found for specified [id] and [type], it will return first non-null information provided
     * by a registered [InformationProvider].
     *
     * @return Found information or null if information cannot be found.
     */
    fun <T> find(id: Information.Id, type: TypeInfo<T>): Information<T>?

    /**
     * Find a information by [id] and [type], this method will first lookup for `static information`, if no
     * one information is found for specified [id] and [type] and [useProviders] is `true`, it will return first
     * non-null information provided by a registered [InformationProvider].
     *
     * @return Found information or null if information cannot be found.
     */
    fun <T> find(id: Information.Id, type: TypeInfo<T>, useProviders: Boolean = true): Information<T>?

}