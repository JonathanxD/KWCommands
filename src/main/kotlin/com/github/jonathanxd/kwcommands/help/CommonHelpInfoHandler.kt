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

import com.github.jonathanxd.iutils.function.collector.Collectors3
import com.github.jonathanxd.iutils.text.Text
import com.github.jonathanxd.iutils.text.TextComponent
import com.github.jonathanxd.iutils.text.converter.TextLocalizer
import com.github.jonathanxd.kwcommands.Texts
import com.github.jonathanxd.kwcommands.argument.ArgumentContainer
import com.github.jonathanxd.kwcommands.command.CommandContainer
import com.github.jonathanxd.kwcommands.command.Container
import com.github.jonathanxd.kwcommands.exception.*
import com.github.jonathanxd.kwcommands.fail.*
import com.github.jonathanxd.kwcommands.parser.*
import com.github.jonathanxd.kwcommands.printer.Printer
import com.github.jonathanxd.kwcommands.processor.CommandResult
import com.github.jonathanxd.kwcommands.processor.MissingInformationResult
import com.github.jonathanxd.kwcommands.processor.UnsatisfiedRequirementsResult
import com.github.jonathanxd.kwcommands.util.*

class CommonHelpInfoHandler : HelpInfoHandler {

    override fun handleCommandException(commandException: CommandException, printer: Printer) {
        when (commandException) {
            is ArgumentsMissingException -> {
                val command = commandException.command
                val providedArgs = commandException.providedArgs
                val missing = command.arguments.filter { arg ->
                    !arg.isOptional && providedArgs.none { it.argument == arg }
                }

                printer.printPlain(Texts.getArgumentCommandsMissingText(command.fullname))

                if (providedArgs.isNotEmpty()) {
                    val args = providedArgs.joinToString { it.argument.id.toString() }
                    printer.printPlain(Text.of("  ", Texts.getProvidedArgumentsText(), ": ", args))
                }

                printer.printPlain(Text.of("  ", Texts.getMissingArgumentText(), ": ",
                        missing.joinToString { it.id.toString() }))
                printer.printEmpty()
                printer.printPlain(Texts.getCommandSpecificationText())
                printer.printFromRoot(command, 0)
                printer.flush()
            }

            is CommandNotFoundException -> {
                printer.printPlain(Texts.getMissingCommandText(commandException.commandStr.toInputString()))

                if (commandException.parsedCommands.isNotEmpty()) {
                    printer.printPlain(Text.of(
                            Texts.getProcessedCommandsText(),
                            ": ",
                            commandException.parsedCommands.joinToString(
                                    prefix = "'",
                                    separator = ",",
                                    postfix = "'") {
                                "${it.command.fullname} ${it.arguments.filter { it.isDefined }.joinToString { it.input.toString() }}"
                            }))
                }

                printer.printPlain(Text.of(Texts.getAvailableCommandsText(), ":"))
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
                val validation = commandException.validation

                printer.printPlain(Texts.getInvalidInputValueText(input.toInputString(), argument.nameOrId, command.fullname))

                if (parsed.isNotEmpty()) {
                    printer.printPlain(Text.of("  ",
                            Texts.getParsedArgumentsText(),
                            ": ",
                            parsed.joinToString { it.argument.id.toString() }))
                }

                if (validation.invalids.isNotEmpty()) {
                    printer.printEmpty()
                    printer.printPlain(Texts.getInvalidInputsText().and(Text.of(":")))
                    for ((input, validator, msg, supported) in validation.invalids) {
                        printer.printEmpty()

                        val rangeText = Text.of(
                                Texts.getInputRangeText(input.start.toString(), input.end.toString()), ": ")
                        val text = printer.localizer.localize(rangeText)
                        val range = (text.length + input.start)..(text.length + input.end)
                        printer.printPlain(Text.of(
                                " | ",
                                text,
                                input.source,
                                " | ",
                                Texts.getInputText(), ": ", input.toInputString()))
                        printer.printPlain(Text.of(" | ", point(range)))

                        printer.printPlain(Text.of(" | ", Texts.getInputTypeText(),
                                ": ", input.type.getTypeString()))

                        val supportedText = supported.map { it.getTypeString() }
                                .reduce { acc, textComponent -> acc.append(", ").append(textComponent) }

                        printer.printPlain(Text.of(" | ", Texts.getValidInputTypesText(),
                                ": ", supportedText
                        ))
                        msg?.let { printer.printPlain(Text.of(" | ", Texts.getMessageText(), ": ", it)) }
                        printer.printPlain(Text.of(" | ", Texts.getValidatorText(), ": ", validator.name))
                    }
                    printer.printEmpty()
                }

                printer.printPlain(Text.of(Texts.getArgumentTypeText(), ": ", argument.typeStr))

                val poss = argument.type.possibilities()

                printPossibilities(poss, PrefixedPrinter(printer, "  - "))

                printer.printEmpty()
                printer.printPlain(Texts.getCommandSpecificationText().and(Text.of(": ")))
                printer.printFromRoot(command, 0)
                printer.flush()
            }

            is NoInputForArgumentException -> {
                val command = commandException.command
                val parsed = commandException.parsedArgs
                val arg = commandException.arg
                printer.printPlain(Texts.getNoInputForArgumentText(arg.nameOrId, command.fullname))

                if (parsed.isNotEmpty()) {
                    printer.printPlain(Text.of(Texts.getParsedArgumentsText(),
                            ": ",
                            parsed.joinToString { it.argument.id.toString() }))
                }

                printer.printEmpty()
                printer.printPlain(Texts.getCommandSpecificationText().and(Text.of(":")))
                printer.printFromRoot(command, 0)
                printer.flush()
            }

            is ArgumentNotFoundException -> {
                val command = commandException.command
                val parsed = commandException.parsedArgs
                val input = commandException.input
                printer.printPlain(Texts.getArgumentNotFoundText(input, command.fullname))

                if (parsed.isNotEmpty()) {
                    printer.printPlain(Text.of(Texts.getParsedArgumentsText(),
                            ": ",
                            parsed.joinToString { it.argument.id.toString() }))
                }

                printer.printEmpty()
                printer.printPlain(Texts.getCommandSpecificationText().and(Text.of(":")))
                printer.printFromRoot(command, 0)
                printer.flush()
            }

            else -> throw commandException
        }


    }

