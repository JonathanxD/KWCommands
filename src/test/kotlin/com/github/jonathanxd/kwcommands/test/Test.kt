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

import com.github.jonathanxd.kwcommands.argument.ArgumentHandler
import com.github.jonathanxd.kwcommands.command.Command
import com.github.jonathanxd.kwcommands.command.CommandContainer
import com.github.jonathanxd.kwcommands.command.CommandName
import com.github.jonathanxd.kwcommands.command.Handler
import com.github.jonathanxd.kwcommands.manager.InformationManager
import com.github.jonathanxd.kwcommands.processor.CommandResult
import com.github.jonathanxd.kwcommands.processor.Processors
import com.github.jonathanxd.kwcommands.processor.ResultHandler
import com.github.jonathanxd.kwcommands.processor.ValueResult
import com.github.jonathanxd.kwcommands.util.Argument
import com.github.jonathanxd.kwcommands.util.Mode
import com.github.jonathanxd.kwcommands.util.toCommandStringList
import org.junit.Assert
import org.junit.ComparisonFailure
import org.junit.Test

class StringTest {

    @Test
    fun test1() {
        val str = "create KWCommands \"Command system\""

        Assert.assertEquals(listOf("create", "KWCommands", "Command system"), str.toCommandStringList())
    }

    @Test
    fun test2() {
        val str = "create KWCommands \"\\\"Command system\\\"\""

        Assert.assertEquals(listOf("create", "KWCommands", "\"Command system\""), str.toCommandStringList())
    }

    @Test
    fun test3() {
        val str = "create KWCommands \\\"Command system\\\""

        Assert.assertEquals(listOf("create", "KWCommands", "\"Command", "system\""), str.toCommandStringList())
    }

    @Test
    fun testMulti1() {
        val str = "create KWCommands \\\"'Command system'\\\""

        Assert.assertEquals(listOf("create", "KWCommands", "\"Command system\""), str.toCommandStringList())
    }

    @Test
    fun testMulti1m() {
        val str = "create KWCommands '\\\"Command system\\\"'"

        Assert.assertEquals(listOf("create", "KWCommands", "\"Command system\""), str.toCommandStringList())
    }

    @Test
    fun testMulti2() {
        val str = "create KWCommands '\"Command system\"'"

        Assert.assertEquals(listOf("create", "KWCommands", "\"Command system\""), str.toCommandStringList())
    }

    @Test
    fun testMulti3() {
        val str = "create KWCommands '\"Command system'\""

        Assert.assertEquals(listOf("create", "KWCommands", "\"Command system\""), str.toCommandStringList())
    }

    @Test
    fun testMulti4() {
        val str = "create KWCommands \"'Command system\"'"

        Assert.assertEquals(listOf("create", "KWCommands", "Command system"), str.toCommandStringList(mode = Mode.MULTI_DELIMITER))
    }
}

class CommandTest {

    @Test
    fun test() {
        val fnmHandler = object : Handler {
            override fun handle(commandContainer: CommandContainer,
                                informationManager: InformationManager,
                                resultHandler: ResultHandler): Any {
                val sb = StringBuilder()

                sb.append(commandContainer.command.fullname)

                if (commandContainer.arguments.isNotEmpty()) {
                    sb.append(commandContainer.arguments.filter { it.isDefined }.map { it.value!!.toString() }.joinToString(prefix = "(", postfix = ")"))
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
                                requirements = emptyList(),
                                requiredInfo = emptySet(),
                                possibilities = emptyList())

                ),
                requiredInfo = emptySet(),
                requirements = emptyList())

        command.addSubCommand(Command(
                parent = command,
                name = CommandName.StringName("door"),
                description = "",
                handler = fnmHandler,
                order = 0,
                alias = emptyList(),
                arguments = emptyList(),
                requiredInfo = emptySet(),
                requirements = emptyList()
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
                                requirements = emptyList(),
                                requiredInfo = emptySet(),
                                possibilities = emptyList()),
                        Argument(id = "amount",
                                isOptional = true,
                                defaultValue = null,
                                validator = { it.toIntOrNull() != null },
                                transformer = String::toInt,
                                possibilities = emptyList(),
                                requirements = emptyList(),
                                requiredInfo = emptySet(),
                                handler = ArgumentHandler.create { arg, _, _, _ ->
                                    return@create arg.value!!
                                }),
                        Argument(id = "double",
                                isOptional = false,
                                defaultValue = null,
                                validator = { it.toDoubleOrNull() != null },
                                transformer = String::toDouble,
                                requirements = emptyList(),
                                requiredInfo = emptySet(),
                                possibilities = emptyList())
                ),
                requiredInfo = emptySet(),
                requirements = emptyList()
        ))

        val processor = Processors.createCommonProcessor()

        processor.commandManager.registerCommand(command, this)


        processor.handle(processor.process(listOf("open", "house"), this))
                .assertAll(listOf("open(house)"))

        processor.handle(processor.process(listOf("open", "door", "window", "of house", "5.0"), this))
                .assertAll(listOf("open door", "open window(of house, 5.0)"))

        processor.handle(processor.process(listOf("open", "door", "window", "of house", "1", "7.0"), this))
                .assertAll(listOf("open door", 1, "open window(of house, 1, 7.0)"))

        processor.handle(processor.process(listOf("open", "door", "&", "open", "window", "of house", "5", "19.0"), this))
                .assertAll(listOf("open door", 5, "open window(of house, 5, 19.0)"))
    }

}

fun ValueResult.assert(expected: Any?) {
    Assert.assertEquals(expected, this.value)
}

fun List<CommandResult>.assertAll(expected: List<Any?>) {

    if (this.count { it is ValueResult } != expected.size)
        throw ComparisonFailure("List is not equal.", expected.toString(), this.map { it.toString() }.toString())

    this.forEachIndexed { index, result ->
        (result as? ValueResult)?.assert(expected[index])
    }

}