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

import com.github.jonathanxd.iutils.type.TypeInfo
import com.github.jonathanxd.kwcommands.manager.InformationProviders

/**
 * Information provider, a provider is able to provide different types of information,
 * the main difference between provided information and static information is that provided
 * information can vary depending of the moment that command is called.
 *
 * [provide] method will be called whenever the command is called and is only called if a
 * static information for the id is not found.
 */
@FunctionalInterface
interface InformationProvider {

    /**
     * Provides information for [type] and [tags].
     *
     * @param type Type of requested information.
     * @param tags Tags of requested information.
     * @return Information or null if this provider cannot provide a information for provided [type] and [args].
     */
    fun <T> provide(type: TypeInfo<out T>, tags: Array<out String>, manager: InformationProviders): Information<T>?

    /**
     * Provides information for [id].
     *
     * @param id Id of requested information
     * @return Information or null if this provider cannot provide a information of [id].
     */
    fun <T> provide(id: Information.Id<T>, manager: InformationProviders): Information<T>? =
            this.provide(id.type, id.tags, manager)

    companion object {
        /**
         * Creates unsafe [InformationProvider].
         * `null` is provided to [type] when a information is requested only by [id].
         */
        @Suppress("UNCHECKED_CAST")
        fun unsafe(provider: (id: Information.Id<*>,
                              manager: InformationProviders) -> Information<*>?): InformationProvider =
                object : InformationProvider {
                    override fun <T> provide(type: TypeInfo<out T>,
                                             tags: Array<out String>, manager: InformationProviders): Information<T>? =
                            provider(Information.Id(type, tags), manager) as Information<T>?

                }

        /**
         * Creates unsafe [InformationProvider].
         */
        @Suppress("UNCHECKED_CAST")
        fun unsafe(provider: (type: TypeInfo<*>,
                              tags: Array<out String>,
                              manager: InformationProviders) -> Information<*>?): InformationProvider =
                object : InformationProvider {
                    override fun <T> provide(type: TypeInfo<out T>,
                                             tags: Array<out String>,
                                             manager: InformationProviders): Information<T>? =
                            provider(type, tags, manager) as Information<T>?
                }

        /**
         * Creates a safe [InformationProvider] that is safe for [stype].
         */
        @Suppress("UNCHECKED_CAST")
        fun <U> safeFor(stype: TypeInfo<U>,
                        provider: (id: Information.Id<U>,
                                   manager: InformationProviders) -> Information<U>?): InformationProvider =
                object : InformationProvider {
                    override fun <T> provide(type: TypeInfo<out T>,
                                             tags: Array<out String>,
                                             manager: InformationProviders): Information<T>? {
                        if (stype == type)
                            return provider(Information.Id(type, tags) as Information.Id<U>, manager) as Information<T>?

                        return null
                    }
                }

        /**
         * Creates a safe [InformationProvider] that is safe for [stype].
         */
        @Suppress("UNCHECKED_CAST")
        fun <U> safeFor(stype: TypeInfo<U>,
                        provider: (type: TypeInfo<out U>,
                                   tags: Array<out String>,
                                   manager: InformationProviders) -> Information<U>?): InformationProvider =
                object : InformationProvider {
                    override fun <T> provide(type: TypeInfo<out T>,
                                             tags: Array<out String>,
                                             manager: InformationProviders): Information<T>? {
                        if (stype == type)
                            return provider(type as TypeInfo<out U>, tags, manager) as Information<T>?

                        return null
                    }
                }
    }

}