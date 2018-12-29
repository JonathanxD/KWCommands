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
package com.github.jonathanxd.kwcommands.test

import com.github.jonathanxd.iutils.`object`.Either
import com.github.jonathanxd.kwcommands.fail.ParseFail
import com.github.jonathanxd.kwcommands.help.HelpInfoHandler
import com.github.jonathanxd.kwcommands.printer.Printer
import com.github.jonathanxd.kwcommands.processor.CommandResult
import com.github.jonathanxd.kwcommands.processor.ValueResult
import org.junit.Assert

typealias ExecutionResult = Either<ParseFail, List<CommandResult>>

fun ExecutionResult.commonTest(handler: HelpInfoHandler, printer: Printer,
                               expectFail: (ParseFail) -> Boolean = { false }) {
    this.test {
        success {
            handler.handleResults(this, printer)
            Assert.assertTrue(this.all { it is ValueResult && it.value == Unit })
        }
        fail {
            handler.handleFail(this, printer)
            if (!expectFail(this)) {
                throw AssertionError()
            }
        }
    }
}

fun nextTest() {
    println()
}

fun ExecutionResult.test(f: ResultTest.() -> Unit) {
    return f(ResultTest(this))
}

class ResultTest(private val result: ExecutionResult) {
    fun success(f: List<CommandResult>.() -> Unit) {
        if (result.isRight) {
            return f(result.right)
        }
    }

    fun fail(f: ParseFail.() -> Unit) {
        if (result.isLeft) {
            return f(result.left)
        }
    }
}