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

import com.github.jonathanxd.kwcommands.command.Command
import com.github.jonathanxd.kwcommands.command.CommandName
import com.github.jonathanxd.kwcommands.dsl.command
import com.github.jonathanxd.kwcommands.information.RequiredInformation
import com.github.jonathanxd.kwcommands.requirement.Requirement
import com.github.jonathanxd.kwcommands.util.append
import com.github.jonathanxd.kwcommands.util.level
import com.github.jonathanxd.kwcommands.util.nameOrId

/**
 * Common implementation of command printer backing to a print function
 */
class CommonPrinter(val out: (String) -> Unit,
                    val printHeaderAndFooter: Boolean = true) : Printer {

    private val buffer = mutableListOf<String>()
    private val commands = mutableListOf<Command>()

    init {
        if(printHeaderAndFooter) Companion.printHeader(commands, buffer)
    }

    override fun printCommand(command: Command, level: Int) {
        Companion.printTo(buffer, commands, command, level)
    }

    override fun printFromRoot(command: Command, level: Int) {
        Companion.printFromRoot(buffer, commands, command, level)
    }

    override fun printTo(command: Command, level: Int, out: (String) -> Unit) {
        val buffer = mutableListOf<String>()
        val commands = mutableListOf<Command>()

        if(printHeaderAndFooter) Companion.printHeader(commands, buffer)

        Companion.printTo(buffer, commands, command, level)

        if(printHeaderAndFooter) Companion.printFooter(commands, buffer)

        Companion.flushTo(out, commands, buffer)
    }

    override fun printPlain(text: String) {
        Companion.printPlain(text, this.commands, this.buffer)
    }

    override fun flush() {
        if(printHeaderAndFooter) Companion.printFooter(commands, buffer)

        Companion.flushTo(this.out, this.commands, this.buffer)
        this.commands.clear()
        this.buffer.clear()

        if(printHeaderAndFooter) Companion.printHeader(commands, buffer)
    }

    companion object {
        private val DummyCommand = command {
            name { string { "dummy" } }
            order = -1
        }

        private val header = listOf(
                "-------- Commands --------",
                "",
                "--------   Label  --------",
                " [ ]  = Required",
                " < >  = Optional",
                "  ?   = Optional",
                "  ->  = Main command",
                " -'>  = Sub command",
                "  -   = Description",
                " - x: = Description of argument x",
                "  *   = Information Requirement",
                "  **  = Requirement",
                "--------   Label  --------",
                "")

        private val footer = listOf(
                "",
                "-------- Commands --------"
        )

        fun printPlain(text: String,
                       commands: MutableList<Command>, buffer: MutableList<String>) {
            commands += DummyCommand
            buffer += text
        }

        fun printHeader(commands: MutableList<Command>, buffer: MutableList<String>) {
            header.forEach {
                printPlain(it, commands, buffer)
            }
        }

        fun printFooter(commands: MutableList<Command>, buffer: MutableList<String>) {
            footer.forEach {
                printPlain(it, commands, buffer)
            }
        }

        /**
         * Prints [command] of inheritance [level][level] to [out]. See [Printer.printTo].
         */
        fun printTo(command: Command, level: Int, out: (String) -> Unit) {
            val buffer = mutableListOf<String>()
            val commands = mutableListOf<Command>()

            printHeader(commands, buffer)

            this.printTo(buffer, commands, command, level)

            printFooter(commands, buffer)

            this.flushTo(out, commands, buffer)
        }

        fun printFromRoot(buffer: MutableList<String>,
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

        fun printTo(buffer: MutableList<String>,
                    commands: MutableList<Command>,
                    command: Command,
                    level: Int) {
            val builder = StringBuilder()

            if (level == 0)
                builder.append("->")
            else
                builder.append("-").let {
                    for (i in 0..level)
                        it.append("-")
                    it.append("'>")
                }

            builder.append(" ")

            builder.append(command.name)

            command.arguments.forEach {
                builder.append(" ")

                builder.apply {
                    this.append(if (it.isOptional) "<" else "[")

                    this.append(it.nameOrId)

                    this.append(": ").append(if (it.type.canResolve()) it.type.toString() else it.type.classLiteral)

                    this.append(if (it.isOptional) ">" else "]")
                }

            }

            buffer += builder.toString()
            commands += command
        }

        fun flushTo(out: (String) -> Unit, commands: List<Command>, buffer: List<String>) {
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

                    val cmdDescNotBlank = command.description.isNotBlank()

                    if (cmdDescNotBlank) {
                        builder.append(' ', remaining)
                        builder.append(" - ${command.description}")
                        out.flushAndClean(builder)
                    } else {
                        out.flushAndClean(builder)
                    }

                    val to = buff.indexOf(">")

                    if (command.arguments.any { it.description.isNotBlank() }) {
                        builder.append(' ', to + 1)
                        builder.append("Arguments description:")
                        out.flushAndClean(builder)

                        command.arguments.forEach {
                            if (it.description.isNotBlank()) {
                                builder.append(' ', to + 1)
                                builder.append(" - ${it.nameOrId}: ${it.description}")
                                out.flushAndClean(builder)
                            }
                        }
                    }

                    val anyReq = command.requirements.isNotEmpty() || command.arguments.any { it.requirements.isNotEmpty() }
                    val anyInfoReq = command.requiredInfo.isNotEmpty() || command.arguments.any { it.requiredInfo.isNotEmpty() }

                    if (anyReq || anyInfoReq) {
                        builder.append(' ', to + 1)

                        builder.append("Requirements:")

                        out.flushAndClean(builder)

                        val requirementPrinter: (Requirement<*, *>) -> Unit = {
                            builder.setLength(0)

                            builder.append(' ', to + 3)

                            builder.append("** Requires value '${it.required}' of subject '${it.subject.type}' (tags: ${it.subject.tags.joinToString(separator = " ")}) (Tester: ${it.tester.javaClass.simpleName})")
                            out.flushAndClean(builder)
                        }

                        val infoRequirementPrinter: (RequiredInformation) -> Unit = {
                            builder.setLength(0)

                            builder.append(' ', to + 3)

                            builder.append("* Requires information '${it.id}' of type '${it.id.type}'.")
                            out.flushAndClean(builder)
                        }

                        command.arguments.forEach { arg ->

                            if (arg.requirements.isNotEmpty() || arg.requiredInfo.isNotEmpty()) {
                                builder.append(' ', to + 2)
                                builder.append("Argument(${arg.id}):")
                                out.flushAndClean(builder)
                            }

                            if (arg.requiredInfo.isNotEmpty()) {
                                arg.requiredInfo.forEach(infoRequirementPrinter)
                            }

                            if(arg.requirements.isNotEmpty()) {
                                arg.requirements.forEach(requirementPrinter)
                            }
                        }

                        if (command.requirements.isNotEmpty() || command.requiredInfo.isNotEmpty()) {
                            builder.append(' ', to + 2)
                            builder.append("Command:")
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
                    out("No commands")
                }
            }
        }
    }
}