    override fun handleFail(parseFail: ParseFail, printer: Printer) {
        when (parseFail) {
            is ArgumentsMissingFail -> {
                val command = parseFail.command
                val providedArgs = parseFail.providedArgs
                val missing = command.arguments.filter { arg ->
                    !arg.isOptional && providedArgs.none { it.argument == arg }
                }

                printer.printPlain(Texts.getArgumentCommandsMissingText(command.fullname))

                if (providedArgs.isNotEmpty()) {
                    val args = providedArgs.joinToString { it.argument.id.toString() }
                    printer.printPlain(Text.of("  ", Texts.getProvidedArgumentsText(), ": ", args))
                }

                printer.printPlain(Text.of("  ", Texts.getMissingArgumentText(), ": ",
                        missing.joinToString { it.id.toString() }))
                printer.printEmpty()
                printer.printPlain(Texts.getCommandSpecificationText())
                printer.printFromRoot(command, 0)
                printer.flush()
            }

            is ArgumentNotFoundFail -> {
                val command = parseFail.command
                val parsed = parseFail.parsedArgs
                val input = parseFail.input
                printer.printPlain(Texts.getArgumentNotFoundText(input, command.fullname))

                if (parsed.isNotEmpty()) {
                    printer.printPlain(Text.of(Texts.getParsedArgumentsText(),
                            ": ",
                            parsed.joinToString { it.argument.id.toString() }))
                }

                printer.printEmpty()
                printer.printPlain(Texts.getCommandSpecificationText().and(Text.of(":")))
                printer.printFromRoot(command, 0)
                printer.flush()
            }

            is CommandNotFoundFail -> {
                printer.printPlain(Texts.getMissingCommandText(parseFail.commandStr.toInputString()))

                if (parseFail.parsedCommands.isNotEmpty()) {
                    printer.printPlain(Text.of(
                            Texts.getProcessedCommandsText(),
                            ": ",
                            parseFail.parsedCommands.joinToString(
                                    prefix = "'",
                                    separator = ", ",
                                    postfix = "'") {
                                if (it.arguments.isNotEmpty())
                                    "${it.command.fullname} ${it.arguments.filter { it.isDefined }.joinToString { it.input.toString() }}"
                                else
                                    it.command.fullname
                            }))
                }

                printer.printPlain(Text.of(Texts.getAvailableCommandsText(), ":"))
                parseFail.manager.createListWithAllCommands().forEach {
                    printer.printCommand(it, it.level)
                }
                printer.flush()
            }

            is InvalidInputForArgumentFail -> {
                val command = parseFail.command
                val parsed = parseFail.parsedArgs
                val argument = parseFail.arg
                val input = parseFail.input
                val validation = parseFail.validation

                printer.printPlain(Texts.getInvalidInputValueText(input.toInputString(), argument.nameOrId, command.fullname))

                if (parsed.isNotEmpty()) {
                    printer.printPlain(Text.of("  ",
                            Texts.getParsedArgumentsText(),
                            ": ",
                            parsed.joinToString { it.argument.id.toString() }))
                }

                if (validation.invalids.isNotEmpty()) {
                    printer.printEmpty()
                    printer.printPlain(Texts.getInvalidInputsText().and(Text.of(":")))
                    for ((input, validator, msg, supported) in validation.invalids) {
                        printer.printEmpty()

                        val rangeText = Text.of(
                                Texts.getInputRangeText(input.start.toString(), input.end.toString()), ": ")
                        val text = printer.localizer.localize(rangeText)
                        val range = (text.length + input.start)..(text.length + input.end)
                        printer.printPlain(Text.of(
                                " | ",
                                text,
                                input.source,
                                " | ",
                                Texts.getInputText(), ": ", input.toInputString()))
                        printer.printPlain(Text.of(" | ", point(range)))

                        printer.printPlain(Text.of(" | ", Texts.getInputTypeText(),
                                ": ", input.type.getTypeString()))

                        val supportedText = supported.map { it.getTypeString() }
                                .reduce { acc, textComponent -> acc.append(", ").append(textComponent) }

                        printer.printPlain(Text.of(" | ", Texts.getValidInputTypesText(),
                                ": ", supportedText
                        ))
                        msg?.let { printer.printPlain(Text.of(" | ", Texts.getMessageText(), ": ", it)) }
                        printer.printPlain(Text.of(" | ", Texts.getValidatorText(), ": ", validator.name))
                    }
                    printer.printEmpty()
                }

                printer.printPlain(Text.of(Texts.getArgumentTypeText(), ": ", argument.typeStr))

                val poss = argument.type.possibilities()

                printPossibilities(poss, PrefixedPrinter(printer, "  - "))

                printer.printEmpty()
                printer.printPlain(Texts.getCommandSpecificationText().and(Text.of(": ")))
                printer.printFromRoot(command, 0)
                printer.flush()
            }

            is NoInputForArgumentFail -> {
                val command = parseFail.command
                val parsed = parseFail.parsedArgs
                val arg = parseFail.arg
                printer.printPlain(Texts.getNoInputForArgumentText(arg.nameOrId, command.fullname))

                if (parsed.isNotEmpty()) {
                    printer.printPlain(Text.of(Texts.getParsedArgumentsText(),
                            ": ",
                            parsed.joinToString { it.argument.id.toString() }))
                }

                printer.printEmpty()
                printer.printPlain(Texts.getCommandSpecificationText().and(Text.of(":")))
                printer.printFromRoot(command, 0)
                printer.flush()
            }

            is ArgumentInputParseFail -> {
                val command = parseFail.command
                val parsed = parseFail.parsedArgs
                val argument = parseFail.arg
                val start = parseFail.iter.sourceIndex
                val fail = parseFail.inputParseFail
                val inputType = parseFail.inputParseFail.argumentType.inputType

                printer.printPlain(
                        Text.of(Texts.getMalformedInputText(inputType.getTypeString())))

                if (parsed.isNotEmpty()) {
                    printer.printPlain(Text.of("  ",
                            Texts.getParsedArgumentsText(),
                            ": ",
                            parsed.joinToString { it.argument.id.toString() }))
                }

                printer.printEmpty()

                val source = parseFail.iter.sourceString

                val rangeText = Text.of(
                        Texts.getInputRangeText(start.toString(), (source.length - 1).toString()), ": ")
                val text = printer.localizer.localize(rangeText)
                val range = (text.length + start)..(text.length + source.length - 1)
                printer.printPlain(Text.of(" | ", text, source))
                printer.printPlain(Text.of(" | ", point(range)))
                printer.printPlain(Text.of(" | ", Texts.getValidInputTypesText(),
                        ": ", inputType.getTypeString()
                ))

                when (fail) {
                    is ListTokenExpectedFail, is MapTokenExpectedFail -> {
                        val tokens = (fail as? ListTokenExpectedFail)?.tokens
                                ?: (fail as MapTokenExpectedFail).tokens

                        val foundToken = (fail as? ListTokenExpectedFail)?.foundToken
                                ?: (fail as MapTokenExpectedFail).foundToken

                        if (tokens.isNotEmpty()) {
                            printer.printPlain(Text.of(" | ", Texts.getExpectedTokensText(), ": ",
                                    tokens.joinToString { "'$it'" }, "."))
                        }

                        printer.printPlain(Text.of(" | ", Texts.getFoundTokenText(), ": ",
                                "'", foundToken, "'."))

                        when {
                            fail is ListTokenExpectedFail && (fail.input as ListInput).input.isNotEmpty() -> {
                                val list = fail.input.input.joinToString(prefix = "[", postfix = "]") {
                                    it.getString()
                                }
                                printer.printPlain(Text.of(" | ", Texts.getParsedListText(), ": ", list, "."))
                            }
                            fail is MapTokenExpectedFail && (fail.input as MapInput).input.isNotEmpty() -> {
                                val map = fail.input.input.joinToString(prefix = "{", postfix = "}") {
                                    "${it.first.getString()}=${it.second.getString()}"
                                }

                                printer.printPlain(Text.of(" | ", Texts.getParsedMapText(), ": ", map, "."))
                            }
                        }
                    }
                }

                printer.printEmpty()

                printer.printPlain(Text.of(Texts.getArgumentTypeText(), ": ", argument.typeStr))

                val poss = argument.type.possibilities()

                printPossibilities(poss, PrefixedPrinter(printer, "  - "))

                printer.printEmpty()
                printer.printPlain(Texts.getCommandSpecificationText().and(Text.of(": ")))
                printer.printFromRoot(command, 0)
                printer.flush()
            }

        }
    }


