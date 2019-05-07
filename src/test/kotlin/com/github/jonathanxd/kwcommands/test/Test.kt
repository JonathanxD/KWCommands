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
import com.github.jonathanxd.iutils.text.Text
import com.github.jonathanxd.iutils.kt.asText
import com.github.jonathanxd.kwcommands.argument.Argument
import com.github.jonathanxd.kwcommands.argument.ArgumentHandler
import com.github.jonathanxd.kwcommands.argument.StaticListArguments
import com.github.jonathanxd.kwcommands.command.Command
import com.github.jonathanxd.kwcommands.command.CommandContainer
import com.github.jonathanxd.kwcommands.command.Handler
import com.github.jonathanxd.kwcommands.dsl.argument
import com.github.jonathanxd.kwcommands.fail.ParseFail
import com.github.jonathanxd.kwcommands.help.CommonHelpInfoHandler
import com.github.jonathanxd.kwcommands.information.InformationProviders
import com.github.jonathanxd.kwcommands.parser.SingleInput
import com.github.jonathanxd.kwcommands.printer.Printers
import com.github.jonathanxd.kwcommands.processor.CommandResult
import com.github.jonathanxd.kwcommands.processor.Processors
import com.github.jonathanxd.kwcommands.processor.ResultHandler
import com.github.jonathanxd.kwcommands.processor.ValueResult
import com.github.jonathanxd.kwcommands.util.*
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
                                informationProviders: InformationProviders,
                                resultHandler: ResultHandler): Any {
                val sb = StringBuilder()

                sb.append(commandContainer.command.fullname)

                if (commandContainer.arguments.isNotEmpty()) {
                    sb.append(commandContainer.arguments.filter { it.isDefined }.joinToString(prefix = "(", postfix = ")") { it.value.toString() })
                }

                println(sb.toString())
                return sb.toString()
            }
        }

        val command = Command(
                parent = null,
                name = "open",
                description = Text.of(),
                handler = fnmHandler,
                order = 0,
                alias = emptyList(),
                arguments = StaticListArguments(listOf(
                        argument<SingleInput, String> {
                            name = "name"
                            description = "".asText()
                            isOptional = false
                            isMultiple = false
                            defaultValue = null
                            type = stringArgumentType
                            requirements {}
                            requiredInfo {}
                        }
                )),
                requiredInfo = emptySet(),
                requirements = emptyList())

        command.addSubCommand(Command(
                parent = command,
                name = "door",
                description = Text.of(),
                handler = fnmHandler,
                order = 0,
                alias = emptyList(),
                arguments = StaticListArguments(),
                requiredInfo = emptySet(),
                requirements = emptyList()
        ))

        command.addSubCommand(Command(
                parent = command,
                name = "window",
                description = Text.of(),
                handler = fnmHandler,
                order = 0,
                alias = emptyList(),
                arguments = StaticListArguments(listOf(
                        Argument(name = "name",
                                alias = emptyList(),
                                description = "".asText(),
                                isOptional = false,
                                argumentType = stringArgumentType,
                                requirements = emptyList(),
                                requiredInfo = emptySet()),
                        Argument(name = "amount",
                                alias = emptyList(),
                                description = "".asText(),
                                isOptional = true,
                                argumentType = intArgumentType,
                                requirements = emptyList(),
                                requiredInfo = emptySet(),
                                handler = ArgumentHandler.create { arg, _, _, _ ->
                                    return@create arg.value ?: Unit
                                }),
                        Argument(name = "double",
                                alias = emptyList(),
                                description = "".asText(),
                                isOptional = false,
                                argumentType = doubleArgumentType,
                                requirements = emptyList(),
                                requiredInfo = emptySet())
                )),
                requiredInfo = emptySet(),
                requirements = emptyList()
        ))

        val processor = Processors.createCommonProcessor()

        processor.parser.commandManager.registerCommand(command, this)


        processor.parseAndDispatch("open house", this)
                .assertAll(listOf("open(house)"))

        processor.parseAndDispatch("open door window \"of house\" 5.0", this)
                .assertAll(listOf("open door", "open window(of house, 5.0)"))

        processor.parseAndDispatch("open door window \"of house\" 1 7.0", this)
                .assertAll(listOf("open door", 1, "open window(of house, 1, 7.0)"))

        processor.parseAndDispatch("open door & open window \"of house\" 5 19.0", this)
                .assertAll(listOf("open door", 5, "open window(of house, 5, 19.0)"))
    }

}

fun ValueResult.assert(expected: Any?) {
    Assert.assertEquals(expected, this.value)
}

fun Either<ParseFail, List<CommandResult>>.assertAll(expected: List<Any?>) {
    if (this.isLeft) {
        val h = CommonHelpInfoHandler()
        val printer = Printers.sysOutWHF
        h.handleFail(this.left, printer)
    }

    Assert.assertTrue(this.isRight)
    this.right.assertAll(expected)
}

fun Either<ParseFail, List<CommandResult>>.assertWithAsserter(asserter: (Any?) -> Unit, sizeAsserter: (Int) -> Unit = {}) {
    if (this.isLeft) {
        val h = CommonHelpInfoHandler()
        val printer = Printers.sysOutWHF
        h.handleFail(this.left, printer)
    }

    Assert.assertTrue(this.isRight)
    this.right.assertAll(asserter, sizeAsserter)
}

fun List<CommandResult>.assertAll(expected: List<Any?>) {

    if (this.count { it is ValueResult && it.value != Unit } != expected.size)
        throw ComparisonFailure("List is not equal.", expected.toString(), this.map { it.toString() }.toString())

    this.filter { it !is ValueResult || it.value != Unit }.forEachIndexed { index, result ->
        (result as? ValueResult)?.assert(expected[index])
    }

}

fun List<CommandResult>.assertAll(asserter: (Any?) -> Unit, sizeAsserter: (Int) -> Unit = {}) {

    sizeAsserter(this.count { it is ValueResult && it.value != Unit })

    this.filter { it !is ValueResult || it.value != Unit }.forEachIndexed { index, result ->
        asserter((result as? ValueResult)?.value)
    }

}