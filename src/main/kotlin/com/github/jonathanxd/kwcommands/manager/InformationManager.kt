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
     * If no one information can be found using [id][Information.Id.id]-[tags][Information.Id.tags] combination,
     * then the implementation should lookup by [id][Information.Id.id] only, if one information has the same
     * [id][Information.Id.id] as [id], then this information should be returned, if more than one information
     * has the same id, then `null` should be returned.
     *
     * @return Found information or `null` if information cannot be found.
     */
    fun <T> find(id: Information.Id, type: TypeInfo<T>): Information<T>?

    /**
     * Find a information by [id] and [type], this method will first lookup for `static information`, if no
     * one information is found for specified [id] and [type] and [useProviders] is `true`, it will return first
     * non-null information provided by a registered [InformationProvider].
     *
     * If no one information can be found using [id][Information.Id.id]-[tags][Information.Id.tags] combination,
     * then the implementation should lookup by [id][Information.Id.id] only, if one information has the same
     * [id][Information.Id.id] as [id], then this information should be returned, if more than one information
     * has the same id, then `null` should be returned.
     *
     * @return Found information or `null` if information cannot be found.
     */
    fun <T> find(id: Information.Id, type: TypeInfo<T>, useProviders: Boolean = true): Information<T>?

    /**
     * Same as [find], but returns an [Information.EMPTY] instead of a `null` reference if [Information] cannot be found.
     *
     * Make sure to check if returned information [Information.isNotEmpty], getting the value before checking it may
     * lead to cast exception.
     */
    fun <T> findOrEmpty(id: Information.Id, type: TypeInfo<T>): Information<T> =
            this.find(id, type) ?: Information.empty()

    /**
     * Same as [find], but returns an [Information.EMPTY] instead of a `null` reference if [Information] cannot be found.
     *
     * Make sure to check if returned information [Information.isNotEmpty], getting the value before checking it may
     * lead to cast exception.
     */
    fun <T> findOrEmpty(id: Information.Id, type: TypeInfo<T>, useProviders: Boolean = true): Information<T> =
            this.find(id, type, useProviders) ?: Information.empty()

    /**
     * Creates a safe copy of this manager, modifications on the copy does not affect this instance.
     */
    fun copy(): InformationManager
}