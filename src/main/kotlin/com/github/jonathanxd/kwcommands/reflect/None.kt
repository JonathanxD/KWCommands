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
package com.github.jonathanxd.kwcommands.reflect

import com.github.jonathanxd.kwcommands.argument.Argument
import com.github.jonathanxd.kwcommands.argument.ArgumentContainer
import com.github.jonathanxd.kwcommands.argument.ArgumentHandler
import com.github.jonathanxd.kwcommands.command.CommandContainer
import com.github.jonathanxd.kwcommands.command.Handler
import com.github.jonathanxd.kwcommands.manager.InformationManager
import com.github.jonathanxd.kwcommands.parser.Input
import com.github.jonathanxd.kwcommands.processor.ResultHandler
import com.github.jonathanxd.kwcommands.parser.PossibilitiesFunc
import com.github.jonathanxd.kwcommands.parser.Transformer
import com.github.jonathanxd.kwcommands.parser.Validator


sealed class None

object NoneValidator : None(), Validator {
    override fun invoke(parsed: List<ArgumentContainer<*>>, current: Argument<*>, value: Input): Boolean =
            true
}

object NoneTransformer : None(), Transformer<Any> {
    override fun invoke(parsed: List<ArgumentContainer<*>>, current: Argument<*>, value: Input): Any =
        value
}

object NonePossibilities : None(), PossibilitiesFunc {
    override fun invoke(parsed: List<ArgumentContainer<*>>, current: Argument<*>): Map<String, List<String>> =
            emptyMap()
}

object NoneHandler : None(), Handler {
    override fun handle(commandContainer: CommandContainer, informationManager: InformationManager,
                        resultHandler: ResultHandler): Any =
            Unit
}

object NoneArgumentHandler : None(), ArgumentHandler<Any?> {
    override fun handle(argumentContainer: ArgumentContainer<Any?>, commandContainer: CommandContainer,
                        informationManager: InformationManager, resultHandler: ResultHandler): Any =
            Unit
}
