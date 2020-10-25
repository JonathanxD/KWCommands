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
package com.github.jonathanxd.kwcommands.util

import com.github.jonathanxd.kwcommands.fail.ParseFail
import com.github.jonathanxd.kwcommands.help.CommonHelpInfoHandler
import com.github.jonathanxd.kwcommands.printer.Printers
import com.github.jonathanxd.kwcommands.processor.CommandResult


@JvmOverloads
inline fun <V> ParseFail.thrown(
    cause: Throwable? = null,
    backend: (message: String) -> V = {
        throw ParseFailException(message = it, cause = cause)
    }
): V {
    val handler = CommonHelpInfoHandler()
    val builder = StringBuilder()

    builder.append("Failed to parse!!!\n")

    handler.handleFail(this, Printers.toStringBuilder(builder))

    return backend(builder.toString())
}

@JvmOverloads
inline fun <V> CommandResult.thrown(
    cause: Throwable? = null,
    backend: (message: String) -> V = {
        throw CommandResultException(message = it, cause = cause)
    }
): V {
    val handler = CommonHelpInfoHandler()
    val builder = StringBuilder()

    handler.handleResult(this, Printers.toStringBuilder(builder))

    return backend(builder.toString())
}

@JvmOverloads
inline fun <V> List<CommandResult>.thrown(
    cause: Throwable? = null,
    backend: (message: String) -> V = {
        throw CommandResultException(message = it, cause = cause)
    }
): V {
    val handler = CommonHelpInfoHandler()
    val builder = StringBuilder()

    handler.handleResults(this, Printers.toStringBuilder(builder))

    return backend(builder.toString())
}

class ParseFailException : RuntimeException {
    constructor() : super()
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
    constructor(cause: Throwable?) : super(cause)
}

class CommandResultException : RuntimeException {
    constructor() : super()
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
    constructor(cause: Throwable?) : super(cause)
}