    private fun printPossibilities(possibilities: List<Input>, printer: Printer) {
        if (possibilities.isNotEmpty()) {
            printer.printPlain(Texts.getArgumentPossibilitiesText().and(Text.of(": ")))

            possibilities.map {
                printPossibility(it, printer)
            }

        }
    }

    private fun printPossibility(possibility: Input, printer: Printer) {
        when (possibility) {
            is SingleInput -> {
                printer.printPlain(Text.of(possibility.input))
            }
            is ListInput -> {
                if (possibility.input.all { it is SingleInput }) {
                    val list = possibility.input
                            .map { it as SingleInput }
                            .map(SingleInput::input)
                            .stream()
                            .collect(Collectors3.split(10))
                    list.forEach {
                        it.forEach {
                            printer.printPlain(Text.of(it))
                        }
                    }
                } else {
                    possibility.input.forEach {
                        printPossibility(it, printer)
                    }
                }
            }
            is MapInput -> {
                possibility.input.forEach { (k, v) ->
                    printer.printPlain(Text.of(Texts.getKeyText(), ": "))
                    printPossibility(k, printer)
                    printer.printPlain(Text.of(Texts.getValueText(), ": "))
                    printPossibility(v, printer)
                    printer.printEmpty()
                }
            }
            else -> {
                printer.printPlain(Text.of(possibility.getString()))
            }
        }
    }

