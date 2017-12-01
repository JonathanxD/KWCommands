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

import com.github.jonathanxd.iutils.`object`.Default
import com.github.jonathanxd.iutils.collection.Comparators3
import com.github.jonathanxd.iutils.type.TypeInfoSortComparator
import com.github.jonathanxd.jwiutils.kt.typeInfo
import com.github.jonathanxd.kwcommands.information.Information
import com.github.jonathanxd.kwcommands.information.InformationProvider
import java.util.*
import java.util.function.Function

/**
 * Common implementation of [InformationManager].
 */
class InformationManagerImpl : InformationManager {

    private val informationSet_ = mutableSetOf<Information<*>>()
    private val informationProviders_ = mutableSetOf<InformationProvider>()

    override val informationSet: Set<Information<*>> = Collections.unmodifiableSet(this.informationSet_)
    override val informationProviders: Set<InformationProvider> = Collections.unmodifiableSet(this.informationProviders_)

    init {
        this.registerInformation(INFORMATION_MANAGER_ID, this, "Information manager")
    }

    override fun <T> registerInformation(id: Information.Id<T>, value: T, description: String?): Boolean =
            this.informationSet_.add(Information(id, value, description))

    override fun registerInformation(information: Information<*>): Boolean =
            this.informationSet_.add(information)

    override fun unregisterInformation(id: Information.Id<*>): Boolean =
            this.informationSet_.removeIf { it.id == id }

    override fun registerInformationProvider(informationProvider: InformationProvider): Boolean =
            this.informationProviders_.add(informationProvider)

    override fun unregisterInformationProvider(informationProvider: InformationProvider): Boolean =
            this.informationProviders_.remove(informationProvider)

    @Suppress("UNCHECKED_CAST")
    override fun <T> find(id: Information.Id<T>, useProviders: Boolean): Information<T>? {

        val assignFound = mutableListOf<Information<*>>()

        informationSet_.forEach {
            val itId = it.id

            val tagsMatch = id.tags.isEmpty() || id.tags.all { itId.tags.contains(it) }

            if ((id.type == Default::class.java || id.type == itId.type)
                    && tagsMatch)
                return it as Information<T>

            if (tagsMatch && id.type.isAssignableFrom(itId.type))
                assignFound += it
        }

        if (assignFound.isNotEmpty()) {
            return assignFound.maxWith(Comparators3.map(comparator, Function { it.id.type })) as Information<T>
        }

        if (!useProviders)
            return null

        this.informationProviders_.forEach {
            it.provide(id, this)?.let {
                return it
            }
        }

        return null
    }

    override fun copy(): InformationManager {
        val newManager = InformationManagerImpl()

        newManager.informationSet_.addAll(this.informationSet_)
        newManager.informationProviders_.addAll(this.informationProviders_)

        return newManager
    }

    companion object {
        internal val comparator = TypeInfoSortComparator()
    }

}

/**
 * Empty information manager.
 */
object InformationManagerVoid : InformationManager {

    override val informationSet: Set<Information<*>> get() = emptySet()
    override val informationProviders: Set<InformationProvider> get() = emptySet()
    override fun <T> registerInformation(id: Information.Id<T>, value: T, description: String?): Boolean = false
    override fun registerInformation(information: Information<*>): Boolean = true
    override fun unregisterInformation(id: Information.Id<*>): Boolean = true
    override fun registerInformationProvider(informationProvider: InformationProvider): Boolean = true
    override fun unregisterInformationProvider(informationProvider: InformationProvider): Boolean = true
    override fun <T> find(id: Information.Id<T>, useProviders: Boolean): Information<T>? = null
    override fun copy(): InformationManager = this
}