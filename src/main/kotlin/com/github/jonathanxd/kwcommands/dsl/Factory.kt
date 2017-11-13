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

import com.github.jonathanxd.iutils.type.TypeInfo
import com.github.jonathanxd.jwiutils.kt.typeInfo
import com.github.jonathanxd.kwcommands.argument.*
import com.github.jonathanxd.kwcommands.command.*
import com.github.jonathanxd.kwcommands.information.Information
import com.github.jonathanxd.kwcommands.information.RequiredInformation
import com.github.jonathanxd.kwcommands.manager.InformationManager
import com.github.jonathanxd.kwcommands.parser.PossibilitiesFunc
import com.github.jonathanxd.kwcommands.parser.Transformer
import com.github.jonathanxd.kwcommands.parser.Validator
import com.github.jonathanxd.kwcommands.processor.ResultHandler
import com.github.jonathanxd.kwcommands.util.EnumTransformer
import com.github.jonathanxd.kwcommands.util.EnumValidator
import com.github.jonathanxd.kwcommands.util.enumPossibilities
import com.github.jonathanxd.kwcommands.requirement.Requirement
import com.github.jonathanxd.kwcommands.requirement.RequirementTester
import com.github.jonathanxd.kwcommands.util.*

class BuildingArgument<T> {
    lateinit var id: Any
    var name: String = ""
    var description: String = ""
    var isOptional: Boolean = false
    var isMultiple: Boolean = false
    lateinit var type: TypeInfo<out T>
    var defaultValue: T? = null
    lateinit var validator: Validator
    lateinit var transformer: Transformer<T>
    var possibilities: PossibilitiesFunc = possibilitiesFunc { _, _ -> emptyMap() }
    val requirements = UList<Requirement<*, *>>()
    val requiredInfo = USet<RequiredInformation>()
    var handler: ArgumentHandler<out T>? = null

    inline fun id(f: () -> Any) {
        this.id = f()
    }

    inline fun name(f: () -> String) {
        this.name = f()
    }

    inline fun description(f: () -> String) {
        this.description = f()
    }

    inline fun optional(f: () -> Boolean) {
        this.isOptional = f()
    }

    inline fun type(f: () -> TypeInfo<T>) {
        this.type = f()
    }

