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
import com.github.jonathanxd.kwcommands.argument.ArgumentType
import com.github.jonathanxd.kwcommands.command.Command
import com.github.jonathanxd.kwcommands.command.CommandContainer
import com.github.jonathanxd.kwcommands.fail.*
import com.github.jonathanxd.kwcommands.information.InformationProviders
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
                    iter.sourceString,
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

    private fun ParseFail.suggestBlankSpace(): Boolean =
            (this is SourcedParseFail && !this.source.endsWith(" "))

    private fun SourcedCharIterator.isValid(): Boolean =
            this.runAndRestore {
                if (this.hasPrevious() && this.previous() == ' ')
                    this.hasPrevious() && this.previous() != ' '
                else
                    true
            }

    private fun suggestArguments(command: Command,
                                 parsedArgs: List<ArgumentContainer<*>>,
                                 source: String,
                                 completions: ListCompletionsImpl,
                                 informationProviders: InformationProviders,
                                 suggestName: Boolean = true) {

        if (suggestName) {
            val completions2 = ListCompletionsImpl()
            this.autoCompleters.completeArgumentName(command, parsedArgs, completions2, informationProviders)

            completions2.retainIfAnyMatch { parsedArgs.none { arg -> arg.argument.name == it } }
            completions2.map { "--$it" }

            completions.merge(completions2)
        }

        val next: Argument<*>? = command.arguments.getRemainingArguments(parsedArgs).let {
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
                            EmptyInput(source),
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

        val iter = parseFail.iter.char

        if (!iter.isValid())
            return

        val completions = ListCompletionsImpl()
        val completionList = completions.list

        if (parseFail.iter.char.hasNext()) {
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

                if (parseFail.suggestBlankSpace()) {
                    suggestion += " "
                } else {
                    if (parsedArgs.isEmpty()
                            || parsedArgs.size >= command.arguments.getRemainingArguments(parsedArgs).count { !it.isOptional }) {
                        this.autoCompleters.completeCommand(command,
                                parseFail.parsedCommands,
                                completions,
                                parseFail.manager,
                                informationProviders)
                    }

                    suggestArguments(command, parsedArgs, parseFail.source, completions, informationProviders)
                }

            }
            is ArgumentNotFoundFail -> {
                val command = parseFail.command
                val parsedArgs = parseFail.parsedArgs
                val input: Input = parseFail.input

                this.autoCompleters.completeArgumentName(command, parsedArgs, completions, informationProviders)

                completions.retainIfAnyMatch { it.startsWith(input.content) }
                completions.map { "--$it" }
            }
            is InvalidInputForArgumentFail -> {
                val command = parseFail.command
                val parsedArgs = parseFail.parsedArgs
                val argument = parseFail.arg
                val input = parseFail.input

                this.autoCompleters.completeArgumentInput(command,
                        parsedArgs,
                        argument,
                        argument.argumentType,
                        EmptyInput(parseFail.source),
                        completions,
                        informationProviders)
                completions.retainIfAnyMatch { it.startsWith(input.content) }
            }
            is NoInputForArgumentFail -> {
                val command = parseFail.command
                val parsedArgs = parseFail.parsedArgs
                val argument = parseFail.arg

                if (parseFail.suggestBlankSpace()) {
                    suggestion += " "
                } else {
                    this.autoCompleters.completeArgumentInput(command,
                            parsedArgs,
                            argument,
                            argument.argumentType,
                            EmptyInput(parseFail.source),
                            completions,
                            informationProviders)
                }
            }
            is ArgumentInputParseFail -> {
                val command = parseFail.command
                val argument = parseFail.arg
                val parsedArgs = parseFail.parsedArgs
                val fail = parseFail.inputParseFail

                when (fail) {
                    is TokenExpectedFail -> {
                        val elementType = getType(argument.argumentType, fail.root, fail.input)

                        if (elementType != null) {

                            val part = elementType.first
                            val type = elementType.second

                            if (type.parse(part).isValue) {
                                suggestion += fail.tokens.map { it.toString() }
                            }

                            val completions2 = ListCompletionsImpl()
                            this.autoCompleters.completeArgumentInput(command,
                                    parsedArgs,
                                    argument,
                                    elementType.second,
                                    part,
                                    completions2,
                                    informationProviders)

                            completions2.retainIfAnyMatch { it.startsWith(part.getString()) }
                            completions.merge(completions2)
                        } else {
                            suggestion += fail.tokens.map { it.toString() }
                        }
                    }
                    is TokenOrElementExpectedFail -> {
                        suggestion += fail.tokens.map { it.toString() }

                        val type = getType(argument.argumentType, fail.root, fail.input)

                        val input = type?.first ?: fail.input
                        val elementType = type?.second ?: argument.argumentType

                        this.autoCompleters.completeArgumentInput(command,
                                parsedArgs,
                                argument,
                                elementType,
                                input,
                                completions,
                                informationProviders)
                    }
                    is InvalidInputForArgumentTypeFail -> {
                        this.autoCompleters.completeArgumentInput(command,
                                parsedArgs,
                                argument,
                                argument.argumentType,
                                null,
                                completions,
                                informationProviders)

                        completions.retainIfAnyMatch { it.startsWith(fail.input.getString()) }
                    }
                    is NextElementNotFoundFail -> {

                        fun completeForType(type: ArgumentType<*, *>) =
                                when (type.inputType) {
                                    is ListInputType -> {
                                        completions.add("[")
                                    }
                                    is MapInputType -> completions.add("{")
                                    else -> this.autoCompleters.completeArgumentInput(command, parsedArgs,
                                            argument,
                                            type,
                                            fail.input,
                                            completions,
                                            informationProviders)
                                }

                        val current = fail.input

                        val argType = getType(argument.argumentType, fail.root, fail.input)

                        if (argType != null) {
                            completeForType(argType.second)
                        } else {

                            val type = when (current) {
                                is ListInput -> argument.argumentType.getListType(current.input.size)
                                is MapInput -> {
                                    if (current.input.lastOrNull()?.second is EmptyInput)
                                        argument.argumentType.getMapValueType(current.input.size)
                                    else
                                        argument.argumentType.getMapKeyType(current.input.size)
                                }
                                else -> argument.argumentType
                            }

                            completeForType(type)
                        }
                    }
                    is NoMoreElementsInputParseFail -> {
                        if (parseFail.suggestBlankSpace())
                            suggestion += " "
                        else {

                            this.suggestArguments(
                                    command,
                                    parsedArgs,
                                    parseFail.source,
                                    completions,
                                    informationProviders
                            )
                        }
                    }
                    else -> {
                        this.autoCompleters.completeArgumentInput(command, parsedArgs,
                                argument,
                                argument.argumentType,
                                fail.input,
                                completions,
                                informationProviders)
                    }
                }

            }
        }

        suggestion += completionList
    }


    private fun getType(argumentType: ArgumentType<*, *>, root: Input?, input: Input): Pair<Input, ArgumentType<*, *>>? {
        val currentInput = root ?: input

        when (currentInput) {
            is ListInput -> {
                if (currentInput.input.isEmpty()) {
                    return currentInput to argumentType
                }
                currentInput.input.forEachIndexed { i, elem ->
                    if (!argumentType.hasType(i))
                        return null

                    val e = argumentType.getListType(i)

                    if (i == currentInput.input.size - 1) {
                        if (elem is EmptyInput) {
                            return elem to e
                        } else {
                            if (elem.type.isCompatible(e.inputType)) {
                                getType(e, null, elem)?.let {
                                    return it
                                }
                            }

                            val kparse = e.parse(elem)

                            if (kparse.isInvalid) {
                                return elem to e
                            }

                        }
                    }
                }
            }
            is MapInput -> {
                if (currentInput.input.isEmpty())
                    return currentInput to argumentType

                currentInput.input.forEachIndexed { i, pair ->
                    if (!argumentType.hasType(i))
                        return null

                    val k = argumentType.getMapKeyType(i)
                    val v = argumentType.getMapValueType(i)

                    if (i == currentInput.input.size - 1) {
                        val key = pair.first
                        val value = pair.second
                        when {
                            key is EmptyInput -> return key to k
                            value is EmptyInput -> return value to v
                            else -> {
                                if (key.type.isCompatible(k.inputType))
                                    getType(k, null, key)?.let {
                                        return it
                                    }

                                if (value.type.isCompatible(v.inputType))
                                    getType(v, null, value)?.let {
                                        return it
                                    }

                                val kparse = k.parse(key)

                                if (kparse.isInvalid) {
                                    return key to k
                                }

                                val vparse = v.parse(value)

                                if (vparse.isInvalid) {
                                    return value to v
                                }
                            }
                        }
                    }
                }
            }
            is EmptyInput -> {
                //if (currentInput.type.isCompatible(argumentType.inputType))
                return currentInput to argumentType
            }
            else -> {}
        }

        return null
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