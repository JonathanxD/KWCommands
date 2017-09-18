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
package com.github.jonathanxd.kwcommands.command

import com.github.jonathanxd.iutils.type.AbstractTypeInfo
import com.github.jonathanxd.iutils.type.TypeInfo
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

    fun <T> getArgById(id: Any): T =
            this.getOptArgById<T>(id)
                    ?: throw ArgumentMissingException("Argument with id $id is missing!")

    fun <T> getOptArgById(id: Any): T? =
            this.commandContainer.getArgument<T>(id)?.value

    fun <T> getArg(id: Any, type: TypeInfo<T>): T =
            this.getOptArg(id, type)
                    ?: throw ArgumentMissingException("Argument with id $id is missing!")

    fun <T> getOptArg(id: Any, type: TypeInfo<T>): T? =
            this.commandContainer.getArgument(id, type)?.value

    fun <T> getInfo(infoId: Information.Id, typeInfo: TypeInfo<T>): Information<T> =
            this.getOptInfo(infoId, typeInfo)
                    ?: throw InfoMissingException("Information with id $infoId of type $typeInfo is missing!")


    fun <T> getOptInfo(infoId: Information.Id, typeInfo: TypeInfo<T>): Information<T>? =
            this.informationManager.find(infoId, typeInfo)

    fun <T> getInfoById(infoId: Information.Id): Information<T> =
            this.informationManager.findById(infoId)
                    ?: throw InfoMissingException("Information with id $infoId is missing!")


    fun <T> getOptInfoById(infoId: Information.Id): Information<T>? =
            this.informationManager.findById(infoId)

    fun <T> getInfoValue(infoId: Information.Id, typeInfo: TypeInfo<T>): T =
            this.getInfo(infoId, typeInfo).value

    fun <T> getInfoValueById(infoId: Information.Id): T =
            this.getInfoById<T>(infoId).value

    fun <T> getOptInfoValue(infoId: Information.Id, typeInfo: TypeInfo<T>): T? =
            this.getOptInfo(infoId, typeInfo)?.value

    fun <T> getOptInfoValueById(infoId: Information.Id): T? =
            this.getOptInfoById<T>(infoId)?.value

    @Suppress("UNCHECKED_CAST")
    fun <T> satisfyReq(req: Requirement<T, *>): Boolean =
            getOptInfo(req.subject, req.infoType as TypeInfo<T>)?.let { req.test(it) } ?: false

    // inline

    inline fun <reified T> getArg(id: Any): T =
            (object : AbstractTypeInfo<T>() {}).let { type ->
                commandContainer.getArgument(id, type)?.value
                        ?: throw ArgumentMissingException("Argument with id $id of type $type is missing!")
            }

    inline fun <reified T> getOptArg(id: Any): T? =
            commandContainer.getArgument(id, object : AbstractTypeInfo<T>() {})?.value

    inline fun <reified T> getInfo(infoId: Information.Id): Information<T> =
            (object : AbstractTypeInfo<T>() {}).let { type ->
                this.informationManager.find(infoId, type)
                        ?: throw InfoMissingException("Information with id $infoId of type $type is missing!")
            }

    inline fun <reified T> getOptInfo(infoId: Information.Id): Information<T>? =
            this.informationManager.find(infoId, object : AbstractTypeInfo<T>() {})

    inline fun <reified T> getInfoValue(infoId: Information.Id): T =
            (object : AbstractTypeInfo<T>() {}).let { type ->
                this.informationManager.find(infoId, type)?.value
                        ?: throw InfoMissingException("Information with id $infoId of type $type is missing!")
            }

    inline fun <reified T> getOptInfoValue(infoId: Information.Id): T? =
            this.informationManager.find(infoId, object : AbstractTypeInfo<T>() {})?.value

    inline fun <reified T> inlineSatisfyReq(req: Requirement<T, *>): Boolean =
            getOptInfo<T>(req.subject)?.let { req.test(it) } ?: false
}