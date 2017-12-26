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
package com.github.jonathanxd.kwcommands.test

import com.github.jonathanxd.jwiutils.kt.ifLeftSide
import com.github.jonathanxd.jwiutils.kt.textOf
import com.github.jonathanxd.jwiutils.kt.typeInfo
import com.github.jonathanxd.kwcommands.argument.CustomArgumentType
import com.github.jonathanxd.kwcommands.dsl.*
import com.github.jonathanxd.kwcommands.help.CommonHelpInfoHandler
import com.github.jonathanxd.kwcommands.manager.CommandManagerImpl
import com.github.jonathanxd.kwcommands.manager.InformationProvidersImpl
import com.github.jonathanxd.kwcommands.printer.CommonPrinter
import com.github.jonathanxd.kwcommands.processor.Processors
import com.github.jonathanxd.kwcommands.util.KLocale
import com.github.jonathanxd.kwcommands.util.stringArgumentType
import org.junit.Assert
import org.junit.Test

class MapTest2 {
    @Test
    fun test() {
        val printer = CommonPrinter(KLocale.localizer, ::println)
        val handler = CommonHelpInfoHandler()

        val helloCommand = command {
            name { "hello" }
            arguments {
                staticListArguments {
                    +stringArg { name { "name" } }
                }
            }
            handlerWithContext {
                println("Hello ${it.getArg<String>("name")}")
            }
        }

        val runCommand = command {
            name { "promote" }
            requiredInfo {
                requireInfo<DslTest.Group> {
                    id(informationId<DslTest.Group> { tags { +"group" } })
                }
            }
            requirements {
                +requirement<DslTest.Group, DslTest.Group>(DslTest.Group.ADMIN) {
                    subject(informationId { tags { +"group" } })
                    tester(textOf("GroupTester")) { (required1), (_, value) ->
                        value == required1
                    }
                }
            }
            arguments {
                staticListArguments {
                    +stringArg {
                        name { "userId" }
                        description { textOf("Identification of user to promote") }
                        type = CustomArgumentType({ it.toLowerCase() }, null, stringArgumentType, typeInfo())
                    }
                    +listArg(enumArg<DslTest.Group> {
                        name { "groupList" }
                        description { textOf("Groups to add user to") }
                    })
                }
            }

            handlerWithContext {
                println("Promoted ${it.getArg<String>("userId")} to ${it.getArg<List<DslTest.Group>>("groupList")}")
            }
        }

        val infoManager = InformationProvidersImpl()
        infoManager.registerInformation(informationId<DslTest.Group> { tags { +"group" } }, DslTest.Group.ADMIN, "")
        val manager = CommandManagerImpl()
        val processor = Processors.createCommonProcessor(manager)

        manager.registerCommand(helloCommand, this)
        manager.registerCommand(runCommand, this)

        processor.parseAndDispatch("hello KWCommands", this, infoManager).let {
            if (it.isRight) {
                handler.handleResults(it.right, printer)
                Assert.assertTrue(it.right.isEmpty())
            }
            it
        }.ifLeftSide {
            handler.handleFail(it, printer)
            throw AssertionError()
        }

        processor.parseAndDispatch("promote KWCommands ADMIN USER", this, infoManager).let {
            if (it.isRight) {
                handler.handleResults(it.right, printer)
                Assert.assertTrue(it.right.isEmpty())
            }
            it
        }.ifLeftSide {
            handler.handleFail(it, printer)
            throw AssertionError()
        }

        processor.parseAndDispatch("promote KWCommands x", this, infoManager).let {
            if (it.isRight) {
                handler.handleResults(it.right, printer)
                throw AssertionError()
            }
            it
        }.ifLeftSide {
            handler.handleFail(it, printer)
        }
    }
}