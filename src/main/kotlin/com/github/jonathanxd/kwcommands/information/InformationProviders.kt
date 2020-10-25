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

import com.github.jonathanxd.iutils.kt.typeInfo
import com.github.jonathanxd.iutils.type.TypeInfo
import com.github.jonathanxd.kwcommands.dispatch.CommandDispatcher
import com.github.jonathanxd.kwcommands.manager.CommandManager
import com.github.jonathanxd.kwcommands.parser.CommandParser
import com.github.jonathanxd.kwcommands.printer.Printer

@JvmField
val COMMAND_MANAGER_ID = Information.Id(typeInfo<CommandManager>(), arrayOf("command_manager"))
@JvmField
val INFORMATION_PROVIDERS_ID =
    Information.Id(typeInfo<InformationProviders>(), arrayOf("information_providers"))
@JvmField
val COMMAND_PARSER_ID = Information.Id(typeInfo<CommandParser>(), arrayOf("command_parser"))
@JvmField
val COMMAND_DISPATCHER_ID =
    Information.Id(typeInfo<CommandDispatcher>(), arrayOf("command_dispatcher"))
@JvmField
val PRINTER_ID = Information.Id(typeInfo<Printer>(), arrayOf("printer"))

/**
 * Register and provide information.
 */
interface InformationProviders {

    /**
     * Static information set
     */
    val informationSet: Set<Information<*>>

    /**
     * Information providers.
     */
    val informationProviders: Set<InformationProvider>

    /**
     * Register recommended information.
     */
    fun registerRecommendations(
        manager: CommandManager? = null,
        parser: CommandParser? = null,
        dispatcher: CommandDispatcher? = null,
        printer: Printer? = null
    ) {
        manager?.let {
            this.registerInformation(COMMAND_MANAGER_ID, it, "Recommended manager")
        }

        parser?.let {
            this.registerInformation(COMMAND_PARSER_ID, it, "Recommended parser")
        }

        dispatcher?.let {
            this.registerInformation(COMMAND_DISPATCHER_ID, it, "Recommended dispatcher")
        }

        printer?.let {
            this.registerInformation(PRINTER_ID, it, "Recommended printer")
        }
    }

    /**
     * Register a [static information][Information] with [id] and [description] with [value].
     */
    fun <T> registerInformation(
        id: Information.Id<T>,
        value: T,
        description: String? = null
    ): Boolean

    /**
     * Register a [static information][information].
     */
    fun registerInformation(information: Information<*>): Boolean

    /**
     * Unregister static information with id [id].
     */
    fun unregisterInformation(id: Information.Id<*>): Boolean

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
     * Find a information by [type] and [tags], this method will first lookup for `static information`, if no
     * one information is found for specified [type] and [tags pair], it will return first non-null
     * information provided by a registered [InformationProvider].
     *
     * If no one information can be found using [type]-[tags] combination,
     * then the implementation should lookup by [tags] only and then [type] only, if no one information
     * is found or more than one information matches the predicate, `null` is returned.
     *
     * @return Found information or `null` if information cannot be found.
     */
    fun <T> find(type: TypeInfo<out T>, tags: Array<out String>): Information<T>? =
        this.find(type, tags, useProviders = true)

    /**
     * Find a information by [type] and [tags], this method will first lookup for `static information`, if no
     * one information is found for specified [type] and [tags pair], it will return first non-null
     * information provided by a registered [InformationProvider].
     *
     * If no one information can be found using [type]-[tags] combination,
     * then the implementation should lookup by [tags] only and then [type] only, if no one information
     * is found or more than one information matches the predicate, `null` is returned.
     *
     * @return Found information or `null` if information cannot be found.
     */
    fun <T> find(
        type: TypeInfo<out T>,
        tags: Array<out String>,
        useProviders: Boolean = true
    ): Information<T>?

    /**
     * Same as [find], but with erased [type].
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> findErased(type: TypeInfo<*>, tags: Array<out String>): Information<T>? =
        this.find(type as TypeInfo<out T>, tags, useProviders = true)

    /**
     * Same as [find], but with erased [type].
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> findErased(
        type: TypeInfo<*>,
        tags: Array<out String>,
        useProviders: Boolean = true
    ): Information<T>? =
        this.find(type as TypeInfo<out T>, tags, useProviders)

    /**
     * Same as [find], but returns an [Information.EMPTY] instead of a `null` reference if [Information] cannot be found.
     *
     * Make sure to check if returned information [Information.isNotEmpty], getting the value before checking it may
     * lead to cast exception.
     */
    fun <T> findOrEmpty(type: TypeInfo<out T>, args: Array<out String>): Information<T> =
        this.find(type, args, useProviders = true) ?: Information.empty()

