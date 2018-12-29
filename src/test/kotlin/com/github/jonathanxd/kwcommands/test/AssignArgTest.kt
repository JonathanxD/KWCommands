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

import com.github.jonathanxd.iutils.kt.toText
import com.github.jonathanxd.kwcommands.AIO
import com.github.jonathanxd.kwcommands.KWCommands
import com.github.jonathanxd.kwcommands.fail.NoInputForArgumentFail
import com.github.jonathanxd.kwcommands.help.CommonHelpInfoHandler
import com.github.jonathanxd.kwcommands.help.HelpInfoHandler
import com.github.jonathanxd.kwcommands.information.InformationProviders
import com.github.jonathanxd.kwcommands.information.InformationProvidersImpl
import com.github.jonathanxd.kwcommands.printer.CommonPrinter
import com.github.jonathanxd.kwcommands.printer.Printer
import com.github.jonathanxd.kwcommands.reflect.annotation.Arg
import com.github.jonathanxd.kwcommands.reflect.annotation.Cmd
import com.github.jonathanxd.kwcommands.reflect.annotation.Info
import com.github.jonathanxd.kwcommands.util.KLocale
import org.junit.Assert
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AssignArgTest {

    lateinit var aio: AIO
    lateinit var provider: InformationProviders
    lateinit var handler: HelpInfoHandler
    lateinit var printer: Printer

    @Before
    fun setup() {
        aio = KWCommands.createAio(this)
                .loadObj(AssignArg())
                .registerLoaded()
        printer = CommonPrinter(KLocale.localizer, ::println, printHeaderAndFooter = false)
        handler = CommonHelpInfoHandler()

        provider = InformationProvidersImpl().apply {
            registerRecommendations(printer = printer)
        }
    }

    @Test
    fun normalArgTest() {
        aio.parseAndDispatch("get --url http://localhost", provider)
                .commonTest(handler, printer)
    }

    @Test
    fun assignArgTest() {
        aio.parseAndDispatch("get --url=http://localhost", provider)
                .commonTest(handler, printer)
    }

    @Test
    fun missingAssignArgValueTest() {
        aio.parseAndDispatch("get --url=", provider)
                .commonTest(handler, printer,
                        expectFail = {
                            it is NoInputForArgumentFail && it.arg.name == "url"
                        })
    }

    class AssignArg {
        @Cmd(name = "get")
        fun get(@Arg("url") url: String,
                @Info printer: Printer) {
            printer.printPlain("hello world".toText())
            printer.flush()

            assertEquals("Url must be localhost", "http://localhost", url)
        }
    }
}