/*
 *      KWCommands - New generation of WCommands written in Kotlin <https://github.com/JonathanxD/KWCommands>
 *
 *         The MIT License (MIT)
 *
 *      Copyright (c) 2018 JonathanxD
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

import com.github.jonathanxd.kwcommands.argument.ArgumentContainer
import com.github.jonathanxd.kwcommands.argument.ArgumentHandler
import com.github.jonathanxd.kwcommands.argument.ArgumentType
import com.github.jonathanxd.kwcommands.command.CommandContainer
import com.github.jonathanxd.kwcommands.command.Handler
import com.github.jonathanxd.kwcommands.information.InformationProviders
import com.github.jonathanxd.kwcommands.parser.*
import com.github.jonathanxd.kwcommands.processor.ResultHandler

sealed class None

object NoneParser : None(), ArgumentParser<Input, Any> {
    override fun parse(
        input: Input,
        valueOrValidationFactory: ValueOrValidationFactory
    ): ValueOrValidation<Any> =
        valueOrValidationFactory.value(input.toPlain())

}

object NonePossibilities : None(), Possibilities {
    override fun invoke(): List<Input> =
        emptyList()
}

object NoneHandler : None(), Handler {
    override fun handle(
        commandContainer: CommandContainer, informationProviders: InformationProviders,
        resultHandler: ResultHandler
    ): Any =
        Unit
}

object NoneArgumentHandler : None(), ArgumentHandler<Any?> {
    override fun handle(
        argumentContainer: ArgumentContainer<Any?>, commandContainer: CommandContainer,
        informationProviders: InformationProviders, resultHandler: ResultHandler
    ): Any =
        Unit
}


class NoneArgumentType : None(), () -> ArgumentType<*, *> {
    override fun invoke(): ArgumentType<*, *> {
        throw IllegalStateException("none")
    }

}
