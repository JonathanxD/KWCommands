/*
 *      KWCommands - New generation of WCommands written in Kotlin <https://github.com/JonathanxD/KWCommands>
 *
 *         The MIT License (MIT)
 *
 *      Copyright (c) 2020 JonathanxD
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
package com.github.jonathanxd.kwcommands.fail

import com.github.jonathanxd.kwcommands.argument.Argument
import com.github.jonathanxd.kwcommands.argument.ArgumentContainer
import com.github.jonathanxd.kwcommands.command.Command
import com.github.jonathanxd.kwcommands.command.CommandContainer
import com.github.jonathanxd.kwcommands.manager.CommandManager
import com.github.jonathanxd.kwcommands.parser.Input
import com.github.jonathanxd.kwcommands.parser.Validation
import com.github.jonathanxd.kwcommands.util.InputParseFail
import com.github.jonathanxd.kwcommands.util.StatedIterator

class InvalidInputForArgumentFail(
    val command: Command,
    val parsedArgs: List<ArgumentContainer<*>>,
    input: Input,
    val arg: Argument<*>,
    val validation: Validation,
    parsedCommands: List<CommandContainer>,
    manager: CommandManager,
    iter: StatedIterator<Input>
) : InputedParseFail(parsedCommands, manager, input, iter)

class ArgumentInputParseFail(
    val command: Command,
    val parsedArgs: List<ArgumentContainer<*>>,
    val arg: Argument<*>,
    val inputParseFail: InputParseFail,
    parsedCommands: List<CommandContainer>,
    manager: CommandManager,
    iter: StatedIterator<Input>
) : InputedParseFail(parsedCommands, manager, inputParseFail.input, iter)

class IncompatibleInputTypesForShortArgumentsFail(
    val command: Command,
    val parsedArgs: List<ArgumentContainer<*>>,
    val expectedArg: Argument<*>,
    val incompatibleArg: Argument<*>,
    input: Input,
    parsedCommands: List<CommandContainer>,
    manager: CommandManager,
    iter: StatedIterator<Input>
) : InputedParseFail(parsedCommands, manager, input, iter)