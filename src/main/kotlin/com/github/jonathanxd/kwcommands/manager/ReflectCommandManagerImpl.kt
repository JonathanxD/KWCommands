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
package com.github.jonathanxd.kwcommands.manager

import com.github.jonathanxd.iutils.type.TypeInfo
import com.github.jonathanxd.kwcommands.argument.ArgumentType
import com.github.jonathanxd.kwcommands.command.Command
import com.github.jonathanxd.kwcommands.reflect.env.ArgumentTypeProvider
import com.github.jonathanxd.kwcommands.reflect.env.ArgumentTypeStorage
import com.github.jonathanxd.kwcommands.reflect.env.ReflectionEnvironment

/**
 * Implementation of [ReflectCommandManager] which delegates calls to [manager] and [environment].
 */
class ReflectCommandManagerImpl(val manager: CommandManager = CommandManagerImpl()) :
    ReflectCommandManager {

    override val registeredCommands: Set<Command>
        get() = this.manager.registeredCommands

    override val commandsWithOwner: Set<Pair<Command, Any>>
        get() = this.manager.commandsWithOwner

    private val environment: ReflectionEnvironment = ReflectionEnvironment(this)
    override val argumentTypeStorage: ArgumentTypeStorage = object : ArgumentTypeStorage {

        override fun registerProvider(argumentTypeProvider: ArgumentTypeProvider): Boolean {
            return environment.registerProvider(argumentTypeProvider)
        }

        override fun <T> getArgumentTypeOrNull(type: TypeInfo<T>): ArgumentType<*, T>? {
            return environment.getArgumentTypeOrNull(type)
        }

        override fun <T> getArgumentType(type: TypeInfo<T>): ArgumentType<*, T> {
            return environment.getArgumentType(type)
        }

    }

    override fun <T> registerClass(klass: Class<T>, instance: T, owner: Any): Boolean {
        val commands = environment.fromClass(
            klass, instanceProvider { if (it == klass) instance else null },
            owner, false
        )
        return this.environment.registerCommands(commands, owner)
    }

    override fun <T> registerClassWithInner(
        klass: Class<T>,
        instanceProvider: InstanceProvider,
        owner: Any
    ): Boolean {
        val commands = environment.fromClass(klass, instanceProvider, owner, true)
        return this.environment.registerCommands(commands, owner)
    }

    override fun <T> unregisterClass(klass: Class<T>, owner: Any?): Boolean {
        val allStatic = environment.fromClass(klass, instanceProvider { null }, owner).all {
            this.manager.unregisterCommand(it, owner)
        }

        return allStatic && environment.fromClass(klass, instanceProvider { Unit }, owner).all {
            this.manager.unregisterCommand(it, owner)
        }
    }

    override fun registerCommand(command: Command, owner: Any): Boolean {
        return this.manager.registerCommand(command, owner)
    }

    override fun unregisterCommand(command: Command, owner: Any?): Boolean {
        return this.manager.unregisterCommand(command, owner)
    }

    override fun unregisterAllCommandsOfOwner(owner: Any): Boolean {
        return this.manager.unregisterAllCommandsOfOwner(owner)
    }

    override fun isRegistered(command: Command, owner: Any?): Boolean {
        return this.manager.isRegistered(command, owner)
    }

    override fun findCommand(name: String, owner: Any?): Command? {
        return this.manager.findCommand(name, owner)
    }

    override fun getCommand(name: String, owner: Any?): Command? {
        return this.manager.getCommand(name, owner)
    }

    override fun getCommand(path: Array<String>, owner: Any?): Command {
        return this.manager.getCommand(path, owner)
    }

    override fun getOptionalCommand(path: Array<String>, owner: Any?): Command? {
        return this.manager.getOptionalCommand(path, owner)
    }

    override fun getOwners(command: Command): Set<Any> {
        return this.manager.getOwners(command)
    }

    override fun getSubCommand(command: Command, name: String): Command? {
        return this.manager.getSubCommand(command, name)
    }

    override fun createCommandsPair(): List<Pair<Command, Any>> {
        return this.manager.createCommandsPair()
    }

    override fun createListWithCommands(): List<Command> {
        return this.manager.createListWithCommands()
    }

    override fun createListWithAllCommands(): List<Command> {
        return this.manager.createListWithAllCommands()
    }

}