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
package com.github.jonathanxd.kwcommands.command


/**
 * Represents the name of a command, a name of a command can be a [regex][RegexName] pattern or plain [string][StringName].
 */
sealed class CommandName : Comparable<String> {

    override fun equals(other: Any?): Boolean {
        return if(other != null && other is CommandName) this.toString() == other.toString() else super.equals(other)
    }

    override fun hashCode(): Int {
        return this.toString().hashCode()
    }

    abstract override fun toString(): String

    /**
     * Regex pattern.
     */
    class RegexName(val regex: Regex) : CommandName() {
        override fun compareTo(other: String): Int {
            return if (other.matches(regex)) 0 else -1
        }

        override fun hashCode(): Int = this.regex.hashCode()
        override fun equals(other: Any?): Boolean =
                if (other != null && other is RegexName) this.regex == other.regex else super.equals(other)

        override fun toString(): String = this.regex.toString()

    }

    /**
     * Plain string name.
     */
    class StringName(val string: String) : CommandName() {

        override fun compareTo(other: String): Int {
            return this.string.compareTo(other)
        }

        override fun hashCode(): Int = this.string.hashCode()
        override fun equals(other: Any?): Boolean =
                if (other != null && other is StringName) this.string == other.string else super.equals(other)

        override fun toString(): String = this.string
    }

}