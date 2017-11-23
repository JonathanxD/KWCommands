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
package com.github.jonathanxd.kwcommands.util

import com.github.jonathanxd.kwcommands.command.Command
import com.github.jonathanxd.kwcommands.manager.CommandManager
import com.github.jonathanxd.kwcommands.printer.Printer

fun Printer.printAll(manager: CommandManager) {

    manager.createListWithCommands().forEach {
        this.printCommand(it, 0)
        it.consumeAllSubCommands { command, level ->
            this.printCommand(command, level)
        }
    }

}

fun Printer.printAll(command: Command) {
    this.printCommand(command, 0)
    command.consumeAllSubCommands { lcommand, level ->
        this.printCommand(lcommand, level)
    }

}

/**
 * Creates a string that uses `^` and `_` to point two positions provided in [range].
 *
 * Example, if you print a string `hello world`, and call `point(4..6)`, the returned string
 * will point `o` and `w':
 *
 * `hello world`
 * `    ^_^`
 *
 * Because `o` is the 6th character of string, and 8th character of string (remember that blank
 * space counts as a character):
 *
 * `h e l l o   w o r l d`
 * `0 1 2 3 4 5 6 7 8 9 10`
 */
fun point(range: ClosedRange<Int>): String {
    val builder = StringBuilder()

    repeat(range.start) { builder.append(' ') }
    builder.append('^')

    // Append only one ^ if start and end at same point
    if (range.start == range.endInclusive/* || range.start == range.endInclusive - 1*/)
        return builder.toString()

    // -1 because ^ will be appended at the end.
    val finalTimes = range.endInclusive - 1 - range.start

    if (finalTimes > 0) { // Avoid negative times. (But allows consecutive ^^, for cases like 2..3 for example)
        repeat(finalTimes) { builder.append('_') }
    }

    builder.append('^')

    return builder.toString()
}