    /**
     * Same as [findOrEmpty] but with erased [type]
     */
    fun <T> findErasedOrEmpty(type: TypeInfo<*>, args: Array<out String>): Information<T> =
        this.findErased(type, args) ?: Information.empty()

    /**
     * Same as [find], but returns an [Information.EMPTY] instead of a `null` reference if [Information] cannot be found.
     *
     * Make sure to check if returned information [Information.isNotEmpty], getting the value before checking it may
     * lead to cast exception.
     */
    fun <T> findOrEmpty(
        type: TypeInfo<out T>,
        args: Array<out String>,
        useProviders: Boolean = true
    ): Information<T> =
        this.find(type, args, useProviders) ?: Information.empty()

    /**
     * Same as [findOrEmpty] but with erased [type]
     */
    fun <T> findErasedOrEmpty(
        type: TypeInfo<*>,
        args: Array<out String>,
        useProviders: Boolean = true
    ): Information<T> =
        this.findErased(type, args, useProviders) ?: Information.empty()

    /**
     * Creates a safe copy of this manager, modifications on the copy does not affect this instance.
     */
    fun copy(): InformationProviders

    // With id variants

    /**
     * Find a information by [id], this method will first lookup for `static information`, if no
     * one information is found for specified [id], it will return first non-null information provided
     * by a registered [InformationProvider].
     *
     * If no one information can be found using [id][Information.Id.id]-[tags][Information.Id.tags] combination,
     * then the implementation should lookup by [id][Information.Id.id] only, if one information has the same
     * [id][Information.Id.id] as [id], then this information should be returned, if more than one information
     * has the same id, then `null` should be returned.
     *
     * @return Found information or `null` if information cannot be found.
     */
    fun <T> find(id: Information.Id<T>, useProviders: Boolean = true): Information<T>? =
        this.find(id.type, id.tags, useProviders)


    /**
     * Find a information by [id], this method will first lookup for `static information`, if no
     * one information is found for specified [id], it will return first non-null information provided
     * by a registered [InformationProvider].
     *
     * If no one information can be found using [type][Information.Id.type]-[tags][Information.Id.tags] combination,
     * then the implementation should lookup by [type][Information.Id.type] only, if one information has the same
     * [type][Information.Id.type] as [id], then this information should be returned, if more than one information
     * has the same type, the implementation will lookup for an assignable information, if no one information is found,
     * then `null` should be returned.
     *
     * @return Found information or `null` if information cannot be found.
     */
    fun <T> find(id: Information.Id<T>): Information<T>? =
        this.find(id.type, id.tags, true)

    /**
     * Same as [find], but with erased [Information.Id] type.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> findErased(id: Information.Id<*>): Information<T>? =
        this.find(id.type as TypeInfo<out T>, id.tags, true)

    /**
     * Same as [find], but with erased [Information.Id] type.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> findErased(id: Information.Id<*>, useProviders: Boolean = true): Information<T>? =
        this.find(id.type as TypeInfo<out T>, id.tags, useProviders)


    /**
     * Same as [find], but returns an [Information.EMPTY] instead of a `null` reference if [Information] cannot be found.
     *
     * Make sure to check if returned information [Information.isNotEmpty], getting the value before checking it may
     * lead to cast exception.
     */
    fun <T> findOrEmpty(id: Information.Id<T>): Information<T> =
        this.find(id.type, id.tags) ?: Information.empty()

    /**
     * Same as [find], but returns an [Information.EMPTY] instead of a `null` reference if [Information] cannot be found.
     *
     * Make sure to check if returned information [Information.isNotEmpty], getting the value before checking it may
     * lead to cast exception.
     */
    fun <T> findOrEmpty(id: Information.Id<T>, useProviders: Boolean = true): Information<T> =
        this.find(id.type, id.tags, useProviders) ?: Information.empty()

    /**
     * Same as [findOrEmpty] but with erased [Information.Id]
     */
    fun <T> findErasedOrEmpty(id: Information.Id<*>): Information<T> =
        this.findErased(id.type, id.tags) ?: Information.empty()


    /**
     * Same as [findOrEmpty] but with erased [Information.Id]
     */
    fun <T> findErasedOrEmpty(id: Information.Id<*>, useProviders: Boolean = true): Information<T> =
        this.findErased(id.type, id.tags, useProviders) ?: Information.empty()


}