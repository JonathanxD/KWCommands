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

import com.github.jonathanxd.kwcommands.argument.Argument
import com.github.jonathanxd.kwcommands.argument.ArgumentContainer
import com.github.jonathanxd.kwcommands.command.Command
import com.github.jonathanxd.kwcommands.command.CommandContainer
import com.github.jonathanxd.kwcommands.fail.*
import com.github.jonathanxd.kwcommands.manager.InformationProviders
import com.github.jonathanxd.kwcommands.parser.*
import com.github.jonathanxd.kwcommands.util.*
import java.util.*

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

    override fun completeWithOwnerFunc(input: String,
                                       ownerProvider: OwnerProvider,
                                       informationProviders: InformationProviders): List<String> {
        val suggestions = mutableListOf<String>()
        val iter = IndexedSourcedCharIter(input)

        val parse = parser.parseWithOwnerFunction(iter, ownerProvider)

        if (parse.isRight) {
            completeSuccess(parse.right, iter, suggestions, informationProviders)
        } else {
            complete(parse.left, suggestions, informationProviders)
        }

        return suggestions
    }

    private fun completeSuccess(commandContainers: List<CommandContainer>,
                                iter: SourcedCharIterator,
                                suggestion: MutableList<String>,
                                informationProviders: InformationProviders) {
        val completions = ListCompletionsImpl()
        val last = commandContainers.lastOrNull()

        this.autoCompleters.completeCommand(last?.command,
                commandContainers,
                completions,
                this.parser.commandManager,
                informationProviders)

        if (last != null) {
            suggestArguments(last.command,
                    last.arguments.filter { it.isDefined },
                    iter,
                    completions,
                    informationProviders)
        }

        if (completions.list.isNotEmpty()) {
            if (iter.suggestBlankSpace()) {
                suggestion += " "
            } else {
                suggestion += completions.list
            }
        }
    }

    private fun SourcedCharIterator.suggestBlankSpace(): Boolean {
        val prev =
                if (!this.hasPrevious()) true
                else this.runAndRestore {
                    val prev = this.previous()
                    prev != ' '
                }

        return prev
    }

    private fun SourcedCharIterator.isValid(): Boolean =
            this.runAndRestore {
                if (this.hasPrevious() && this.previous() == ' ')
                    this.hasPrevious() && this.previous() != ' '
                else
                    true
            }

    private fun suggestArguments(command: Command,
                                 parsedArgs: List<ArgumentContainer<*>>,
                                 iter: SourcedCharIterator,
                                 completions: ListCompletionsImpl,
                                 informationProviders: InformationProviders) {
        val completions2 = ListCompletionsImpl()
        this.autoCompleters.completeArgumentName(command, parsedArgs, completions2, informationProviders)

        completions2.retainIfAnyMatch { parsedArgs.none { arg -> arg.argument.name == it } }
        completions2.map { "--$it" }

        completions.merge(completions2)
        val next: Argument<*>? = command.arguments.getArguments(parsedArgs).let {
            if (it.isNotEmpty())
                it.first()
            else null
        }

        if (next != null) {
            when (next.argumentType.inputType) {
                is SingleInputType -> {
                    this.autoCompleters.completeArgumentInput(command,
                            parsedArgs,
                            next,
                            next.argumentType,
                            EmptyInput(iter.sourceString),
                            completions,
                            informationProviders)
                }
                is MapInputType -> {
                    completions.add("{")
                }
                is ListInputType -> {
                    completions.add("[")
                }
            }

        }
    }

    private fun complete(parseFail: ParseFail,
                         suggestion: MutableList<String>,
                         informationProviders: InformationProviders) {
        val iter = parseFail.iter

        if (!iter.isValid())
            return

        val completions = ListCompletionsImpl()
        val completionList = completions.list

        if (parseFail.iter.hasNext()) {
            this.autoCompleters.handleNonCompletable(parseFail, informationProviders)
            return
        }

        when (parseFail) {
            is CommandNotFoundFail -> {
                val cmd = parseFail.commandStr.getString()

                this.autoCompleters.completeCommand(null,
                        parseFail.parsedCommands,
                        completions,
                        parseFail.manager,
                        informationProviders)

                completions.retainIfAnyMatch { it.startsWith(cmd) }
            }
            is ArgumentsMissingFail -> {
                val command = parseFail.command
                val parsedArgs = parseFail.parsedArgs

                if (iter.suggestBlankSpace()) {
                    suggestion += " "
                } else {
                    if (parsedArgs.isEmpty()
                            || parsedArgs.size >= command.arguments.getArguments(parsedArgs).count { !it.isOptional }) {
                        this.autoCompleters.completeCommand(command,
                                parseFail.parsedCommands,
                                completions,
                                parseFail.manager,
                                informationProviders)
                    }

                    suggestArguments(command, parsedArgs, iter, completions, informationProviders)
                }

            }
            is ArgumentNotFoundFail -> {
                val command = parseFail.command
                val parsedArgs = parseFail.parsedArgs
                val input: String = parseFail.input

                this.autoCompleters.completeArgumentName(command, parsedArgs, completions, informationProviders)

                completions.retainIfAnyMatch { it.startsWith(input) }
                completions.map { "--$it" }
            }
            is ArgumentInputParseFail -> {
                val command = parseFail.command
                val argument = parseFail.arg
                val parsedArgs = parseFail.parsedArgs
                val fail = parseFail.inputParseFail

                if (fail is TypeNotFoundFail) {
                    return
                }

                when (fail) {
                    is TokenExpectedFail -> {
                        suggestion += fail.tokens.map { it.toString() }
                    }
                    is TokenOrElementExpectedFail -> {
                        suggestion += fail.tokens.map { it.toString() }

                        this.autoCompleters.completeArgumentInput(command,
                                parsedArgs,
                                argument,
                                fail.argumentType,
                                fail.input,
                                completions,
                                informationProviders)
                    }
                    is InvalidInputForArgumentTypeFail -> {
                        this.autoCompleters.completeArgumentInput(command,
                                parsedArgs,
                                argument,
                                fail.argumentType,
                                null,
                                completions,
                                informationProviders)

                        completions.retainIfAnyMatch { it.startsWith(fail.input.getString()) }
                    }
                    is NextElementNotFoundFail -> {
                        when (fail.argumentType.inputType) {
                            is ListInputType -> completions.add("[")
                            is MapInputType -> completions.add("{")
                            else -> this.autoCompleters.completeArgumentInput(command, parsedArgs,
                                    argument,
                                    fail.argumentType,
                                    fail.input,
                                    completions,
                                    informationProviders)
                        }
                    }
                    is NoMoreElementsInputParseFail -> {
                        if (iter.suggestBlankSpace())
                            suggestion += " "
                        else
                            this.autoCompleters.completeArgumentInput(command, parsedArgs,
                                    argument,
                                    fail.argumentType,
                                    fail.input,
                                    completions,
                                    informationProviders)
                    }
                    else -> {
                        this.autoCompleters.completeArgumentInput(command, parsedArgs,
                                argument,
                                fail.argumentType,
                                fail.input,
                                completions,
                                informationProviders)
                    }
                }

            }
        }

        suggestion += completionList
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

    private fun ListCompletionsImpl.merge(other: ListCompletionsImpl) {
        this.list.addAll(other.list)
    }

    private inline fun ListCompletionsImpl.map(transform: (String) -> String) {
        val new = this.list.toMutableList().map(transform)
        this.list.clear()
        this.list.addAll(new)
    }

    private fun ListCompletionsImpl.retainIfAnyMatch(pred: (String) -> Boolean) {
        if (this.list.any(pred))
            this.list.retainAll(pred)
    }
}