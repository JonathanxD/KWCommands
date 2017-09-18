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
 * Command printer
 */
interface Printer {

    /**
     * Prints command to buffer.
     *
     * @param command Command to print.
     * @param level   Command inheritance level (0 for main commands).
     */
    fun printCommand(command: Command, level: Int)

    /**
     * Prints from root command to [command] to buffer.
     *
     * This will print all recursive parent commands of [command] and the [command] itself to buffer.
     *
     * @param command Command to print.
     * @param level   Base command inheritance level (0 for main commands).
     */
    fun printFromRoot(command: Command, level: Int)

    /**
     * Prints command directly to an [output][out].
     *
     * @param command Command to print.
     * @param level   Command inheritance level (0 for main commands).
     */
    fun printTo(command: Command, level: Int, out: (String) -> Unit)

    /**
     * Prints plain [text].
     */
    fun printPlain(text: String)

    /**
     * Flush buffer texts to predefined output.
     */
    fun flush()
}