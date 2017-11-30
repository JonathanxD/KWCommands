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

interface SourcedIterator {
    val sourceString: String

    /**
     * Index of element of next() invocation (not next element, but index of the
     * element returned by the next invocation)
     */
    val sourceIndex: Int
}

interface SourcedCharIterator : SourcedIterator, Iterator<Char> {
    fun from(sourcedCharIterator: SourcedCharIterator)
    fun copy(): SourcedCharIterator

    fun hasPrevious(): Boolean
    fun previous(): Char

    /**
     * Runs [func] and restore iterator state to state before the invocation of [func]
     */
    fun <R> runAndRestore(func: () -> R): R

    /**
     * Runs [func] and return modified iterator instance
     */
    fun <R> runInNew(func: SourcedCharIterator.() -> R): Pair<R, SourcedCharIterator>

}


fun String.sourcedCharIterator(): SourcedCharIterator = IndexedSourcedCharIter(this)

class IndexedSourcedCharIter(val input: String) : CharIterator(), SourcedCharIterator {

    override val sourceString: String
        get() = this.input
    override val sourceIndex: Int
        get() = if (this.charIndex == 0) throw NoSuchElementException("Calling index before next() invocation")
        else this.charIndex - 1

    private var charIndex = 0

    override fun hasNext(): Boolean = charIndex < input.length

    override fun nextChar(): Char = input[charIndex++]

    override fun hasPrevious(): Boolean = charIndex > 0

    override fun previous(): Char = input[--charIndex]

    override fun copy(): SourcedCharIterator {
        val new = IndexedSourcedCharIter(input)
        new.charIndex = this.charIndex
        return new
    }

    override fun from(sourcedCharIterator: SourcedCharIterator) {
        this.charIndex = sourcedCharIterator.sourceIndex + 1
    }

    override fun <R> runAndRestore(func: () -> R): R {
        val oldIndex = this.charIndex
        val f = func()
        this.charIndex = oldIndex
        return f
    }

    override fun <R> runInNew(func: SourcedCharIterator.() -> R): Pair<R, SourcedCharIterator> {
        val cp = this.copy()
        val r = func(cp)
        return r to cp
    }

}
