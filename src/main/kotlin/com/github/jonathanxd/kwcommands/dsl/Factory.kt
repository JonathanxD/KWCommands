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
package com.github.jonathanxd.kwcommands.dsl

import com.github.jonathanxd.iutils.kt.asText
import com.github.jonathanxd.iutils.kt.typeInfo
import com.github.jonathanxd.iutils.text.TextComponent
import com.github.jonathanxd.iutils.type.TypeInfo
import com.github.jonathanxd.kwcommands.argument.*
import com.github.jonathanxd.kwcommands.command.Command
import com.github.jonathanxd.kwcommands.command.CommandContainer
import com.github.jonathanxd.kwcommands.command.CommandContext
import com.github.jonathanxd.kwcommands.command.Handler
import com.github.jonathanxd.kwcommands.information.Information
import com.github.jonathanxd.kwcommands.information.RequiredInformation
import com.github.jonathanxd.kwcommands.information.InformationProviders
import com.github.jonathanxd.kwcommands.parser.ArgumentParser
import com.github.jonathanxd.kwcommands.parser.Input
import com.github.jonathanxd.kwcommands.parser.ListInput
import com.github.jonathanxd.kwcommands.parser.SingleInput
import com.github.jonathanxd.kwcommands.processor.ResultHandler
import com.github.jonathanxd.kwcommands.requirement.*
import com.github.jonathanxd.kwcommands.util.*

class BuildingArgument<I : Input, T> {
    var name: String = ""
    val alias = UList<String>()
    var description: TextComponent = "".asText()
    var isOptional: Boolean = false
    var isMultiple: Boolean = false
    lateinit var typeInfo: TypeInfo<out T>
    var type: ArgumentType<I, T>? = null
    var defaultValue: T? = null
    val requirements = UList<Requirement<*, *>>()
    val requiredInfo = USet<RequiredInformation>()
    var handler: ArgumentHandler<out T>? = null

    inline fun name(f: () -> String) {
        this.name = f()
    }

    inline fun description(f: () -> TextComponent) {
        this.description = f()
    }

    inline fun optional(f: () -> Boolean) {
        this.isOptional = f()
    }

    inline fun type(f: () -> TypeInfo<T>) {
        this.typeInfo = f()
    }

    inline fun argumentType(f: () -> ArgumentType<I, T>) {
        this.type = f()
    }

    inline fun multiple(f: () -> Boolean) {
        this.isMultiple = f()
    }

    inline fun defaultValue(f: () -> T?) {
        this.defaultValue = f()
    }

    inline fun alias(f: UList<String>.() -> Unit) {
        f(this.alias)
    }

    inline fun requirements(f: UList<Requirement<*, *>>.() -> Unit) {
        f(this.requirements)
    }

    inline fun requiredInfo(f: USet<RequiredInformation>.() -> Unit) {
        f(this.requiredInfo)
    }

    inline fun handler(crossinline f: (argumentContainer: ArgumentContainer<T>,
                                       commandContainer: CommandContainer,
                                       informationProviders: InformationProviders,
                                       resultHandler: ResultHandler) -> Any) {
        this.handler = object : ArgumentHandler<T> {
            override fun handle(argumentContainer: ArgumentContainer<T>,
                                commandContainer: CommandContainer,
                                informationProviders: InformationProviders,
                                resultHandler: ResultHandler): Any =
                    f(argumentContainer, commandContainer, informationProviders, resultHandler)

        }
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun toArgument(): Argument<T> = Argument(
            name = this.name,
            alias = this.alias.coll.toList(),
            description = this.description,
            isOptional = this.isOptional,
            argumentType = this.type as ArgumentType<*, T>,
            requirements = this.requirements.coll.toList(),
            requiredInfo = this.requiredInfo.coll.toSet(),
            handler = this.handler
    )
}

class BuildingRequirement<T, R>(var required: R) {
    lateinit var subject: RequirementSubject<T>
    lateinit var type: TypeInfo<out R>
    lateinit var tester: RequirementTester<T, R>

