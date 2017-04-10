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

import com.github.jonathanxd.kwcommands.command.Command
import java.util.*


/**
 * This class adds to a queue all commands that the dependency are not resolved yet and add
 * [Command] instance to a `created command list` when the dependency is resolved. This class
 * checks and tries to add `queued commands` to `created command list` list when you get [command list][commands].
 *
 * This class is used to avoid premature dependency checking (earlier than the dependent command can be registered).
 *
 * Obs: This class uses the same strategy as WCommands.
 */
class CommandFactoryQueue {

    private val created_ = mutableListOf<Command>()
    private val createdUnmod = Collections.unmodifiableList(created_)
    private val queued_ = mutableListOf<QueuedCommand>()

    /**
     * Resolve all queued commands and gets the list of commands.
     */
    val commands: List<Command>
        get() {
            this.createAll()

            if (queued_.isNotEmpty())
                throw IllegalStateException("Missing dependency or recursive dependency found. ${queued_.map {
                    "Sub command ${it.dependencyProvider()} for command ${it.name} (in ${it.location}) is missing."
                }}")

            return createdUnmod
        }

    /**
     *
     */
    fun add(command: Command) {
        this.created_.add(command)
    }

    /**
     * Adds a command to the command queue. This does not means that the command will be added directly to the queue,
     * this method will first check dependencies using [dependencyCheck], if the dependency is satisfied (function returns true),
     * the command will be created using [factory] and added to `created command list`, if the dependency is not satisfied,
     * the command will be added to `queued commands` and will be resolved later (when requested).
     *
     * @param location           Location where the command is.
     * @param name               Name of the command.
     * @param factory            Command factory. (first argument is a immutable version of `created commands list`.
     * @param dependencyCheck    Dependency checker. (first argument is a immutable version of `created commands list`.
     * @param dependencyProvider Dependency path provider.
     */
    fun queueCommand(location: Any, name: String, factory: (createdCommands: List<Command>) -> Command, dependencyCheck: (createdCommands: List<Command>) -> Boolean, dependencyProvider: () -> String) {
        if (dependencyCheck(this.createdUnmod))
            this.created_.add(factory(this.createdUnmod))
        else
            this.queued_.add(QueuedCommand(location, name, factory, dependencyCheck, dependencyProvider))

    }

    /**
     * Resolves all `queued commands`.
     */
    private fun createAll() {
        var lastSize = -1

        while (queued_.isNotEmpty()) {
            if (lastSize == -1)
                lastSize = queued_.size
            else if (lastSize == queued_.size)
                throw IllegalStateException("Recursive dependency found. Queued commands: <${this.queued_}>. Created commands: <${this.created_}>")


            val copy = queued_.toList()

            for (it in copy) {
                if (it.create()) {
                    lastSize = queued_.size
                    break
                }
            }
        }
    }

    /**
     * Check dependencies of [QueuedCommand], if satisfied, remove from `queued command list` and add to `created command list`.
     *
     * @return True if dependencies is satisfied.
     */
    private fun QueuedCommand.create(): Boolean {
        if (this.dependencyCheck(createdUnmod)) {
            created_.add(this.factory(createdUnmod))

            if (queued_.contains(this))
                queued_.remove(this)

            return true
        }

        return false
    }

}

internal data class QueuedCommand(val location: Any, val name: String, val factory: (List<Command>) -> Command, val dependencyCheck: (List<Command>) -> Boolean, val dependencyProvider: () -> String)
