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
package com.github.jonathanxd.kwcommands.help

import com.github.jonathanxd.kwcommands.argument.ArgumentContainer
import com.github.jonathanxd.kwcommands.command.CommandContainer
import com.github.jonathanxd.kwcommands.command.Container
import com.github.jonathanxd.kwcommands.exception.*
import com.github.jonathanxd.kwcommands.printer.Printer
import com.github.jonathanxd.kwcommands.processor.CommandResult
import com.github.jonathanxd.kwcommands.processor.MissingInformationResult
import com.github.jonathanxd.kwcommands.processor.UnsatisfiedRequirementsResult
import com.github.jonathanxd.kwcommands.util.level
import com.github.jonathanxd.kwcommands.util.nameOrId
import com.github.jonathanxd.kwcommands.util.typeStr

class CommonHelpInfoHandler : HelpInfoHandler {

    override fun handleCommandException(commandException: CommandException, printer: Printer) {
        when (commandException) {
            is ArgumentsMissingException -> {
                val command = commandException.command
                val providedArgs = commandException.providedArgs
                val missing = command.arguments.filter { arg ->
                    !arg.isOptional && providedArgs.none { it.argument == arg }
                }

                printer.printPlain("Some arguments of command '${command.fullname}' is missing:")

                if (providedArgs.isNotEmpty())
                    printer.printPlain("  Provided: ${providedArgs.joinToString { it.argument.id.toString() }}")

                printer.printPlain("  Missing: ${missing.joinToString { it.id.toString() }}")
                printer.printPlain("")
                printer.printPlain("Command specification:")
                printer.printFromRoot(command, 0)
                printer.flush()
            }

            is CommandNotFoundException -> {
                printer.printPlain("Command with name '${commandException.commandStr}' was not found.")

                if (commandException.commands.isNotEmpty())
                    printer.printPlain("Processed commands: ${commandException.commands.joinToString(
                            prefix = "'",
                            separator = ",",
                            postfix = "'") {
                        "${it.command.fullname} ${it.arguments.filter { it.isDefined }.joinToString { it.input.toString() }}"
                    }}")

                printer.printPlain("Available commands:")
                commandException.manager.createListWithAllCommands().forEach {
                    printer.printCommand(it, it.level)
                }
                printer.flush()
            }

            is InvalidInputForArgumentException -> {
                val command = commandException.command
                val parsed = commandException.parsedArgs
                val argument = commandException.arg
                val input = commandException.input
                printer.printPlain("Invalid input value '$input' provided for argument " +
                        "'${argument.nameOrId}' of command '${command.fullname}'")

                if (parsed.isNotEmpty())
                    printer.printPlain("  Successfully parsed args: ${parsed.joinToString { it.argument.id.toString() }}")

                printer.printPlain("Argument type: ${argument.typeStr}")

                if (argument.possibilities.isNotEmpty())
                    printer.printPlain("Argument possibilities: ${argument.possibilities.joinToString()}")

                printer.printPlain("")
                printer.printPlain("Command specification:")
                printer.printFromRoot(command, 0)
                printer.flush()
            }

            is NoArgumentForInputException -> {
                val command = commandException.command
                val parsed = commandException.parsedArgs
                val input = commandException.input
                printer.printPlain("Input value $input is not a valid for any argument of command '${command.fullname}'")

                if (parsed.isNotEmpty())
                    printer.printPlain("  Successfully parsed args: ${parsed.joinToString { it.argument.id.toString() }}")

                printer.printPlain("")
                printer.printPlain("Command specification:")
                printer.printFromRoot(command, 0)
                printer.flush()
            }

            is NoInputForArgumentException -> {
                val command = commandException.command
                val parsed = commandException.parsedArgs
                val arg = commandException.arg
                printer.printPlain("No input value provided for named argument '${arg.nameOrId}' of command '${command.fullname}'")

                if (parsed.isNotEmpty())
                    printer.printPlain("  Successfully parsed args: ${parsed.joinToString { it.argument.id.toString() }}")

                printer.printPlain("")
                printer.printPlain("Command specification:")
                printer.printFromRoot(command, 0)
                printer.flush()
            }

            else -> throw commandException
        }


    }

    private fun CommandResult.str(): String {
        fun Container.containerStr(): String = when(this) {
            is CommandContainer -> "command ${this.command.fullname}"
            is ArgumentContainer<*> -> "argument ${this.argument.nameOrId}"
            else -> this.toString()
        }

        return this.container.containerStr() + if (rootContainer != null) " of ${rootContainer!!.containerStr()}" else ""
    }

    override fun handleResults(commandResults: List<CommandResult>, printer: Printer) {
        commandResults.forEach {
            when (it) {
                is UnsatisfiedRequirementsResult -> {
                    val unsatisfied = it.unsatisfiedRequirements

                    printer.printPlain("Unsatisfied requirements of '${it.str()}':")

                    if (unsatisfied.isNotEmpty())
                        unsatisfied.forEach {
                            printer.printPlain("  Information identification:" +
                                    " Type: ${it.informationId.type}." +
                                    " Tags: ${it.informationId.tags.joinToString()}.")
                            printer.printPlain("  Present: ${it.information != null}.${
                            if (it.information != null) ". Value: ${it.information.value}" else ""
                            }")
                            printer.printPlain("  Required: ${it.requirement.required}")
                            printer.printPlain("  Tester: ${it.requirement.tester}")
                            printer.printPlain("  Reason: ${it.reason.name}")
                        }

                    printer.flush()
                }
                is MissingInformationResult -> {
                    val missing = it.missingInformationList

                    printer.printPlain("Missing information of '${it.str()}':")

                    if (missing.isNotEmpty())
                        missing.forEach {
                            printer.printPlain("  " +
                                    " Type: ${it.requiredInfo.id.type}." +
                                    " Tags: ${it.requiredInfo.id.tags.joinToString()}." +
                                    " Include provided: ${it.requiredInfo.useProviders}.")
                        }

                    printer.flush()
                }
            }

            if (commandResults.size > 1) {
                printer.printPlain("")
                printer.flush()
            }
        }
    }

}