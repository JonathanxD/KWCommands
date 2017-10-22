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

import com.github.jonathanxd.kwcommands.dsl.*
import com.github.jonathanxd.kwcommands.exception.CommandException
import com.github.jonathanxd.kwcommands.help.CommonHelpInfoHandler
import com.github.jonathanxd.kwcommands.information.Information
import com.github.jonathanxd.kwcommands.manager.CommandManagerImpl
import com.github.jonathanxd.kwcommands.manager.InformationManagerImpl
import com.github.jonathanxd.kwcommands.printer.CommonPrinter
import com.github.jonathanxd.kwcommands.processor.Processors
import org.junit.Test

class DslTest {
    @Test
    fun dslTest() {
        val printer = CommonPrinter(::println)
        val handler = CommonHelpInfoHandler()

        val helloCommand = command {
            name { string("hello") }
            arguments {
                +stringArg { id { "name" } }
            }
            handlerWithContext {
                println("Hello ${it.getArg<String>("name")}")
            }
        }

        val runCommand = command {
            name { string("promote") }
            requiredInfo {
                requireInfo<Group> {
                    id(informationId<Group> { tags { +"group" } })
                }
            }
            requirements {
                +requirement<Group, Group>(Group.ADMIN) {
                    subject(informationId { tags { +"group" } })
                    tester { (required1), (_, value) -> value == required1 }
                }
            }
            arguments {
                +stringArg {
                    id { "userId" }
                    transformer { it.toLowerCase() }
                }
                +enumArg<Group> {
                    id {"group"}
                }
            }
        }

        val infoManager = InformationManagerImpl()
        val manager = CommandManagerImpl()
        val processor = Processors.createCommonProcessor(manager)

        manager.registerCommand(helloCommand, this)
        manager.registerCommand(runCommand, this)

        try {
            processor.processAndHandle(listOf("hello", "KWCommands", "x"), this, infoManager)
        } catch (ex: CommandException) {
            handler.handleCommandException(ex, printer)
        }
    }

    enum class Group {
        ADMIN,
        USER
    }
}