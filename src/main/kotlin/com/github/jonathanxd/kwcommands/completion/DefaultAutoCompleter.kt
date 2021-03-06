/*
 *      KWCommands - New generation of WCommands written in Kotlin <https://github.com/JonathanxD/KWCommands>
 *
 *         The MIT License (MIT)
 *
 *      Copyright (c) 2020 JonathanxD
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

import com.github.jonathanxd.iutils.kt.get
import com.github.jonathanxd.iutils.text.localizer.Localizer
import com.github.jonathanxd.kwcommands.NamedAndAliased
import com.github.jonathanxd.kwcommands.argument.Argument
import com.github.jonathanxd.kwcommands.argument.ArgumentContainer
import com.github.jonathanxd.kwcommands.argument.ArgumentType
import com.github.jonathanxd.kwcommands.command.Command
import com.github.jonathanxd.kwcommands.command.CommandContainer
import com.github.jonathanxd.kwcommands.information.InformationProviders
import com.github.jonathanxd.kwcommands.manager.CommandManager
import com.github.jonathanxd.kwcommands.parser.*

class DefaultAutoCompleter : AutoCompleter {

    override fun completeCommand(
        command: Command?,
        commandContainers: List<CommandContainer>,
        completions: Completions,
        commandManager: CommandManager,
        informationProviders: InformationProviders,
        localizer: Localizer?
    ) {
        val commands = mutableListOf<Command>()
        val last = commandContainers.lastOrNull()
        val root = if (command?.parent == null) command else command.superCommand

        command?.subCommands?.let {
            commands += it
        }

        commands += if (last?.command == command)
            commandManager.registeredCommands.filter { it != root }
        else
            last?.command?.subCommands.orEmpty()


        completions.addAll(commands.flatMap { it.completions(localizer) })
    }

    override fun completeArgumentName(
        command: Command,
        arguments: List<ArgumentContainer<*>>,
        completions: Completions,
        informationProviders: InformationProviders,
        localizer: Localizer?,
        appendArgPrefix: Boolean
    ) {
        completions.addAll(command.arguments.getRemainingArguments(arguments)
            .flatMap {
                it.completions(localizer)
            }.map { if (appendArgPrefix) "--$it" else it })
    }

    private fun NamedAndAliased.completions(localizer: Localizer?): List<String> {
        val completions = mutableListOf<String>()
        completions += this.name
        completions += this.alias

        localizer?.let { loc ->
            loc[this.nameComponent].let {
                if (it != this.name)
                    completions += it
            }
            this.aliasComponent?.let(loc::getLocalizations)?.let {
                completions += it.map { loc[it] }
            }
        }

        return completions
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

        when (argumentType.inputType) {
            is SingleInputType -> {
                argumentType
            }
            is ListInputType -> {
                val last = (input as? ListInput)?.input?.size ?: 0

                argumentType.getListType((input as? ListInput)?.input.orEmpty(), last)
            }
            is MapInputType -> {
                val last = (input as? MapInput)?.input?.size ?: 0

                if (input is MapInput
                        && input.input.isNotEmpty()
                        && input.input.last().second is EmptyInput
                ) {
                    argumentType.getMapValueType(input.input.last().first, last)
                } else {
                    argumentType.getMapKeyType((input as? MapInput)?.input.orEmpty(), last)
                }
            }
            else -> null
        }?.let {
            completions.addAll(it.possibilities().map { it.getString() })
        }


    }

}