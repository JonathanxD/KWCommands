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
import com.github.jonathanxd.iutils.type.TypeInfoUtil
import kotlin.reflect.KClass

/**
 * Specified identification of a [information][Info] request.
 *
 * Either [value] or [typeInfo] must be provided, otherwise Reflection Environment will throw an exception.
 *
 * @property value Type of the information
 * @property typeInfo Used in place of [value] to provide a [TypeInfo] literal to be used instead
 * of Java type, this is recommended to be used to specify a type with generic information.
 * @property tags Tags to be used to identify the requested information.
 */
annotation class Id(
    val value: KClass<*> = Default::class,
    val typeLiter: String = "",
    vararg val tags: String = []
)

val Id.isDefault: Boolean
    get() = this.value.java == Default::class.java && this.typeLiter.isEmpty() && this.tags.isEmpty()

fun Id.idTypeInfo(inferred: TypeInfo<*>): TypeInfo<*> =
    this.typeInfoOrNull ?: inferred

val Id.typeInfoOrNull: TypeInfo<*>?
    get() =
        if (this.value.java != Default::class.java)
            TypeInfo.of(this.value.java)
        else if (this.value.java == Default::class.java && this.typeLiter.isNotEmpty())
            TypeInfoUtil.fromFullString(this.typeLiter).single()
        else null

val Id.typeInfo: TypeInfo<*>
    get () =
        this.typeInfoOrNull
                ?: throw IllegalStateException("Neither type literal nor value class was defined in annotation $this.")


