/*
 *      KWCommands - New generation of WCommands written in Kotlin <https://github.com/JonathanxD/KWCommands>
 *
 *         The MIT License (MIT)
 *
 *      Copyright (c) 2018 JonathanxD
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
package com.github.jonathanxd.kwcommands.reflect.annotation

import com.github.jonathanxd.iutils.`object`.Default
import com.github.jonathanxd.iutils.type.TypeInfo
import com.github.jonathanxd.kwcommands.information.Information
import com.github.jonathanxd.kwcommands.reflect.element.infoComponent

/**
 * Used to request an information.
 *
 * The annotated element should be of either types:
 *
 * - [Information] with a reified type of value type
 * - [Information] [value][Information.value] type.
 *
 * If the annotated element type is of the [value][Information.value] type (and not [Information]) and
 * [isOptional] is set to `true`, then type of annotated element should be nullable
 * (Language nullable (such as Kotlin nullable) or with `Nullable` annotation), this is not a rule, but is a
 * recommendation. When annotated element type is [Information], it will never be null, if the information is not
 * present, then an [Information.EMPTY] will be provided.
 *
 * @property value Identification of requested information.
 * @property isOptional Whether information is optional or not.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Info(val value: Id = Id(Default::class), val isOptional: Boolean = false)

/**
 * Creates [Information.Id] from [Info].
 *
 * @param inferredType Inferred type to be used if [Info] does not provide one.
 */
@Suppress("UNCHECKED_CAST")
fun Info.createId(inferredType: TypeInfo<*>): Information.Id<*> = this.value.let {
    if (it.isDefault)
        Information.Id(inferredType.infoComponent, emptyArray())
    else
        Information.Id(it.idTypeInfo(inferredType.infoComponent), it.tags)
}

