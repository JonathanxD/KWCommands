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

import com.github.jonathanxd.kwcommands.command.Command
import com.github.jonathanxd.kwcommands.command.CommandContainer
import com.github.jonathanxd.kwcommands.command.CommandName
import com.github.jonathanxd.kwcommands.command.Handler
import com.github.jonathanxd.kwcommands.manager.InformationManager
import com.github.jonathanxd.kwcommands.manager.RequirementUtil
import com.github.jonathanxd.kwcommands.processor.Processors
import com.github.jonathanxd.kwcommands.processor.Result
import com.github.jonathanxd.kwcommands.util.Argument
import org.junit.Assert
import org.junit.Test

class CommandTest {

    @Test
    fun test() {
        val fnmHandler = object : Handler {
            override fun handle(commandContainer: CommandContainer, informationManager: InformationManager): Any {
                val sb = StringBuilder()

                sb.append(commandContainer.command.fullname)

                if (commandContainer.arguments.isNotEmpty()) {
                    sb.append(commandContainer.arguments.filter {it.isDefined}.map { it.value!!.toString() }.joinToString(prefix = "(", postfix = ")"))
                }

                println(sb.toString())
                return sb.toString()
            }
        }

        val command = Command(
                parent = null,
                name = CommandName.StringName("open"),
                description = "",
                handler = fnmHandler,
                order = 0,
                alias = emptyList(),
                arguments = listOf(
                        Argument(id = "name",
                                isOptional = false,
                                defaultValue = null,
                                validator = { true },
                                transformer = { it },
                                possibilities = emptyList())

                ))

        command.addSubCommand(Command(
                parent = command,
                name = CommandName.StringName("door"),
                description = "",
                handler = fnmHandler,
                order = 0,
                alias = emptyList(),
                arguments = emptyList()
        ))

        command.addSubCommand(Command(
                parent = command,
                name = CommandName.StringName("window"),
                description = "",
                handler = fnmHandler,
                order = 0,
                alias = emptyList(),
                arguments = listOf(
                        Argument(id = "name",
                                isOptional = false,
                                defaultValue = null,
                                validator = { true },
                                transformer = { it },
                                possibilities = emptyList()),
                        Argument(id = "amount",
                                isOptional = true,
                                defaultValue = null,
                                validator = { it.toIntOrNull() != null },
                                transformer = String::toInt,
                                possibilities = emptyList()),
                        Argument(id = "double",
                                isOptional = false,
                                defaultValue = null,
                                validator = { it.toDoubleOrNull() != null },
                                transformer = String::toDouble,
                                possibilities = emptyList())
                )
        ))

        val processor = Processors.createCommonProcessor()

        processor.commandManager.registerCommand(command, this)


        processor.handle(processor.process(listOf("open", "house"), this))
                .assertAll(listOf("open(house)"))

        processor.handle(processor.process(listOf("open", "door", "window", "of house", "5.0"), this))
                .assertAll(listOf("open door", "open window(of house, 5.0)"))

        processor.handle(processor.process(listOf("open", "door", "window", "of house", "1", "7.0"), this))
                .assertAll(listOf("open door", "open window(of house, 1, 7.0)"))

        processor.handle(processor.process(listOf("open", "door", "&", "open", "window", "of house", "5", "19.0"), this))
                .assertAll(listOf("open door", "open window(of house, 5, 19.0)"))
    }

}

fun Result.assert(expected: Any?) {
    Assert.assertEquals(expected, this.value)
}

fun List<Result>.assertAll(expected: List<Any?>) {
    Assert.assertEquals(this.size, expected.size)

    this.forEachIndexed { index, result ->
        result.assert(expected[index])
    }

}