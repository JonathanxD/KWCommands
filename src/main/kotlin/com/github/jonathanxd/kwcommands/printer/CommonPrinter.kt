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
package com.github.jonathanxd.kwcommands.printer

import com.github.jonathanxd.iutils.text.Text
import com.github.jonathanxd.iutils.text.TextComponent
import com.github.jonathanxd.iutils.text.localizer.TextLocalizer
import com.github.jonathanxd.iutils.kt.asText
import com.github.jonathanxd.kwcommands.Texts
import com.github.jonathanxd.kwcommands.command.Command
import com.github.jonathanxd.kwcommands.dsl.command
import com.github.jonathanxd.kwcommands.information.RequiredInformation
import com.github.jonathanxd.kwcommands.requirement.Requirement
import com.github.jonathanxd.kwcommands.util.append
import com.github.jonathanxd.kwcommands.util.level

/**
 * Common implementation of command printer backing to a print function
 */
class CommonPrinter(override val localizer: TextLocalizer,
                    val out: (String) -> Unit,
                    val printHeaderAndFooter: Boolean = true) : Printer {

    private val buffer = mutableListOf<TextComponent>()
    private val commands = mutableListOf<Command>()

    init {
        if (printHeaderAndFooter) Companion.printHeader(commands, buffer)
    }

    override fun printCommand(command: Command, level: Int) {
        Companion.printTo(buffer, commands, command, level)
    }

    override fun printFromRoot(command: Command, level: Int) {
        Companion.printFromRoot(buffer, commands, command, level)
    }

    override fun printTo(command: Command, level: Int, out: (String) -> Unit) {
        val buffer = mutableListOf<TextComponent>()
        val commands = mutableListOf<Command>()

        if (printHeaderAndFooter) Companion.printHeader(commands, buffer)

        Companion.printTo(buffer, commands, command, level)

        if (printHeaderAndFooter) Companion.printFooter(commands, buffer)

        Companion.flushTo(out, commands, buffer, this.localizer)
    }

    override fun printEmpty() {
        Companion.printEmpty(this.commands, this.buffer)
    }

    override fun printPlain(text: TextComponent) {
        Companion.printPlain(text, this.commands, this.buffer)
    }

    override fun flush() {
        if (printHeaderAndFooter) Companion.printFooter(commands, buffer)

        Companion.flushTo(this.out, this.commands, this.buffer, this.localizer)
        this.commands.clear()
        this.buffer.clear()

        if (printHeaderAndFooter) Companion.printHeader(commands, buffer)
    }

    companion object {
        private val DummyCommand = command {
            name { "dummy" }
            order = -1
        }

        private val header = listOf(
                Texts.header1(),
                Texts.header2(),
                Texts.header3(),
                Texts.header4(),
                Texts.header5(),
                Texts.header6(),
                Texts.header7(),
                Texts.header8(),
                Texts.header9(),
                Texts.header10(),
                Texts.header11(),
                Texts.header12())

        private val footer = listOf(
                Texts.footer1(),
                Texts.footer2()
        )

        fun printPlain(text: TextComponent,
                       commands: MutableList<Command>,
                       buffer: MutableList<TextComponent>) {
            commands += DummyCommand
            buffer += text
        }

        fun printEmpty(commands: MutableList<Command>, buffer: MutableList<TextComponent>) {
            commands += DummyCommand
            buffer += Text.single("")
        }

        fun printHeader(commands: MutableList<Command>, buffer: MutableList<TextComponent>) {
            header.forEach {
                printPlain(it, commands, buffer)
            }
        }

        fun printFooter(commands: MutableList<Command>, buffer: MutableList<TextComponent>) {
            footer.forEach {
                printPlain(it, commands, buffer)
            }
        }

        /**
         * Prints [command] of inheritance [level][level] to [out]. See [Printer.printTo].
         */
        fun printTo(command: Command, level: Int, out: (String) -> Unit, localize: TextLocalizer) {
            val buffer = mutableListOf<TextComponent>()
            val commands = mutableListOf<Command>()

            printHeader(commands, buffer)

            this.printTo(buffer, commands, command, level)

            printFooter(commands, buffer)

            this.flushTo(out, commands, buffer, localize)
        }

        fun printFromRoot(buffer: MutableList<TextComponent>,
                          commands: MutableList<Command>,
                          command: Command,
                          level: Int) {
            val commandsToPrint = mutableListOf<Command>()

            var parent: Command? = command.parent

            while (parent != null) {
                commandsToPrint.add(0, parent)
                parent = parent.parent
            }

            commandsToPrint += command

            commandsToPrint.forEach {
                printTo(buffer, commands, it, it.level + level)
            }
        }

        fun printTo(buffer: MutableList<TextComponent>,
                    commands: MutableList<Command>,
                    command: Command,
                    level: Int) {
            val components = mutableListOf<TextComponent>()

            components += if (level == 0)
                Text.single("->")
            else
                Text.single("-").let {
                    for (i in 0..level)
                        it.append("-".asText())
                    it.append("'>".asText())
                }

            components += Text.single(" ")
            components += Text.single(command.name)

            command.arguments.all.forEach {
                components += Text.single(" ")

                components.apply {
                    this.add(Text.single(if (it.isOptional) "<" else "["))

                    this.add(Text.single(it.name))

                    this.add(Text.single(": ")
                            .append(Text.single(
                                    if (it.argumentType.type.canResolve()) it.argumentType.type.toString()
                                    else it.argumentType.type.classLiteral)))

                    this.add(Text.single(if (it.isOptional) ">" else "]"))
                }

            }

            buffer.add(Text.of(components))
            commands += command
        }

        fun flushTo(out: (String) -> Unit, commands: List<Command>,
                    cBuffer: List<TextComponent>,
                    localize: TextLocalizer) {
            val buffer = cBuffer.map { localize.localize(it) }

            fun ((String) -> Unit).flushAndClean(builder: StringBuilder) =
                    this(builder.toString().also {
                        builder.setLength(0)
                    })

            if (commands.any { it !== DummyCommand }) {
                val maxSize = buffer.filterIndexed { index, _ -> commands[index] !== DummyCommand }
                        .maxBy(String::length)!!.length + 5

                require(commands.size == buffer.size) { "Command size and buffer size is not equal. Commands: <$commands>. Buffer: <$buffer>" }

                commands.forEachIndexed { index, command ->
                    val buff = buffer[index]
                    val remaining = maxSize - buff.length

                    if (command === DummyCommand) {
                        out(buff)
                        return@forEachIndexed // = continue
                    }

                    val builder = StringBuilder(buff)

                    val cmdDescNotBlank = command.description.isNotEmpty

                    if (cmdDescNotBlank) {
                        builder.append(' ', remaining)
                        builder.append(" - ${localize.localize(command.description)}")
                        out.flushAndClean(builder)
                    } else {
                        out.flushAndClean(builder)
                    }

                    val to = buff.indexOf(">")

                    if (command.arguments.all.any { it.description.isNotEmpty }) {
                        builder.append(' ', to + 1)
                        builder.append(localize.localize(Texts.getArgumentDescriptionText().append(Text.single(":"))))
                        out.flushAndClean(builder)

                        command.arguments.all.forEach {
                            if (it.description.isNotEmpty) {
                                builder.append(' ', to + 1)
                                builder.append(" - ${it.name}: ${localize.localize(it.description)}")
                                out.flushAndClean(builder)
                            }
                        }
                    }

                    val anyReq = command.requirements.isNotEmpty()
                            || command.arguments.all.any { it.requirements.isNotEmpty() }
                    val anyInfoReq = command.requiredInfo.isNotEmpty()
                            || command.arguments.all.any { it.requiredInfo.isNotEmpty() }

                    if (anyReq || anyInfoReq) {
                        builder.append(' ', to + 1)

                        builder.append(localize.localize(Texts.getRequirementsText().append(":".asText())))

                        out.flushAndClean(builder)

                        val requirementPrinter: (Requirement<*, *>) -> Unit = {
                            builder.setLength(0)

                            builder.append(' ', to + 3)

                            val tags=
                                    if (it.subject.tags.isNotEmpty()) it.subject.tags.joinToString(separator = " ")
                                    else "[]"
                            builder.append(localize.localize(Texts.getRequiresValueText(
                                    it.required.toString(),
                                    it.subject.type.toString(),
                                    tags,
                                    it.tester.name
                            )))

                            out.flushAndClean(builder)
                        }

                        val infoRequirementPrinter: (RequiredInformation) -> Unit = {
                            builder.setLength(0)

                            builder.append(' ', to + 3)

                            builder.append(localize.localize(Texts.getRequiresInfoText(
                                    it.id.toString(),
                                    it.id.type.toString())
                            ))
                            out.flushAndClean(builder)
                        }

                        command.arguments.all.forEach { arg ->

                            if (arg.requirements.isNotEmpty() || arg.requiredInfo.isNotEmpty()) {
                                builder.append(' ', to + 2)

                                builder.append(localize.localize(Text.of(
                                        Texts.getArgumentText(),
                                        "(",
                                        arg.name.toString(),
                                        "):"
                                )))
                                out.flushAndClean(builder)
                            }

                            if (arg.requiredInfo.isNotEmpty()) {
                                arg.requiredInfo.forEach(infoRequirementPrinter)
                            }

                            if (arg.requirements.isNotEmpty()) {
                                arg.requirements.forEach(requirementPrinter)
                            }
                        }

                        if (command.requirements.isNotEmpty() || command.requiredInfo.isNotEmpty()) {
                            builder.append(' ', to + 2)
                            builder.append(localize.localize(Texts.getCommandText().append(":".asText())))
                            out.flushAndClean(builder)
                        }

                        if (command.requiredInfo.isNotEmpty()) {
                            command.requiredInfo.forEach(infoRequirementPrinter)
                        }

                        if (command.requirements.isNotEmpty()) {
                            command.requirements.forEach(requirementPrinter)
                        }


                        out("")
                    } else {
                        out("")
                    }
                }

            } else {
                if (commands.isNotEmpty()) {
                    buffer.forEach { out(it) }
                } else {
                    out(localize.localize(Texts.getNoCommandsText()))
                }
            }
        }
    }
}