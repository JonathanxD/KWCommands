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
package com.github.jonathanxd.kwcommands.completion

import com.github.jonathanxd.kwcommands.argument.Argument
import com.github.jonathanxd.kwcommands.argument.ArgumentContainer
import com.github.jonathanxd.kwcommands.argument.ArgumentType
import com.github.jonathanxd.kwcommands.argument.ListArgumentType
import com.github.jonathanxd.kwcommands.command.Command
import com.github.jonathanxd.kwcommands.parser.Input
import com.github.jonathanxd.kwcommands.parser.ListInput
import com.github.jonathanxd.kwcommands.parser.MapInput
import com.github.jonathanxd.kwcommands.parser.SingleInput

class DefaultAutoCompleter : AutoCompleter {
    override fun completeArgumentName(command: Command,
                                      arguments: List<ArgumentContainer<*>>,
                                      completions: Completions) {
        TODO("not implemented")
    }

    override fun completeArgument(command: Command,
                                  arguments: List<ArgumentContainer<*>>,
                                  argument: Argument<*>,
                                  base: Input,
                                  toComplete: SingleInput,
                                  completions: Completions) {
        TODO("not implemented")
    }

    override fun completeArgumentInput(command: Command,
                                       arguments: List<ArgumentContainer<*>>,
                                       argument: Argument<*>,
                                       argumentType: ArgumentType<*, *>,
                                       completions: Completions) {
        val poss = argumentType.possibilities()

        if (argumentType is ListArgumentType<*>) {
            completions.add("[")
        }

        //completions.addAll(poss.map { it.getString() })
    }

    override fun completeArgumentListElement(command: Command,
                                             arguments: List<ArgumentContainer<*>>,
                                             argument: Argument<*>,
                                             argumentType: ArgumentType<*, *>,
                                             completions: Completions) {

        val poss = (argumentType.possibilities().single() as ListInput).input

        completions.addAll(poss.map { it.getString() })
    }

    override fun completeArgumentMapKey(command: Command,
                                        arguments: List<ArgumentContainer<*>>,
                                        argument: Argument<*>,
                                        keyType: ArgumentType<*, *>,
                                        completions: Completions) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun completeArgumentMapValue(command: Command,
                                          arguments: List<ArgumentContainer<*>>,
                                          argument: Argument<*>,
                                          valueType: ArgumentType<*, *>,
                                          completions: Completions) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}