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

                this.append(": ").append(if (it.type.canResolve()) it.type.typeClass.simpleName else it.type.classLiteral)

                this.append(if (it.isOptional) ">" else "]")
            }

            command.requirements.let {
                if (it.isNotEmpty()) {
                    builder.append("  ** ")

                    it.iterator().let { iterator ->
                        while (iterator.hasNext()) {
                            @Suppress("NAME_SHADOWING")
                            val it = iterator.next()
                            builder.append("[Requires value ${it.required} of subject ${it.subject.id.simpleName} (tags: ${it.subject.tags.joinToString(separator = " ")}) (Tester: ${it.tester.javaClass.simpleName})]")

                            if (iterator.hasNext())
                                builder.append(", ")
                        }
                    }


                    builder.append(" **")
                }
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

            commands.forEachIndexed { index, (_, _, _, description) ->
                val buff = buffer[index]

                val remaining = maxSize - buff.length

                val builder = StringBuilder(buff)

                for (i in 0..remaining)
                    builder.append(' ')

                builder.append(" - $description")

                this.out(builder.toString())
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
                "  ** = Requirements",
                "--------   Label  --------",
                "")

        internal val footer = listOf(
                "",
                "-------- Commands --------"
        )
    }
}