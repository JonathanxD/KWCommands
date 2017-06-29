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
package com.github.jonathanxd.kwcommands.requirement

import com.github.jonathanxd.kwcommands.information.Information
import com.github.jonathanxd.kwcommands.manager.InformationManager

/**
 * Check if all [Information] matches the [requirements][Requirement].
 *
 * @return Empty list if all requirements was satisfied or a list with unsatisfied requirements.
 */
fun List<Requirement<*, *>>.checkRequirements(manager: InformationManager): List<UnsatisfiedRequirement<*>> {
    val fails = mutableListOf<UnsatisfiedRequirement<*>>()

    this.forEach {
        val find = manager.find(it.subject, it.infoType)

        if (find == null) {
            fails.add(UnsatisfiedRequirement(it, it.subject, null, Reason.MISSING_INFORMATION))
        } else {
            @Suppress("UNCHECKED_CAST")
            it as Requirement<Any, *>
            @Suppress("UNCHECKED_CAST")
            find as Information<Any>

            if (!it.test(find))
                fails.add(UnsatisfiedRequirement(it, it.subject, find, Reason.UNSATISFIED_REQUIREMENT))

        }

    }

    return fails
}

data class UnsatisfiedRequirement<T>(val requirement: Requirement<T, *>,
                                     val informationId: Information.Id,
                                     val information: Information<T>?,
                                     val reason: Reason)

enum class Reason {
    MISSING_INFORMATION,
    UNSATISFIED_REQUIREMENT
}