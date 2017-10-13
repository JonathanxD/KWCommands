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

import com.github.jonathanxd.kwcommands.exception.*
import com.github.jonathanxd.kwcommands.printer.Printer
import com.github.jonathanxd.kwcommands.util.level
import com.github.jonathanxd.kwcommands.util.nameOrId
import com.github.jonathanxd.kwcommands.util.nameOrIdWithType
import com.github.jonathanxd.kwcommands.util.typeStr

class CommonHelpInfoHandler : HelpInfoHandler {

    override fun handleCommandException(commandException: CommandException, printer: Printer) {
        when(commandException) {
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
            else -> throw commandException
        }


    }

}