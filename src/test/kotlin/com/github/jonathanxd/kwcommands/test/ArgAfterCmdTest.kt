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
package com.github.jonathanxd.kwcommands.test

import com.github.jonathanxd.iutils.kt.ifLeftSide
import com.github.jonathanxd.iutils.kt.textOf
import com.github.jonathanxd.kwcommands.dsl.*
import com.github.jonathanxd.kwcommands.help.CommonHelpInfoHandler
import com.github.jonathanxd.kwcommands.information.InformationProvidersImpl
import com.github.jonathanxd.kwcommands.manager.CommandManagerImpl
import com.github.jonathanxd.kwcommands.printer.CommonPrinter
import com.github.jonathanxd.kwcommands.processor.Processors
import com.github.jonathanxd.kwcommands.util.KLocale
import com.github.jonathanxd.kwcommands.util.stringArgumentType
import org.junit.Assert
import org.junit.Test

class ArgAfterCmdTest {

    @Test
    fun testSimpleCommandWithMapArgument() {
        val printer = CommonPrinter(KLocale.localizer, ::println)
        val handler = CommonHelpInfoHandler()

        val cmd = command {
            name { "example" }
            description { textOf("Test command") }
            arguments {
                staticListArguments {
                    +mapArg(stringArgumentType() to stringArgumentType()) {
                        name { "kv" }
                        description { textOf("Map argument") }
                    }

                }
            }
            handlerWithContext {
                val value: Map<String, String> = it.getArg("kv")

                println("Value = $value")
            }
        }

        val infoManager = InformationProvidersImpl()
        val manager = CommandManagerImpl()
        val processor = Processors.createCommonProcessor(manager)

        manager.registerCommand(cmd, this)

        processor.parseAndDispatch("example {a=a}", this, infoManager).ifLeftSide {
            handler.handleFail(it, printer)
            Assert.fail("Command must not fail")
        }
    }

}