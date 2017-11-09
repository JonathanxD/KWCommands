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
package com.github.jonathanxd.kwcommands.parse

import com.github.jonathanxd.iutils.option.Options
import com.github.jonathanxd.kwcommands.command.CommandContainer
import com.github.jonathanxd.kwcommands.manager.CommandManager

interface CommandParser {
    /**
     * Command manager.
     */
    val commandManager: CommandManager

    /**
     * Parser options
     */
    val options: Options

    /**
     * Process command string list.
     *
     * @param stringList List of commands and its arguments. Each element of this string represents a
     * command or a argument to pass to command.
     * @param owner Owner of the command. The owner is used to lookup for the command in the [commandManager], if a
     * null owner is provided, the [commandManager] will return the first found command.
     */
    fun parse(stringList: List<String>, owner: Any?): List<CommandContainer> =
            parseWithOwnerFunction(stringList, { owner })

    /**
     * Process command string list.
     *
     * This provides a way to specify owner based on command input string (`commandName`).
     *
     * @param stringList List of commands and its arguments. Each element of this string represents a
     * command or a argument to pass to command.
     * @param ownerProvider Provider of the owner of the input command.
     * The owner is used to lookup for the command in the [commandManager], if a
     * null owner is provided, the [commandManager] will return the first found command.
     */
    fun parseWithOwnerFunction(stringList: List<String>,
                               ownerProvider: (commandName: String) -> Any?): List<CommandContainer>

}