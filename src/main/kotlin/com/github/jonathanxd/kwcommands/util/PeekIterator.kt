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

class PeekIterator(val elements: List<String>) : ListIterator<String> {
    private var elementIndex: Int = 0

    fun peekNext(): String = this.elements[this.elementIndex]
    fun peekPrevious(): String = this.elements[this.elementIndex - 1]

    override fun hasNext(): Boolean = this.elementIndex < this.elements.size

    override fun hasPrevious(): Boolean = this.elementIndex - 1 >= 0

    override fun next(): String = this.elements[this.elementIndex].also { this.elementIndex++ } // or elements[elementIndex++]

    override fun nextIndex(): Int = this.elementIndex

    override fun previous(): String = this.elements[--this.elementIndex]

    override fun previousIndex(): Int = this.elementIndex - 1

    fun copy(): PeekIterator {
        val iter = PeekIterator(elements)
        iter.elementIndex = this.elementIndex
        return iter
    }

    fun from(peek: PeekIterator) {
        this.elementIndex = peek.elementIndex
    }
}


fun PeekIterator.charIter(): CharIterator {
    var charIter: CharIterator? = null

    val hasNextC = {
        (charIter != null && (charIter!!.hasNext() || this.hasNext())) || (charIter == null && this.hasNext())
    }
    val nextC = {
        if (charIter == null || !charIter!!.hasNext())
            charIter = this.next().iterator()

        charIter!!.next()
    }

    return object : CharIterator() {
        override fun hasNext(): Boolean = hasNextC()

        override fun nextChar(): Char = nextC()
    }
}