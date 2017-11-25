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
package com.github.jonathanxd.kwcommands.completion

import com.github.jonathanxd.kwcommands.command.CommandContainer
import com.github.jonathanxd.kwcommands.fail.ArgumentNotFoundFail
import com.github.jonathanxd.kwcommands.fail.CommandInputParseFail
import com.github.jonathanxd.kwcommands.fail.InvalidInputForArgumentFail
import com.github.jonathanxd.kwcommands.fail.ParseFail
import com.github.jonathanxd.kwcommands.parser.*
import com.github.jonathanxd.kwcommands.util.*
import java.util.*
import kotlin.collections.ArrayList

class CompletionImpl(override val parser: CommandParser) : Completion {

    private val autoCompleterList_ = mutableListOf<AutoCompleter>()

    override val autoCompleterList: List<AutoCompleter> =
            Collections.unmodifiableList(this.autoCompleterList_)

    private val autoCompleters = AutoCompleters(this.autoCompleterList)

    init {
        registerAutoCompleter(DefaultAutoCompleter())
    }

    override fun registerAutoCompleter(completer: AutoCompleter) {
        this.autoCompleterList_.add(completer)
    }

    override fun unregisterAutoCompleter(completer: AutoCompleter) {
        this.autoCompleterList_.remove(completer)
    }

    override fun completeWithOwnerFunc(input: String, ownerProvider: OwnerProvider): List<String> {
        val suggestions = mutableListOf<String>()

        val parse = parser.parseWithOwnerFunction(input, ownerProvider)

        if (parse.isRight) {
            complete(parse.right, suggestions)
        } else {
            complete(parse.left, suggestions)
        }

        return suggestions
    }

    private fun complete(commandContainers: List<CommandContainer>, suggestion: MutableList<String>) {

    }

    private fun complete(parseFail: ParseFail, suggestion: MutableList<String>) {
        val parsedCommands = parseFail.parsedCommands
        val manager = parseFail.manager
        val iter = parseFail.iter

        val completions = ListCompletionsImpl()
        val completionList = completions.list

        when (parseFail) {
            is ArgumentNotFoundFail -> {
                val command = parseFail.command
                val parsedArgs = parseFail.parsedArgs
                val input: String = parseFail.input

                this.autoCompleters.completeArgumentName(command, parsedArgs, completions)

                suggestion += completionList.filter { it.startsWith(input) }
            }
            is InvalidInputForArgumentFail -> {
                val command = parseFail.command
                val argument = parseFail.arg
                val parsedArgs = parseFail.parsedArgs
                val input = parseFail.input
                val validation = parseFail.validation

                validation.invalids.forEach {
                    this.autoCompleters.completeArgumentInput(command, parsedArgs, argument, input, it.input,
                            completions)
                }

                // TODO
            }
            is CommandInputParseFail -> {
                val command = parseFail.command
                val argument = parseFail.argument
                val parsedArgs = parseFail.parsedArgs
                val input = parseFail.inputType
                var tfail = parseFail.fail
                var tmain: InputParseFail? = null

                while (tfail is NestedInputParseFail) {
                    if (tmain == null)
                        tmain = tfail.fail

                    if (tfail.fail2 is NoMoreElementsInputParseFail) {
                        tfail = tfail.fail
                        break
                    }
                    tfail = tfail.fail2
                }

                val main = tmain ?: tfail
                val fail = tfail
                val mainInput =
                        (main as? MapInputParseFail)?.map
                                ?: (fail as? ListInputParseFail)?.list

                when (fail) {
                    is MapValueNotFound -> {
                        this.autoCompleters.completeArgumentMap(command, parsedArgs, argument,
                                mainInput ?: fail.map,
                                fail.map,
                                fail.key,
                                completions)

                        suggestion += completionList
                    }
                    is ListTokenExpectedFail, is MapTokenExpectedFail -> {
                        val found = (fail as? ListTokenExpectedFail)?.foundToken
                                ?: (fail as MapTokenExpectedFail).foundToken

                        val tokens = (fail as? ListTokenExpectedFail)?.tokens
                                    ?: (fail as MapTokenExpectedFail).tokens

                        val failInput = (fail as? ListTokenExpectedFail)?.list
                                ?: (fail as MapTokenExpectedFail).map

                        if (tokens.contains(',')) {
                            if (fail is ListTokenExpectedFail) {
                                this.autoCompleters.completeArgumentList(command, parsedArgs, argument,
                                        mainInput ?: fail.list,
                                        fail.list,
                                        completions)
                            } else if (fail is MapTokenExpectedFail) {
                                this.autoCompleters.completeArgumentMap(command, parsedArgs, argument,
                                        mainInput ?: fail.map,
                                        fail.map,
                                        null,
                                        completions)
                            }
                        }

                        completions.addAll(tokens.map { it.toString() })

                        suggestion += completionList
                    }
                }
            }
        }
    }

    private class ListCompletionsImpl : Completions {
        val list = mutableListOf<String>()

        override fun add(completion: String) {
            list.add(completion)
        }

        override fun addAll(completions: Iterable<String>) {
            list.addAll(completions) // For cases where bulk add operation is optimized (ex: ArrayList)
        }

    }
}