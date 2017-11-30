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
@file:Suppress("MemberVisibilityCanPrivate")

package com.github.jonathanxd.kwcommands.command

import com.github.jonathanxd.iutils.type.TypeInfo
import com.github.jonathanxd.jwiutils.kt.typeInfo
import com.github.jonathanxd.kwcommands.exception.ArgumentMissingException
import com.github.jonathanxd.kwcommands.exception.InfoMissingException
import com.github.jonathanxd.kwcommands.information.Information
import com.github.jonathanxd.kwcommands.manager.InformationManager
import com.github.jonathanxd.kwcommands.processor.ResultHandler
import com.github.jonathanxd.kwcommands.requirement.Requirement

/**
 * Context of a dispatched command.
 *
 * @property commandContainer Dispatched command container.
 * @property informationManager Manager of information.
 * @property resultHandler Handler of command result.
 */
data class CommandContext(val commandContainer: CommandContainer,
                          val informationManager: InformationManager,
                          val resultHandler: ResultHandler) {

    fun <T> getArgById(name: String): T =
            this.getOptArgById<T>(name)
                    ?: throw ArgumentMissingException("Argument with name '$name' is missing!")

    fun <T> getOptArgById(name: String): T? =
            this.commandContainer.getArgument<T>(name)?.value

    fun <T> getArg(name: String, type: TypeInfo<T>): T =
            this.getOptArg(name, type)
                    ?: throw ArgumentMissingException("Argument with name '$name' is missing!")

    fun <T> getOptArg(name: String, type: TypeInfo<T>): T? =
            this.commandContainer.getArgument(name, type)?.value

    fun <T> getInfo(infoId: Information.Id<T>): Information<T> =
            this.getOptInfo(infoId)
                    ?: throw InfoMissingException("Information with id '$infoId' is missing!")

    fun <T> getOptInfo(infoId: Information.Id<T>): Information<T>? =
            this.informationManager.find(infoId)

    fun <T> getInfoErased(infoId: Information.Id<*>): Information<T> =
            this.informationManager.findErased(infoId)
                    ?: throw InfoMissingException("Information with id '$infoId' is missing!")


    fun <T> getOptInfoErased(infoId: Information.Id<*>): Information<T>? =
            this.informationManager.findErased(infoId)

    fun <T> getInfoValue(infoId: Information.Id<T>): T =
            this.getInfo(infoId).value

    fun <T> getInfoErasedValue(infoId: Information.Id<*>): T =
            this.getInfoErased<T>(infoId).value

    fun <T> getOptInfoValue(infoId: Information.Id<T>): T? =
            this.getOptInfo(infoId)?.value

    fun <T> getOptInfoErasedValue(infoId: Information.Id<*>): T? =
            this.getOptInfoErased<T>(infoId)?.value

    @Suppress("UNCHECKED_CAST")
    fun <T> satisfyReq(req: Requirement<T, *>): Boolean =
            getOptInfo(req.subject)?.let { req.test(it) } ?: false

    // inline

    inline fun <reified T> getArg(name: String): T =
            typeInfo<T>().let { type ->
                commandContainer.getArgument(name, type)?.value
                        ?: throw ArgumentMissingException("Argument with name '$name' of type '$type' is missing!")
            }

    inline fun <reified T> getOptArg(name: String): T? =
            commandContainer.getArgument(name, typeInfo<T>())?.value

    inline fun <reified T> getInfo(tags: Array<String>): Information<T> =
            Information.Id(typeInfo<T>(), tags).let { infoId ->
                this.informationManager.find(infoId)
                        ?: throw InfoMissingException("Information with id '$infoId' is missing!")
            }

    inline fun <reified T> getOptInfo(tags: Array<String>): Information<T>? =
            this.informationManager.find(Information.Id(typeInfo(), tags))

    inline fun <reified T> getInfoValue(tags: Array<String>): T =
            this.getInfo<T>(tags).value

    inline fun <reified T> getOptInfoValue(tags: Array<String>): T? =
            this.getOptInfo<T>(tags)?.value

    inline fun <reified T> inlineSatisfyReq(req: Requirement<T, *>): Boolean =
            getOptInfo<T>(req.subject)?.let { req.test(it) } ?: false
}