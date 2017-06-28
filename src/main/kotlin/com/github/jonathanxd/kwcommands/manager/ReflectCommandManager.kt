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
package com.github.jonathanxd.kwcommands.manager

import com.github.jonathanxd.kwcommands.reflect.env.ArgumentTypeStorage
import com.github.jonathanxd.kwcommands.reflect.env.ReflectionEnvironment

/**
 * A Command Manager which supports registration of class commands (reflection).
 *
 * This manager is intended to help with registration of `class` commands, implementation normally wraps
 * an [ReflectionEnvironment] and delegate calls to it.
 */
interface ReflectCommandManager : CommandManager {

    /**
     * Storage of argument type for conversion.
     */
    val argumentTypeStorage: ArgumentTypeStorage

    /**
     * Registers commands in [klass] for [owner] and returns true if commands was registered with success.
     *
     * This function does not works for inner classes, for inner classes uses: [registerClassWithInner]
     */
    fun <T> registerClass(klass: Class<T>, instance: T, owner: Any): Boolean

    /**
     * Registers commands in [klass] including its inner classes for [owner] and returns true if commands was
     * registered with success.
     */
    fun <T> registerClassWithInner(klass: Class<T>, instanceProvider: InstanceProvider, owner: Any): Boolean

    /**
     * Unregisters commands in [klass] of [owner] and returns true if command was removed with success.
     */
    fun <T> unregisterClass(klass: Class<T>, owner: Any?): Boolean

}

/**
 * Provider of instances.
 */
interface InstanceProvider {

    /**
     * Gets provided instance for [type].
     */
    fun <T> get(type: Class<T>): T
}