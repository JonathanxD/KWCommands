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
import java.util.*

/**
 * Common implementation of [InformationManager].
 */
class InformationManagerImpl : InformationManager {

    private val informationSet_ = mutableSetOf<Information<*>>()
    private val informationProviders_ = mutableSetOf<InformationProvider>()

    override val informationSet: Set<Information<*>> = Collections.unmodifiableSet(this.informationSet_)
    override val informationProviders: Set<InformationProvider> = Collections.unmodifiableSet(this.informationProviders_)

    override fun <T> registerInformation(id: Information.Id, value: T, valueType: TypeInfo<T>, description: String?): Boolean {
        return this.informationSet_.add(Information(id, value, valueType, description))
    }

    override fun registerInformation(information: Information<*>): Boolean {
        return this.informationSet_.add(information)
    }

    override fun unregisterInformation(id: Information.Id): Boolean {
        return this.informationSet_.removeIf { it.id == id }
    }

    override fun registerInformationProvider(informationProvider: InformationProvider): Boolean {
        return this.informationProviders_.add(informationProvider)
    }

    override fun unregisterInformationProvider(informationProvider: InformationProvider): Boolean {
        return this.informationProviders_.remove(informationProvider)
    }

    override fun <T> find(id: Information.Id, type: TypeInfo<T>): Information<T>? =
            find(id, type, true)

    @Suppress("UNCHECKED_CAST")
    override fun <T> find(id: Information.Id, type: TypeInfo<T>, useProviders: Boolean): Information<T>? =
            informationSet_.find f@ {
                if (it.type == type) {
                    val itId = it.id

                    if (id.id == itId.id
                            && (id.tags.isEmpty() || id.tags.all { itId.tags.contains(it) })) {
                        return@f true
                    }

                }

                false
            } as? Information<T> ?: this.informationProviders_.let {
                if (!useProviders)
                    return@let null

                it.forEach { p ->
                    val get = p.provide(id, type)
                    if (get != null)
                        return@let get
                }

                return@let null
            }

    override fun copy(): InformationManager {
        val newManager = InformationManagerImpl()

        newManager.informationSet_.addAll(this.informationSet_)
        newManager.informationProviders_.addAll(this.informationProviders_)

        return newManager
    }
}

/**
 * Empty information manager.
 */
object InformationManagerVoid : InformationManager {

    override val informationSet: Set<Information<*>>
        get() = emptySet()

    override val informationProviders: Set<InformationProvider>
        get() = emptySet()

    override fun <T> registerInformation(id: Information.Id, value: T, valueType: TypeInfo<T>, description: String?): Boolean {
        return false
    }

    override fun registerInformation(information: Information<*>): Boolean {
        return true
    }

    override fun unregisterInformation(id: Information.Id): Boolean {
        return true
    }

    override fun registerInformationProvider(informationProvider: InformationProvider): Boolean {
        return true
    }

    override fun unregisterInformationProvider(informationProvider: InformationProvider): Boolean {
        return true
    }

    override fun <T> find(id: Information.Id, type: TypeInfo<T>): Information<T>? {
        return null
    }

    override fun <T> find(id: Information.Id, type: TypeInfo<T>, useProviders: Boolean): Information<T>? {
        return null
    }

    override fun copy(): InformationManager = this

}