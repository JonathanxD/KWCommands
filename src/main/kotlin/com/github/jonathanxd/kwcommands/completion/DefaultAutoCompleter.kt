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
import com.github.jonathanxd.kwcommands.command.Command
import com.github.jonathanxd.kwcommands.parser.Input
import com.github.jonathanxd.kwcommands.parser.MapInput
import com.github.jonathanxd.kwcommands.parser.SingleInput
import com.github.jonathanxd.kwcommands.util.nameOrId

class DefaultAutoCompleter : AutoCompleter {
    override fun completeArgumentName(command: Command,
                                      arguments: List<ArgumentContainer<*>>,
                                      completions: Completions) {
        val suggestions =
                command.arguments
                        .filterNot { a -> arguments.any { it.argument == a } }
                        .map { it.nameOrId }

        completions.addAll(suggestions)
    }

    override fun completeArgument(command: Command,
                                  arguments: List<ArgumentContainer<*>>,
                                  argument: Argument<*>,
                                  base: Input,
                                  toComplete: SingleInput,
                                  completions: Completions) {
        val suggestions =
                argument.possibilities(arguments, argument)
                        .map(Input::getString)

        completions.addAll(suggestions)
    }

    override fun completeArgumentInput(command: Command,
                                       arguments: List<ArgumentContainer<*>>,
                                       argument: Argument<*>,
                                       base: Input,
                                       toComplete: Input,
                                       completions: Completions) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun completeArgumentMap(command: Command,
                                     arguments: List<ArgumentContainer<*>>,
                                     argument: Argument<*>,
                                     base: Input,
                                     toCompleteMap: MapInput,
                                     key: Input?,
                                     completions: Completions) {

        val suggestions =
                argument.possibilities(arguments, argument)

        if (base == toCompleteMap) {

            if (key != null) {
                filter.filtered.forEach {
                    if (it is MapInput) {
                        it.input.forEach { (k, v) ->
                            if (key.getString() == k.getString())
                                completions.add(v.getString())
                        }
                    }
                }
            } else {
                filter.filtered.forEach {
                    if (it is MapInput) {
                        it.input.forEach { (it, _) ->
                            completions.add(it.getString())
                        }
                    }
                }
            }
        } else {

            TODO()
        }

    }


}