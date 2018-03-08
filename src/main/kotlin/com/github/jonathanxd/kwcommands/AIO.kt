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
package com.github.jonathanxd.kwcommands

import com.github.jonathanxd.kwcommands.command.Command
import com.github.jonathanxd.kwcommands.completion.CompletionImpl
import com.github.jonathanxd.kwcommands.dispatch.CommandDispatcherImpl
import com.github.jonathanxd.kwcommands.help.CommonHelpInfoHandler
import com.github.jonathanxd.kwcommands.information.InformationProviders
import com.github.jonathanxd.kwcommands.manager.CommandManagerImpl
import com.github.jonathanxd.kwcommands.manager.InstanceProvider
import com.github.jonathanxd.kwcommands.manager.instanceProvider
import com.github.jonathanxd.kwcommands.parser.CommandParserImpl
import com.github.jonathanxd.kwcommands.processor.Processors
import com.github.jonathanxd.kwcommands.reflect.CommandFactoryQueue
import com.github.jonathanxd.kwcommands.reflect.env.ReflectionEnvironment

/**
 * All-in-one: CommandManager, Parser, Dispatcher, Processor and Environment
 *
 * @property owner Owner to be used to register command.
 */
class AIO(val owner: Any) {
    val commandManager = CommandManagerImpl()
    val parser = CommandParserImpl(commandManager)
    val dispatcher = CommandDispatcherImpl(commandManager)
    val processor = Processors.createCommonProcessor(commandManager, parser, dispatcher)
    val reflectionEnvironment = ReflectionEnvironment(commandManager)
    val completion = CompletionImpl(parser)
    val help = CommonHelpInfoHandler()

    /**
     * Stores commands to be loaded later, this allows the parent command resolution
     * to be runed later, allowing sub-commands to reside in different classes from parent commands.
     */
    private val queue = CommandFactoryQueue()

    /**
     * Parse commands of [obj] class and load them on [queue]. Loaded commands should be
     * registered later with [registerLoaded].
     */
    fun loadObj(obj: Any): AIO {
        this.reflectionEnvironment.fromClass(
            obj::class.java,
            instanceProvider { obj },
            owner,
            queue,
            false
        )
        return this
    }

    /**
     * Parse commands of [obj] class and inner classes and load them on [queue].
     * Loaded commands should be registered later with [registerLoaded].
     */
    fun loadObjWithInner(obj: Any, ownerResolver: InstanceProvider): AIO {
        this.reflectionEnvironment.fromClass(obj::class.java, ownerResolver, owner, queue, true)
        return this
    }

    /**
     * Register commands loaded in [queue].
     */
    fun registerLoaded(): AIO {
        val commands = this.queue.commands
        this.commandManager.registerAll(commands, this.owner)
        this.queue.clear()
        return this
    }

    /**
     * Register command for this [owner].
     */
    fun registerCommand(command: Command): AIO {
        this.commandManager.registerCommand(command, this.owner)
        return this
    }

    /**
     * Complete commands of this [owner].
     */
    fun complete(commandString: String, informationProviders: InformationProviders) =
        this.completion.complete(commandString, this.owner, informationProviders)

    /**
     * Parse and dispatch commands of this [owner].
     */
    fun parseAndDispatch(commandString: String, informationProviders: InformationProviders) =
        this.processor.parseAndDispatch(commandString, owner, informationProviders)
}