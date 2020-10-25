/*
 *      KWCommands - New generation of WCommands written in Kotlin <https://github.com/JonathanxD/KWCommands>
 *
 *         The MIT License (MIT)
 *
 *      Copyright (c) 2020 JonathanxD
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
package com.github.jonathanxd.kwcommands.test.reflect

import com.github.jonathanxd.iutils.kt.typeInfo
import com.github.jonathanxd.iutils.type.TypeInfo
import com.github.jonathanxd.kwcommands.argument.ArgumentType
import com.github.jonathanxd.kwcommands.command.CommandContainer
import com.github.jonathanxd.kwcommands.dsl.informationId
import com.github.jonathanxd.kwcommands.information.Information
import com.github.jonathanxd.kwcommands.information.InformationProvidersImpl
import com.github.jonathanxd.kwcommands.manager.*
import com.github.jonathanxd.kwcommands.parser.SingleInput
import com.github.jonathanxd.kwcommands.printer.CommonPrinter
import com.github.jonathanxd.kwcommands.processor.Processors
import com.github.jonathanxd.kwcommands.processor.UnsatisfiedRequirementsResult
import com.github.jonathanxd.kwcommands.reflect.annotation.*
import com.github.jonathanxd.kwcommands.reflect.env.ArgumentTypeProvider
import com.github.jonathanxd.kwcommands.reflect.env.ArgumentTypeStorage
import com.github.jonathanxd.kwcommands.reflect.env.ReflectionEnvironment
import com.github.jonathanxd.kwcommands.reflect.env.cast
import com.github.jonathanxd.kwcommands.requirement.Reason
import com.github.jonathanxd.kwcommands.requirement.Requirement
import com.github.jonathanxd.kwcommands.requirement.RequirementTester
import com.github.jonathanxd.kwcommands.test.assertAll
import com.github.jonathanxd.kwcommands.test.assertWithAsserter
import com.github.jonathanxd.kwcommands.util.*
import org.junit.Assert
import org.junit.Test
import java.util.function.Supplier

class ReflectionTest {

    val simplePlayerArgumentType = simpleArgumentType(
            argumentParser<SingleInput, SimplePlayer> { value, valueOrValidationFactory ->
                valueOrValidationFactory.value(SimplePlayer(value.input))
            },
            EmptyPossibilitesFunc,
            null,
            typeInfo()
    )

    @Test
    fun test() {
        val information = InformationProvidersImpl()

        information.registerInformation(informationId { tags { +"player" } }, Player)

        val manager = CommandManagerImpl()
        val env = ReflectionEnvironment(manager)
        env.registerCommands(env.fromClass(Download::class, instanceProvider { it.newInstance() }, this), this)

        val printer = CommonPrinter(KLocale.localizer, ::println)

        printer.printAll(manager)
        printer.flush()

        val processor = Processors.createCommonProcessor(manager)

        val result =
                processor.parseAndDispatch("download https://askdsal.0/x.file 10", this, information) // ?

        Assert.assertTrue(result.isRight)
        Assert.assertTrue(result.right.any { it is UnsatisfiedRequirementsResult })
    }

    @Test
    fun testMissInfo() {

        val manager = CommandManagerImpl()
        val env = ReflectionEnvironment(manager)
        env.registerCommands(env.fromClass(Download::class, instanceProvider { it.newInstance() },
                this), this)

        val printer = CommonPrinter(KLocale.localizer, ::println)

        printer.printAll(manager)
        printer.flush()

        val processor = Processors.createCommonProcessor(manager)

        val result =
                processor.parseAndDispatch("download https://askdsal.0/x.file 10", this) // ?

        Assert.assertTrue(result.isRight)
        Assert.assertTrue(result.right.any {
            it is UnsatisfiedRequirementsResult
                    && it.unsatisfiedRequirements.isNotEmpty()
                    && it.unsatisfiedRequirements.first().reason == Reason.MISSING_INFORMATION
        })
    }

    @Test
    fun game() {
        val information = InformationProvidersImpl()

        information.registerInformation(informationId { tags { +"player" } }, Player)

        val manager = CommandManagerImpl()
        val env = ReflectionEnvironment(manager)

        ReflectionEnvironment.registerGlobal(object : ArgumentTypeProvider {
            override fun <T> provide(type: TypeInfo<T>, storage: ArgumentTypeStorage): ArgumentType<*, T>? {
                if (type == TypeInfo.of(SimplePlayer::class.java)) {
                    return simplePlayerArgumentType.cast(type)
                }

                return null
            }

        })

        env.registerCommands(env.fromClass(World::class, instanceProvider { it.newInstance() }, this), this)

        val printer = CommonPrinter(KLocale.localizer, ::println)

        printer.printAll(manager)
        printer.flush()

        val processor = Processors.createCommonProcessor(manager)

        processor.parseAndDispatch("world setblock 10 10 0 stone", this, information)
                .assertAll(listOf("setted block STONE at 10, 10, 0"))

        processor.parseAndDispatch("world tpto Adm A B C", this, information)
                .assertAll(listOf("teleported A, B, C to Adm!"))
    }

    @Test
    fun testManager() {
        val manager = ReflectCommandManagerImpl()

        manager.registerClass(TpCommand::class.java, TpCommand(), this)

        val processor = Processors.createCommonProcessor(manager)

        processor.parseAndDispatch("tp a c", this)
                .assertAll(listOf("Teleported a to c!"))
    }

    @Test
    fun testInner() {
        val manager = ReflectCommandManagerImpl()

        manager.registerClassWithInner(InnerCommands::class.java, object : InstanceProvider {
            override fun invoke(type: Class<*>): Any? = type.newInstance()
        }, this)

        val printer = CommonPrinter(KLocale.localizer, ::println)

        printer.printAll(manager)
        printer.flush()

        val processor = Processors.createCommonProcessor(manager)

        processor.parseAndDispatch("a capitalize kwcommands", this)
                .assertAll(listOf("Kwcommands"))

        processor.parseAndDispatch("a b kwcommands", this)
                .assertAll(listOf("KWCOMMANDS"))
    }

    @Test
    fun testOptionalInfo() {
        val information = InformationProvidersImpl()

        val simplePlayer = SimplePlayer("Player9")

        information.registerInformation(informationId { tags { +"player" } }, simplePlayer)

        val manager = CommandManagerImpl()
        val env = ReflectionEnvironment(manager)
        env.registerCommands(env.fromClass(TestOptInfo::class, instanceProvider { it.newInstance() }, this), this)

        val printer = CommonPrinter(KLocale.localizer, ::println)

        printer.printAll(manager)
        printer.flush()

        val processor = Processors.createCommonProcessor(manager)

        val result = processor.parseAndDispatch("getName", this, information)

        result.assertAll(listOf(simplePlayer.name))
    }

    @Test
    fun testCommandContainer() {
        val information = InformationProvidersImpl()

        val manager = CommandManagerImpl()
        val env = ReflectionEnvironment(manager)
        env.registerCommands(env.fromClass(TestCommandContainer::class, instanceProvider { it.newInstance() }, this), this)

        val printer = CommonPrinter(KLocale.localizer, ::println)

        printer.printAll(manager)
        printer.flush()

        val processor = Processors.createCommonProcessor(manager)

        val result = processor.parseAndDispatch("command container", this, information)


        result.assertWithAsserter({ Assert.assertTrue(it is CommandContainer) })
    }

}

@Cmd(name = "download", description = "Download settings")
class Download {

    @Arg(value = "url", requirements = arrayOf(
            Require(subject = Id(Player::class, "player"), required = "remote.download", testerType = PermissionRequirementTest::class)
    ))
    lateinit var url: String

    var connections: Int = 0

}

@Cmd(name = "world", description = "World actions")
class World {

    @Cmd(name = "setblock", description = "Sets the block in position x, y, z")
    @Requires(
            Require(subject = Id(Player::class, "player"),
                    required = "world.modify",
                    testerType = PermissionRequirementTest::class),
            Require(subject = Id(Player::class, "player"),
                    requiredProvider = PermProvider::class,
                    testerType = PermissionProvidedRequirementTest::class)
    )
    fun setBlock(@Arg("x") x: Int,
                 @Arg("y") y: Int,
                 @Arg("z") z: Int,
                 @Arg(value = "block")
                 @Requires(
                         Require(subject = Id(Player::class, "player"),
                                 required = "world.modify.block",
                                 testerType = PermissionRequirementTest::class
                         ),
                         Require(required = "solid",
                                 testerType = BlockParamTest::class
                         )
                 )
                 block: Block): Any = "setted block $block at $x, $y, $z"

    @Cmd(name = "tpto", description = "Teleport [players] to [target] player")
    fun tpTo(@Arg("target") target: String, @Arg("players") players: List<SimplePlayer>): Any =
            "teleported ${players.map { it.name }.joinToString()} to $target!"
}


object Player {
    fun hasPermission(perm: String) = perm == "world.modify" || perm == "world.modify.block" || perm == "level.master"
}

data class SimplePlayer(val name: String)

enum class Block {
    STONE {
        override val solid: Boolean = true
        override val fluid: Boolean = false
    },
    DIRT {
        override val solid: Boolean = true
        override val fluid: Boolean = false
    },
    WATER {
        override val solid: Boolean = false
        override val fluid: Boolean = true
    };

    abstract val solid: Boolean
    abstract val fluid: Boolean
}

class TpCommand {

    @Cmd(name = "tp", description = "Teleport a player to another")
    fun execute(@Arg("player") player: String, @Arg("target") target: String): Any =
            "Teleported $player to $target!"

}


val permissionRequirement = Requirement.create("world.modify", informationId { tags { +"player" } }, PermissionRequirementTest)


object PermissionRequirementTest : RequirementTester<Player, String> {
    override fun test(requirement: Requirement<Player, String>, value: Player): Boolean =
            value.hasPermission(requirement.required)
}

object PermissionProvidedRequirementTest : RequirementTester<Player, Permission> {
    override fun test(requirement: Requirement<Player, Permission>, value: Player): Boolean =
            value.hasPermission(requirement.required.permission)
}

object PermProvider : Supplier<Permission> {
    override fun get(): Permission = Permission("level.master")
}

data class Permission(val permission: String)

object BlockParamTest : RequirementTester<Block, String> {
    override fun test(requirement: Requirement<Block, String>, value: Block): Boolean =
            when (requirement.required) {
                "solid" -> value.solid
                "fluid" -> value.fluid
                else -> false
            }
}


@Cmd(name = "a", description = "")
class InnerCommands {

    @Cmd(name = "capitalize", description = "")
    fun capitalize(@Arg("text") text: String): String = text.capitalize()

    @Cmd(name = "b", description = "")
    class CommandB {
        @CmdHandler
        fun handle(@Arg("name") name: String): String {
            return name.toUpperCase()
        }
    }

}

@Cmd(name = "getName", description = "Gets player name.")
class TestOptInfo {
    @CmdHandler
    fun handle(@Info(Id(tags = ["player"])) player: SimplePlayer,
               @Info playerInfo: Information<SimplePlayer>): String {
        Assert.assertEquals(player.name, playerInfo.value.name) // Ensure correctness?
        return playerInfo.value.name
    }
}

@Cmd(name = "command", description = "")
class TestCommandContainer {
    @Cmd(name = "container", description = "")
    fun testContainer(@CmdContainer container: CommandContainer?): CommandContainer? = container
}