    inline fun subject(f: BuildingInfoId<T>.() -> Unit) {
        val info = BuildingInfoId<T>()
        f(info)
        subject = InformationRequirementSubject(info.toId())
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun subject(id: Information.Id<T>) = subject { from(id) }

    inline fun subjectArgument(f: () -> String) {
        subject = ArgumentRequirementSubject(f())
    }

    inline fun type(f: () -> TypeInfo<out R>) {
        this.type = f()
    }

    inline fun tester(crossinline f: (requirement: Requirement<T, R>, value: T) -> Boolean) {
        this.tester = object : RequirementTester<T, R> {
            override fun test(requirement: Requirement<T, R>, value: T): Boolean =
                    f(requirement, value)
        }
    }

    inline fun tester(testerName: TextComponent,
                      crossinline f: (requirement: Requirement<T, R>, value: T) -> Boolean) {
        this.tester = object : RequirementTester<T, R> {
            override val name: TextComponent
                get() = testerName

            override fun test(requirement: Requirement<T, R>, value: T): Boolean =
                    f(requirement, value)

        }
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun toRequirement(): Requirement<T, R> = Requirement(
            required = this.required,
            subject = this.subject,
            type = this.type,
            tester = this.tester
    )
}

class BuildingInfoId<T> {

    lateinit var type: TypeInfo<out T>
    val tags = UList<String>()

    inline fun type(f: () -> TypeInfo<out T>) {
        this.type = f()
    }

    inline fun tags(f: UList<String>.() -> Unit) =
            f(this.tags)

    @Suppress("NOTHING_TO_INLINE")
    inline fun from(id: Information.Id<T>) {
        this.type = id.type
        this.tags.clear()
        this.tags += id.tags
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun toId(): Information.Id<T> = Information.Id(this.type, this.tags.coll.toTypedArray())

}

class BuildingRequiredInfo<T> {
    val id = BuildingInfoId<T>()
    var useProviders: Boolean = true

    inline fun id(f: BuildingInfoId<T>.() -> Unit) = f(id)

    @Suppress("NOTHING_TO_INLINE")
    inline fun id(id: Information.Id<T>) = id { from(id) }

    inline fun useProviders(f: () -> Boolean) {
        this.useProviders = f()
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun toRequiredInformation(): RequiredInformation = RequiredInformation(
            id = this.id.toId(),
            useProviders = this.useProviders
    )
}

inline fun <reified T> informationId(f: BuildingInfoId<T>.() -> Unit): Information.Id<T> {
    val building = BuildingInfoId<T>()

    building.type = typeInfo<T>()

    f(building)

    return building.toId()
}

inline fun <reified I : Input, reified T> argument(f: BuildingArgument<I, T>.() -> Unit): Argument<T> {
    val building = BuildingArgument<I, T>()

    building.typeInfo = typeInfo()

    f(building)

    return building.toArgument()
}


inline fun <I : Input, T> argumentPlain(type: TypeInfo<T>, f: BuildingArgument<I, T>.() -> Unit): Argument<T> {
    val building = BuildingArgument<I, T>()

    building.typeInfo = type

    f(building)

    return building.toArgument()
}

inline fun <reified T, reified R> requirement(required: R, f: BuildingRequirement<T, R>.() -> Unit): Requirement<T, R> {
    val building = BuildingRequirement<T, R>(required)

    building.type = typeInfo()

    f(building)

    return building.toRequirement()
}

inline fun <T, R> requirementPlain(reqType: TypeInfo<R>,
                                   required: R,
                                   f: BuildingRequirement<T, R>.() -> Unit): Requirement<T, R> {
    val building = BuildingRequirement<T, R>(required)

    building.type = reqType

    f(building)

    return building.toRequirement()
}

inline fun <reified T> requireInfo(f: BuildingRequiredInfo<T>.() -> Unit): RequiredInformation {
    val building = BuildingRequiredInfo<T>()

    f(building)

    return building.toRequiredInformation()
}

inline fun <T> requireInfoPlain(f: BuildingRequiredInfo<T>.() -> Unit): RequiredInformation {
    val building = BuildingRequiredInfo<T>()

    f(building)

    return building.toRequiredInformation()
}

inline fun <T> argumentHandler(crossinline f: (argumentContainer: ArgumentContainer<T>,
                                               commandContainer: CommandContainer,
                                               informationProviders: InformationProviders,
                                               resultHandler: ResultHandler) -> Any): ArgumentHandler<T> = object : ArgumentHandler<T> {
    override fun handle(argumentContainer: ArgumentContainer<T>,
                        commandContainer: CommandContainer,
                        informationProviders: InformationProviders,
                        resultHandler: ResultHandler): Any =
            f(argumentContainer, commandContainer, informationProviders, resultHandler)

}


// Additionals

val stringParser: ArgumentParser<SingleInput, String> = StringParser

val intParser: ArgumentParser<SingleInput, Int> = IntParser

val longParser: ArgumentParser<SingleInput, Long> = LongParser

val doubleParser: ArgumentParser<SingleInput, Double> = DoubleParser

val booleanParser: ArgumentParser<SingleInput, Boolean> = BooleanParser

val booleanPossibilities = BooleanPossibilities

inline fun stringArg(f: BuildingArgument<SingleInput, String>.() -> Unit): Argument<String> =
        argument<SingleInput, String> {
            type = stringArgumentType
            f(this)
        }

inline fun intArg(f: BuildingArgument<SingleInput, Int>.() -> Unit): Argument<Int> =
        argument<SingleInput, Int> {
            type = intArgumentType
            f(this)
        }

inline fun longArg(f: BuildingArgument<SingleInput, Long>.() -> Unit): Argument<Long> =
        argument<SingleInput, Long> {
            type = longArgumentType
            f(this)
        }

inline fun doubleArg(f: BuildingArgument<SingleInput, Double>.() -> Unit): Argument<Double> =
        argument<SingleInput, Double> {
            type = doubleArgumentType
            f(this)
        }

inline fun booleanArg(f: BuildingArgument<SingleInput, Boolean>.() -> Unit): Argument<Boolean> =
        argument<SingleInput, Boolean> {
            type = booleanArgumentType
            f(this)
        }

inline fun <reified T> enumArg(f: BuildingArgument<SingleInput, T>.() -> Unit): Argument<T> =
        argument<SingleInput, T> {
            type = enumArgumentType(T::class.java)
            f(this)
        }

inline fun <reified T> listArg(base: Argument<T>): Argument<List<T>> =
        listArg(base, {})

inline fun <reified T> listArg(base: Argument<T>,
                               f: BuildingArgument<ListInput, List<T>>.() -> Unit): Argument<List<T>> =
        argument<ListInput, List<T>> {
            alias { +base.alias }
            name = base.name
            isOptional = base.isOptional
            isMultiple = true
            type = ListArgumentType(base.argumentType,
                    TypeInfo.builderOf(List::class.java).of(base.argumentType.type).buildGeneric())
            requirements { +base.requirements }
            requiredInfo { +base.requiredInfo }
            f(this)
        }

// Command

@Suppress("NOTHING_TO_INLINE")
abstract class UColl<E> {
    abstract val coll: MutableCollection<E>

    inline fun clear() {
        coll.clear()
    }

    inline operator fun Iterable<E>.unaryPlus() {
        coll += this
    }

    inline operator fun E.unaryPlus() {
        coll += this
    }

    inline operator fun E.unaryMinus() {
        coll -= this
    }

    inline operator fun minusAssign(e: E) {
        coll -= e
    }

    inline operator fun minusAssign(e: Iterable<E>) {
        coll -= e
    }

    inline operator fun minusAssign(e: Array<out E>) {
        coll -= e
    }

    inline operator fun plusAssign(e: E) {
        coll += e
    }

    inline operator fun plusAssign(e: Iterable<E>) {
        coll += e
    }

    inline operator fun plusAssign(e: Array<out E>) {
        coll += e
    }
}

class UList<E> : UColl<E>() {
    override val coll = mutableListOf<E>()

}

class USet<E> : UColl<E>() {
    override val coll = mutableSetOf<E>()
}

class BuildingCommand {
    var parent: Command? = null
    var order = 0
    lateinit var name: String
    var description: TextComponent = "".asText()
    var handler: Handler? = null
    var arguments: Arguments = StaticListArguments()
    val requirements = UList<Requirement<*, *>>()
    val requiredInfo = USet<RequiredInformation>()
    val alias = UList<String>()

    inline fun order(f: () -> Int) {
        this.order = f()
    }

    inline fun description(f: () -> TextComponent) {
        this.description = f()
    }

    inline fun arguments(f: () -> Arguments) {
        this.arguments = f()
    }

    inline fun requirements(f: UList<Requirement<*, *>>.() -> Unit) =
            f(this.requirements)

    inline fun requiredInfo(f: USet<RequiredInformation>.() -> Unit) =
            f(this.requiredInfo)


    inline fun alias(f: UList<String>.() -> Unit) =
            f(this.alias)

    inline fun handler(crossinline f: (commandContainer: CommandContainer,
                                       informationProviders: InformationProviders,
                                       resultHandler: ResultHandler) -> Any) {
        this.handler = object : Handler {
            override fun handle(commandContainer: CommandContainer,
                                informationProviders: InformationProviders,
                                resultHandler: ResultHandler): Any =
                    f(commandContainer, informationProviders, resultHandler)
        }
    }

    inline fun handlerWithContext(crossinline f: (context: CommandContext) -> Any) {
        this.handler = object : Handler {
            override fun handle(commandContainer: CommandContainer,
                                informationProviders: InformationProviders,
                                resultHandler: ResultHandler): Any =
                    f(CommandContext(commandContainer, informationProviders, resultHandler))
        }
    }

    inline fun name(f: () -> String) {
        this.name = f()
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun toCommand(): Command = Command(
            parent = this.parent,
            order = this.order,
            name = this.name,
            description = this.description,
            handler = this.handler,
            arguments = this.arguments,
            requirements = this.requirements.coll.toList(),
            requiredInfo = this.requiredInfo.coll.toSet(),
            alias = this.alias.coll.toList()
    )
}

inline fun command(f: BuildingCommand.() -> Unit): Command =
        BuildingCommand().also { f(it) }.toCommand()

inline fun handler(crossinline f: (commandContainer: CommandContainer,
                                   informationProviders: InformationProviders,
                                   resultHandler: ResultHandler) -> Any) = object : Handler {
    override fun handle(commandContainer: CommandContainer, informationProviders: InformationProviders, resultHandler: ResultHandler): Any =
            f(commandContainer, informationProviders, resultHandler)
}

inline fun staticListArguments(f: UList<Argument<*>>.() -> Unit): Arguments =
        UList<Argument<*>>().also(f).let { StaticListArguments(it.coll) }