    private fun CommandResult.str(): TextComponent {
        fun Container.containerStr(): TextComponent = when (this) {

            is CommandContainer -> Text.of(Texts.getCommandText().decapitalize(), " ", this.command.fullname)
            is ArgumentContainer<*> -> Text.of(Texts.getArgumentText().decapitalize(), " ", this.argument.nameOrId)
            else -> Text.of(this.toString())
        }

        return this.container.containerStr().and(
                if (rootContainer != null) Texts.getReqContinuation(rootContainer!!.containerStr()).decapitalize()
                else Text.of()
        )

    }

    override fun handleResults(commandResults: List<CommandResult>, printer: Printer) {
        commandResults.forEach {
            this.handleResult(it, printer)

            if (commandResults.size > 1) {
                printer.printEmpty()
                printer.flush()
            }
        }
    }

    override fun handleResult(commandResult: CommandResult, printer: Printer) {
        when (commandResult) {
            is UnsatisfiedRequirementsResult -> {
                val unsatisfied = commandResult.unsatisfiedRequirements

                printer.printPlain(Texts.getUnsatisfiedRequirementText(commandResult.str()))

                if (unsatisfied.isNotEmpty())
                    unsatisfied.forEach {
                        val tags =
                                if (it.informationId.tags.isNotEmpty())
                                    Text.of(" ", Texts.getTagsText(), ": ", it.informationId.tags.joinToString(), ".")
                                else
                                    Text.of(".")

                        printer.printPlain(Text.of("  ", Texts.getInformationIdentificationText(), ":",
                                " ", Texts.getTypeText(), ": ", it.informationId.type, tags
                        ))

                        printer.printPlain(Text.of("  ",
                                Texts.getPresentText(), ": ", "${it.information != null}. ",
                                if (it.information != null) Text.of(Texts.getValueText(), ": ", it.information.value)
                                else Text.of()))

                        printer.printPlain(Text.of("  ", Texts.getRequiredText(), ": ", it.requirement.required))
                        printer.printPlain(Text.of("  ", Texts.getTesterText(), ": ", it.requirement.tester))
                        printer.printPlain(Text.of("  ", Texts.getReasonText(), ": ", it.reason.name))
                    }

                printer.flush()
            }
            is MissingInformationResult -> {
                val missing = commandResult.missingInformationList

                printer.printPlain(Texts.getMissingInformationText(commandResult.str()).append(Text.of(":")))

                if (missing.isNotEmpty())
                    missing.forEach {
                        val tags =
                                if (it.requiredInfo.id.tags.isNotEmpty())
                                    Text.of(" ", Texts.getTagsText(), ": ", it.requiredInfo.id.tags.joinToString(), ".")
                                else
                                    Text.of(".")

                        printer.printPlain(Text.of("  ",
                                " ", Texts.getTypeText(), ": ", it.requiredInfo.id.type, ".",
                                tags,
                                " ", Texts.getIncludeProvidedText(),
                                ": ", if (it.requiredInfo.useProviders) Texts.getTrueText() else Texts.getFalseText(),
                                "."
                        ))
                    }

                printer.flush()
            }
        }
    }

    internal class PrefixedPrinter(private val wrapped: Printer,
                                   val prefix: String) : Printer by wrapped {
        override val localizer: TextLocalizer
            get() = this.wrapped.localizer

        override fun printPlain(text: TextComponent) {
            this.wrapped.printPlain(Text.of(prefix).append(text))
        }

        override fun printEmpty() {
            this.wrapped.printEmpty()
        }

        override fun flush() {
            this.wrapped.flush()
        }

    }

}