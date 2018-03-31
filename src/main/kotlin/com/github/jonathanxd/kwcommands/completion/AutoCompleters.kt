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
import com.github.jonathanxd.kwcommands.parser.Input

/**
 * An auto-completer that delegate invocations to all [completers].
 */
class AutoCompleters(private val completers: List<AutoCompleter>) : AutoCompleter {

    override fun handleNonCompletable(
        fail: ParseFail,
        informationProviders: InformationProviders,
        localizer: Localizer?
    ) {
        completers.forEach {
            it.handleNonCompletable(fail, informationProviders, localizer)
        }
    }

    override fun completeCommand(
        command: Command?,
        commandContainers: List<CommandContainer>,
        completions: Completions,
        commandManager: CommandManager,
        informationProviders: InformationProviders,
        localizer: Localizer?
    ) {
        completers.forEach {
            it.completeCommand(
                command,
                commandContainers,
                completions,
                commandManager,
                informationProviders,
                localizer
            )
        }
    }

    override fun completeArgumentName(
        command: Command,
        arguments: List<ArgumentContainer<*>>,
        completions: Completions,
        informationProviders: InformationProviders,
        localizer: Localizer?
    ) {
        completers.forEach {
            it.completeArgumentName(
                command,
                arguments,
                completions,
                informationProviders,
                localizer
            )
        }
    }

    override fun completeArgumentInput(
        command: Command,
        arguments: List<ArgumentContainer<*>>,
        argument: Argument<*>,
        argumentType: ArgumentType<*, *>,
        input: Input?,
        completions: Completions,
        informationProviders: InformationProviders,
        localizer: Localizer?
    ) {
        completers.forEach {
            it.completeArgumentInput(
                command,
                arguments,
                argument,
                argumentType,
                input,
                completions,
                informationProviders,
                localizer
            )
        }

    }


}