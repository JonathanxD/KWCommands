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
package com.github.jonathanxd.kwcommands.reflect.annotation

import com.github.jonathanxd.iutils.`object`.Default
import com.github.jonathanxd.iutils.type.TypeInfo
import com.github.jonathanxd.iutils.type.TypeInfoUtil
import com.github.jonathanxd.kwcommands.information.Information
import com.github.jonathanxd.kwcommands.reflect.element.infoComponent

/**
 * Information request.
 *
 * The annotated element should be of either types:
 *
 * - [Information] with a reified type of value type
 * - [Information] [value][Information.value] type.
 *
 * If the annotated element type is [value][Information.value] type (and not [Information]) it should be nullable if
 * [Info] is [optional][isOptional].
 *
 * @property value Id of information.
 * @property isOptional If true, a [Information.EMPTY] will be passed if
 * a information with provided [id][value] cannot be found.
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
    if (it.value.java == Default::class.java && it.typeLiter.isEmpty() && it.tags.isEmpty())
        Information.Id(inferredType.infoComponent, emptyArray())
    else
        Information.Id(it.typeInfo, it.tags)
}

