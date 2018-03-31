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
package com.github.jonathanxd.kwcommands.completion

import com.github.jonathanxd.iutils.text.localizer.Localizer
import com.github.jonathanxd.kwcommands.argument.Argument
import com.github.jonathanxd.kwcommands.argument.ArgumentContainer
import com.github.jonathanxd.kwcommands.argument.ArgumentType
import com.github.jonathanxd.kwcommands.command.Command
import com.github.jonathanxd.kwcommands.command.CommandContainer
import com.github.jonathanxd.kwcommands.fail.ParseFail
import com.github.jonathanxd.kwcommands.information.InformationProviders
import com.github.jonathanxd.kwcommands.manager.CommandManager
import com.github.jonathanxd.kwcommands.parser.CommandParser
import com.github.jonathanxd.kwcommands.parser.Input
import com.github.jonathanxd.kwcommands.parser.OwnerProvider

interface Completion {

    val parser: CommandParser

    val autoCompleterList: List<AutoCompleter>

    fun registerAutoCompleter(completer: AutoCompleter)
    fun unregisterAutoCompleter(completer: AutoCompleter)

    /**
     * Gets suggestions to complete [input].
     */
    fun complete(
        input: String,
        owner: Any?,
        informationProviders: InformationProviders
    ): List<String> =
        this.completeWithOwnerFunc(input, { owner }, informationProviders)

    /**
     * Gets suggestions to complete [input].
     */
    fun complete(
        input: String,
        owner: Any?,
        informationProviders: InformationProviders,
        localizer: Localizer?
    ): List<String> =
        this.completeWithOwnerFunc(input, { owner }, informationProviders, localizer)

    /**
     * Gets suggestions to complete [input].
     */
    fun completeWithOwnerFunc(
        input: String,
        ownerProvider: OwnerProvider,
        informationProviders: InformationProviders
    ): List<String> =
            this.completeWithOwnerFunc(input, ownerProvider, informationProviders, null)

    /**
     * Gets suggestions to complete [input].
     */
    fun completeWithOwnerFunc(
        input: String,
        ownerProvider: OwnerProvider,
        informationProviders: InformationProviders,
        localizer: Localizer?
    ): List<String>

}

interface Completions {
    /**
     * Adds a completion
     */
    fun add(completion: String)

    /**
     * Add all [completions]
     */
    fun addAll(completions: Iterable<String>) =
        completions.forEach { add(it) }

    /**
     * Add all [completions]
     */
    fun addAll(completions: Array<String>) =
        completions.forEach { add(it) }
}

/**
 * Auto completer interface.
 *
 * The implementation should provide all possible values for completion based on provided values,
 * the determination of which values will be really suggested to complete the input is determined
 * by the implementation of [Completion].
 *
 * Example, supposing that we have a command `hello` with argument `name` that can receive `WCommands` and `KWCommands`,
 * when `hello KW` is sent to completer to get suggestions, the implementation of this interface should return
 * `WCommands` and `KWCommands`, and the implementation of [Completion] will choose only `KWCommands` as completion.
 *
 * This class also receives an [InformationProviders] to provide some values, this is not used by default
 * implementation.
 */
interface AutoCompleter {

    /**
     * Handles a non-completable case. A non-completable case is the case where the parsing
     * cannot be finished because of a fail at the mid of it (example, trying to complete
     * an invalid input).
     */
    fun handleNonCompletable(
        fail: ParseFail,
        informationProviders: InformationProviders,
        localizer: Localizer?
    ) = Unit

    /**
     * Gets sub-command completion for [command].
     */
    fun completeCommand(
        command: Command?,
        commandContainers: List<CommandContainer>,
        completions: Completions,
        commandManager: CommandManager,
        informationProviders: InformationProviders,
        localizer: Localizer?
    ) = Unit

    /**
     * Gets completions for argument name (without `--`).
     */
    fun completeArgumentName(
        command: Command,
        arguments: List<ArgumentContainer<*>>,
        completions: Completions,
        informationProviders: InformationProviders,
        localizer: Localizer?
    ) = Unit

    /**
     * Gets completions for [argument].
     */
    fun completeArgumentInput(
        command: Command,
        arguments: List<ArgumentContainer<*>>,
        argument: Argument<*>,
        argumentType: ArgumentType<*, *>,
        input: Input?,
        completions: Completions,
        informationProviders: InformationProviders,
        localizer: Localizer?
    ) = Unit


}