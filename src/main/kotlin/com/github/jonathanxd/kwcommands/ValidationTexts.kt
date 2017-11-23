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
package com.github.jonathanxd.kwcommands

import com.github.jonathanxd.iutils.text.TextComponent
import com.github.jonathanxd.iutils.text.dynamic.DynamicGenerator
import com.github.jonathanxd.iutils.text.dynamic.Section

interface ValidationTextsStub {

    // Char
    @Section("message", "expected_char")
    fun expectedChar(): TextComponent

    @Section("message", "invalid_char")
    fun invalidChar(): TextComponent

    // Byte
    @Section("message", "expected_byte")
    fun expectedByte(): TextComponent

    @Section("message", "invalid_byte")
    fun invalidByte(): TextComponent

    // Short
    @Section("message", "expected_short")
    fun expectedShort(): TextComponent

    @Section("message", "invalid_short")
    fun invalidShort(): TextComponent

    // Int
    @Section("message", "expected_int")
    fun expectedInt(): TextComponent

    @Section("message", "invalid_int")
    fun invalidInt(): TextComponent

    // Long
    @Section("message", "expected_long")
    fun expectedLong(): TextComponent

    @Section("message", "invalid_long")
    fun invalidLong(): TextComponent

    // Float
    @Section("message", "expected_float")
    fun expectedFloat(): TextComponent

    @Section("message", "invalid_float")
    fun invalidFloat(): TextComponent

    // Double
    @Section("message", "expected_double")
    fun expectedDouble(): TextComponent

    @Section("message", "invalid_double")
    fun invalidDouble(): TextComponent

    // Boolean
    @Section("message", "expected_boolean")
    fun expectedBoolean(): TextComponent

    @Section("message", "invalid_boolean")
    fun invalidBoolean(): TextComponent

    // String
    @Section("message", "expected_string")
    fun expectedString(): TextComponent

    // Input List
    @Section("message", "expected_input_list")
    fun expectedInputList(): TextComponent

    // Input Map
    @Section("message", "expected_input_map")
    fun expectedInputMap(): TextComponent

    // Enum
    @Section("message", "expected_enum")
    fun expectedEnum(): TextComponent

    @Section("message", "invalid_enum")
    fun invalidEnum(): TextComponent
}

val ValidationTexts = DynamicGenerator.generate(ValidationTextsStub::class.java)