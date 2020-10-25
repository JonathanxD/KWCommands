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
package com.github.jonathanxd.kwcommands.util

import com.github.jonathanxd.iutils.`object`.Either
import com.github.jonathanxd.iutils.kt.left
import com.github.jonathanxd.kwcommands.parser.EmptyInput

interface StatedIterator<T> : Iterator<Either<InputParseFail, T>> {
    val char: SourcedCharIterator
    val pos: Int
    fun restore(pos: Int)

    fun hasPrevious(): Boolean
    fun previous(): Either<InputParseFail, T>

    fun nextOrNull(): Either<InputParseFail, T>? = if (this.hasNext()) this.next() else null
    fun previousOrNull(): Either<InputParseFail, T>? =
        if (this.hasPrevious()) this.previous() else null
}

class ListBackedStatedIterator<T>(
    val list: List<Either<InputParseFail, T>>,
    override val char: SourcedCharIterator
) : StatedIterator<T> {
    override var pos: Int = -1

    override fun restore(pos: Int) {
        this.pos = pos
    }

    override fun previous(): Either<InputParseFail, T> = this.list[pos--]

    override fun hasPrevious(): Boolean = pos > -1

    override fun hasNext(): Boolean = this.pos + 1 < this.list.size

    override fun next(): Either<InputParseFail, T> =
        if (pos + 1 >= this.list.size)
            left(NoMoreElementsInputParseFail(EmptyInput(char.sourceString)))
        else
            this.list[++pos]

}