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
import com.github.jonathanxd.kwcommands.information.RequiredInformation
import com.github.jonathanxd.kwcommands.requirement.Requirement
import com.github.jonathanxd.kwcommands.util.append

/**
 * Common implementation of command printer backing to a print function
 */
class CommonPrinter(val out: (String) -> Unit) : Printer {

    private val buffer = mutableListOf<String>()
    private val commands = mutableListOf<Command>()

    override fun printCommand(command: Command, level: Int) {
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
                this.append(it.id.toString())

                this.append(": ").append(if (it.type.canResolve()) it.type.toString() else it.type.classLiteral)

                this.append(if (it.isOptional) ">" else "]")
            }

        }

        buffer += builder.toString()
        commands += command
    }

    override fun flush() {

        header.forEach {
            this.out(it)
        }

        if (buffer.isNotEmpty()) {
            val maxSize = buffer.maxBy(String::length)!!.length + 5

            require(commands.size == buffer.size) { "Command size and buffer size is not equal. Commands: <$commands>. Buffer: <$buffer>" }

            commands.forEachIndexed { index, command ->
                val buff = buffer[index]
                val remaining = maxSize - buff.length

                val builder = StringBuilder(buff)

                builder.append(' ', remaining)

                if (command.description.isNotBlank())
                    builder.append(" - ${command.description}")

                this.out(builder.toString())

                val anyReq = command.requirements.isNotEmpty() || command.arguments.any { it.requirements.isNotEmpty() }
                val anyInfoReq = command.requiredInfo.isNotEmpty() || command.arguments.any { it.requiredInfo.isNotEmpty() }

                if (anyReq || anyInfoReq) {
                    builder.setLength(0)

                    val to = buff.indexOf(">")

                    builder.append(' ', to + 1)

                    builder.append("Requirements:")

                    this.out(builder.toString())

                    builder.setLength(0)

                    val requirementPrinter: (Requirement<*, *>) -> Unit = {
                        builder.setLength(0)

                        builder.append(' ', to + 3)

                        builder.append("** Requires value '${it.required}' of subject '${it.subject.id.simpleName}' (tags: ${it.subject.tags.joinToString(separator = " ")}) (Tester: ${it.tester.javaClass.simpleName})")
                        this.out(builder.toString())
                        builder.setLength(0)

                    }

                    val infoRequirementPrinter: (RequiredInformation) -> Unit = {
                        builder.setLength(0)

                        builder.append(' ', to + 3)

                        builder.append("* Requires information '${it.id}' of type '${it.type}'.")
                        this.out(builder.toString())
                        builder.setLength(0)
                    }

                    command.arguments.forEach { arg ->

                        if (arg.requirements.isNotEmpty() || arg.requiredInfo.isNotEmpty()) {
                            builder.append(' ', to + 2)
                            builder.append("Argument(${arg.id}):")
                            this.out(builder.toString())
                            builder.setLength(0)
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
                        this.out(builder.toString())
                        builder.setLength(0)
                    }

                    if (command.requiredInfo.isNotEmpty()) {
                        command.requiredInfo.forEach(infoRequirementPrinter)
                    }

                    if (command.requirements.isNotEmpty()) {
                        command.requirements.forEach(requirementPrinter)
                    }


                    this.out("")

                }

            }


        } else {
            this.out("No commands")
        }

        footer.forEach {
            this.out(it)
        }

        this.buffer.clear()
        this.commands.clear()
    }

    companion object {
        internal val header = listOf(
                "-------- Commands --------",
                "",
                "--------   Label  --------",
                " [ ] = Required",
                " < > = Optional",
                "  ?  = Optional",
                "  -> = Main command",
                " -'> = Sub command",
                "  -  = Description",
                "  *  = Information Requirement",
                "  ** = Requirement",
                "--------   Label  --------",
                "")

        internal val footer = listOf(
                "",
                "-------- Commands --------"
        )
    }
}