    inline fun multiple(f: () -> Boolean) {
        this.isMultiple = f()
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun validate(crossinline validator: ValidatorAlias) {
        this.validator = validator(validator)
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun transformer(crossinline transformer: TransformerAlias<T>) {
        this.transformer = com.github.jonathanxd.kwcommands.util.transformer(transformer)
    }

    inline fun defaultValue(f: () -> T?) {
        this.defaultValue = f()
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun possibilities(crossinline possibilities: PossibilitiesFuncAlias) {
        this.possibilities = possibilitiesFunc(possibilities)
    }

    inline fun requirements(f: UList<Requirement<*, *>>.() -> Unit) {
        f(this.requirements)
    }

    inline fun requiredInfo(f: USet<RequiredInformation>.() -> Unit) {
        f(this.requiredInfo)
    }

    inline fun handler(crossinline f: (argumentContainer: ArgumentContainer<T>,
                                       commandContainer: CommandContainer,
                                       informationManager: InformationManager,
                                       resultHandler: ResultHandler) -> Any) {
        this.handler = object : ArgumentHandler<T> {
            override fun handle(argumentContainer: ArgumentContainer<T>,
                                commandContainer: CommandContainer,
                                informationManager: InformationManager,
                                resultHandler: ResultHandler): Any =
                    f(argumentContainer, commandContainer, informationManager, resultHandler)

        }
    }


    @Suppress("NOTHING_TO_INLINE")
    inline fun toArgument(): Argument<T> = Argument(
            id = this.id,
            name = this.name,
            description = this.description,
            isOptional = this.isOptional,
            type = this.type,
            isMultiple = this.isMultiple,
            defaultValue = this.defaultValue,
            validator = this.validator,
            transformer = this.transformer,
            possibilities = this.possibilities,
            requirements = this.requirements.coll.toList(),
            requiredInfo = this.requiredInfo.coll.toSet(),
            handler = this.handler
    )
}

class BuildingRequirement<T, R>(var required: R) {
    val subject = BuildingInfoId<T>()
    lateinit var type: TypeInfo<out R>
    lateinit var tester: RequirementTester<T, R>

    inline fun subject(f: BuildingInfoId<T>.() -> Unit) = f(this.subject)

    @Suppress("NOTHING_TO_INLINE")
    inline fun subject(id: Information.Id<T>) = subject { from(id) }

    inline fun type(f: () -> TypeInfo<out R>) {
        this.type = f()
    }

    inline fun tester(crossinline f: (requirement: Requirement<T, R>, information: Information<T>) -> Boolean) {
        this.tester = object : RequirementTester<T, R> {
            override fun test(requirement: Requirement<T, R>, information: Information<T>): Boolean =
                    f(requirement, information)
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun toRequirement(): Requirement<T, R> = Requirement(
            required = this.required,
            subject = this.subject.toId(),
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

inline fun <reified T> argument(f: BuildingArgument<T>.() -> Unit): Argument<T> {
    val building = BuildingArgument<T>()

    building.type = typeInfo<T>()

    f(building)

    return building.toArgument()
}


inline fun <T> argumentPlain(type: TypeInfo<T>, f: BuildingArgument<T>.() -> Unit): Argument<T> {
    val building = BuildingArgument<T>()

    building.type = type

    f(building)

    return building.toArgument()
}

inline fun <reified T, reified R> requirement(required: R, f: BuildingRequirement<T, R>.() -> Unit): Requirement<T, R> {
    val building = BuildingRequirement<T, R>(required)

    building.type = typeInfo<R>()

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
                                               informationManager: InformationManager,
                                               resultHandler: ResultHandler) -> Any): ArgumentHandler<T> = object : ArgumentHandler<T> {
    override fun handle(argumentContainer: ArgumentContainer<T>,
                        commandContainer: CommandContainer,
                        informationManager: InformationManager,
                        resultHandler: ResultHandler): Any =
            f(argumentContainer, commandContainer, informationManager, resultHandler)

}


// Additionals

val stringValidator: Validator = validator { _, _, _: String -> true }
val stringTransformer: Transformer<String> = transformer { _, _, it: String -> it }

val intValidator: Validator = validator { _, _, it: String -> it.toIntOrNull() != null }
val intTransformer: Transformer<Int> = transformer { _, _, it: String -> it.toInt() }

val longValidator: Validator = validator { _, _, it: String -> it.toLongOrNull() != null }
val longTransformer: Transformer<Long> = transformer { _, _, it: String -> it.toLong() }

val doubleValidator: Validator = validator { _, _, it: String -> it.toDoubleOrNull() != null }
val doubleTransformer: Transformer<Double> = transformer { _, _, it: String -> it.toDouble() }

val booleanValidator: Validator = validator { _, _, it: String ->
    it == "yes" || it == "no" || it == "true" || it == "false"
}
val booleanTransformer: Transformer<Boolean> = transformer { _, _, it: String ->
    when (it) {
        "yes", "true" -> true
        else -> false
    }
}
val booleanPossibilities = mapOf("" to listOf("yes", "true", "no", "false"))

inline fun stringArg(f: BuildingArgument<String>.() -> Unit): Argument<String> = argument {
    validator = stringValidator
    transformer = stringTransformer
    f(this)
}

inline fun intArg(f: BuildingArgument<Int>.() -> Unit): Argument<Int> = argument {
    validator = intValidator
    transformer = intTransformer
    f(this)
}

inline fun longArg(f: BuildingArgument<Long>.() -> Unit): Argument<Long> = argument {
    validator = longValidator
    transformer = longTransformer
    f(this)
}

inline fun doubleArg(f: BuildingArgument<Double>.() -> Unit): Argument<Double> = argument {
    validator = doubleValidator
    transformer = doubleTransformer
    f(this)
}

inline fun booleanArg(f: BuildingArgument<Boolean>.() -> Unit): Argument<Boolean> = argument {
    validator = booleanValidator
    transformer = booleanTransformer
    possibilities = possibilitiesFunc { _, _ -> booleanPossibilities.toMap() }
    f(this)
}

inline fun <reified T> enumArg(f: BuildingArgument<T>.() -> Unit): Argument<T> = argument {
    validator = EnumValidator(T::class.java)
    transformer = EnumTransformer(T::class.java)
    possibilities = possibilitiesFunc { _, _ -> enumPossibilities(T::class.java).toMap() }
    f(this)
}

@Suppress("UNCHECKED_CAST")
inline fun <T> enumArg(type: Class<T>, f: BuildingArgument<T>.() -> Unit): Argument<T> = argument<Unit> {
    this as BuildingArgument<T>
    this.type { TypeInfo.of(type) }
    validator = EnumValidator(type)
    transformer = EnumTransformer(type)
    possibilities = possibilitiesFunc { _, _ -> enumPossibilities(type).toMap() }
    f(this)
} as Argument<T>

inline fun <reified T> listArg(base: Argument<T>): Argument<List<T>> =
        listArg(base, {})

inline fun <reified T> listArg(base: Argument<T>,
                               f: BuildingArgument<List<T>>.() -> Unit): Argument<List<T>> = argument {
    id = base.id
    name = base.name
    type = TypeInfo.builderOf(List::class.java).of(base.type).buildGeneric()
    isOptional = base.isOptional
    isMultiple = true
    validator = base.validator
    transformer = ListTransformer(base.transformer)
    possibilities = base.possibilities
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

class BuildingCommandName {
    lateinit var name: CommandName

    @Suppress("NOTHING_TO_INLINE")
    inline fun string(string: String) {
        this.name = CommandName.name(string)
    }

    inline fun string(f: () -> String) {
        this.name = CommandName.name(f())
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun regexPattern(pattern: String) {
        this.name = CommandName.regex(pattern)
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun regex(regex: Regex) {
        this.name = CommandName.regex(regex)
    }

    inline fun regexPattern(f: () -> String) {
        this.name = CommandName.regex(f())
    }

    inline fun regex(f: () -> Regex) {
        this.name = CommandName.regex(f())
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun toName(): CommandName = this.name
}

class BuildingCommand {
    var parent: Command? = null
    var order = 0
    var name = BuildingCommandName()
    var description: String = ""
    var handler: Handler? = null
    val arguments = UList<Argument<*>>()
    val requirements = UList<Requirement<*, *>>()
    val requiredInfo = USet<RequiredInformation>()
    val alias = UList<CommandName>()

    inline fun order(f: () -> Int) {
        this.order = f()
    }

    inline fun description(f: () -> String) {
        this.description = f()
    }

    inline fun arguments(f: UList<Argument<*>>.() -> Unit) =
            f(this.arguments)

    inline fun requirements(f: UList<Requirement<*, *>>.() -> Unit) =
            f(this.requirements)

    inline fun requiredInfo(f: USet<RequiredInformation>.() -> Unit) =
            f(this.requiredInfo)


    inline fun alias(f: UList<CommandName>.() -> Unit) =
            f(this.alias)

    inline fun handler(crossinline f: (commandContainer: CommandContainer,
                                       informationManager: InformationManager,
                                       resultHandler: ResultHandler) -> Any) {
        this.handler = object : Handler {
            override fun handle(commandContainer: CommandContainer,
                                informationManager: InformationManager,
                                resultHandler: ResultHandler): Any =
                    f(commandContainer, informationManager, resultHandler)
        }
    }

    inline fun handlerWithContext(crossinline f: (context: CommandContext) -> Any) {
        this.handler = object : Handler {
            override fun handle(commandContainer: CommandContainer,
                                informationManager: InformationManager,
                                resultHandler: ResultHandler): Any =
                    f(CommandContext(commandContainer, informationManager, resultHandler))
        }
    }

    inline fun name(f: BuildingCommandName.() -> Unit) =
            f(this.name)

    inline fun stringName(f: () -> String) {
        this.name.string(f)
    }

    inline fun regexName(f: () -> Regex) {
        this.name.regex(f)
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun toCommand(): Command = Command(
            parent = this.parent,
            order = this.order,
            name = this.name.toName(),
            description = this.description,
            handler = this.handler,
            arguments = this.arguments.coll.toList(),
            requirements = this.requirements.coll.toList(),
            requiredInfo = this.requiredInfo.coll.toSet(),
            alias = this.alias.coll.toList()
    )
}

inline fun command(f: BuildingCommand.() -> Unit): Command =
        BuildingCommand().also { f(it) }.toCommand()

@Suppress("NOTHING_TO_INLINE")
inline fun commandName(name: String) = CommandName.StringName(name)

@Suppress("NOTHING_TO_INLINE")
inline fun commandName(regex: Regex) = CommandName.RegexName(regex)

inline fun handler(crossinline f: (commandContainer: CommandContainer,
                                   informationManager: InformationManager,
                                   resultHandler: ResultHandler) -> Any) = object : Handler {
    override fun handle(commandContainer: CommandContainer, informationManager: InformationManager, resultHandler: ResultHandler): Any =
            f(commandContainer, informationManager, resultHandler)
}

