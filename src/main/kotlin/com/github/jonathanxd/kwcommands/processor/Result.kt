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
package com.github.jonathanxd.kwcommands.processor

import com.github.jonathanxd.kwcommands.command.CommandContainer
import com.github.jonathanxd.kwcommands.command.Container
import com.github.jonathanxd.kwcommands.requirement.UnsatisfiedRequirement
import com.github.jonathanxd.kwcommands.util.MissingInformation

/**
 * Result of command handling.
 */
interface CommandResult {
    /**
     * Root container of command handling. (Null if [container] is the root container).
     */
    val rootContainer: Container?

    /**
     * Current container.
     */
    val container: Container
}

/**
 * When [container] returns an object value.
 */
data class ValueResult(
    val value: Any?,
    override val rootContainer: Container?,
    override val container: Container
) : CommandResult

/**
 * When requirement processor reports missing requirements.
 */
data class UnsatisfiedRequirementsResult(
    val unsatisfiedRequirements: List<UnsatisfiedRequirement<*>>,
    override val rootContainer: Container?,
    override val container: Container
) : CommandResult

/**
 * When [container] reports missing information list.
 */
data class MissingInformationResult(
    val missingInformationList: List<MissingInformation>,
    val requester: Any,
    override val rootContainer: Container?,
    override val container: Container
) : CommandResult

/**
 * A particular result handler which allows command handler to add more [CommandResults][CommandResult]
 * during command handling phase.
 */
interface ResultHandler {

    /**
     * Reports missing information. If called by an argument handler, the command will not be handled.
     *
     * @param missingInformationList List of missing information.
     * @param requester Instance which requested information.
     * @param cancel True if the command execution should be cancelled. Obs: Only for argument handlers, this
     * does not have effects in command handlers.
     */
    fun informationMissing(
        missingInformationList: List<MissingInformation>,
        requester: Any,
        cancel: Boolean
    )

    /**
     * Adds a [ValueResult] to the result list.
     */
    fun result(value: Any?)

    /**
     * Returns true if any handler request cancellation of command execution.
     */
    fun shouldCancel(): Boolean
}

/**
 * Gets command from result.
 */
fun CommandResult.getCommand(): CommandContainer? =
    this.rootContainer as? CommandContainer
            ?: this.container as? CommandContainer
