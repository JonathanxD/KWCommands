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
package com.github.jonathanxd.kwcommands.test.reflect

import com.github.jonathanxd.iutils.type.TypeInfo
import com.github.jonathanxd.kwcommands.dsl.informationId
import com.github.jonathanxd.kwcommands.information.Information
import com.github.jonathanxd.kwcommands.manager.CommandManagerImpl
import com.github.jonathanxd.kwcommands.manager.InformationManagerImpl
import com.github.jonathanxd.kwcommands.manager.InstanceProvider
import com.github.jonathanxd.kwcommands.manager.ReflectCommandManagerImpl
import com.github.jonathanxd.kwcommands.printer.CommonPrinter
import com.github.jonathanxd.kwcommands.processor.Processors
import com.github.jonathanxd.kwcommands.processor.UnsatisfiedRequirementsResult
import com.github.jonathanxd.kwcommands.reflect.annotation.*
import com.github.jonathanxd.kwcommands.reflect.env.ArgumentType
import com.github.jonathanxd.kwcommands.reflect.env.ArgumentTypeProvider
import com.github.jonathanxd.kwcommands.reflect.env.ReflectionEnvironment
import com.github.jonathanxd.kwcommands.reflect.env.cast
import com.github.jonathanxd.kwcommands.requirement.Reason
import com.github.jonathanxd.kwcommands.requirement.Requirement
import com.github.jonathanxd.kwcommands.requirement.RequirementTester
import com.github.jonathanxd.kwcommands.test.assertAll
import com.github.jonathanxd.kwcommands.util.ArgumentType
import com.github.jonathanxd.kwcommands.util.printAll
import org.junit.Assert
import org.junit.Test

class ReflectionTest {

    @Test
    fun test() {
        val information = InformationManagerImpl()

        information.registerInformation(informationId { tags { +"player" } }, Player)

        val manager = CommandManagerImpl()
        val env = ReflectionEnvironment(manager)
        env.registerCommands(env.fromClass(Download::class, { it.newInstance() }, this), this)

        val printer = CommonPrinter(::println)

        printer.printAll(manager)
        printer.flush()

        val processor = Processors.createCommonProcessor(manager)

        val result = processor.handle(processor.process(listOf("download", "https://askdsal.0/x.file", "10"), this), information)// ?

        Assert.assertTrue(result.any { it is UnsatisfiedRequirementsResult })
    }

    @Test
    fun testMissInfo() {

        val manager = CommandManagerImpl()
        val env = ReflectionEnvironment(manager)
        env.registerCommands(env.fromClass(Download::class, { it.newInstance() }, this), this)

        val printer = CommonPrinter(::println)

        printer.printAll(manager)
        printer.flush()

        val processor = Processors.createCommonProcessor(manager)

        val result = processor.handle(processor.process(listOf("download", "https://askdsal.0/x.file", "10"), this))// ?

        Assert.assertTrue(result.any {
            it is UnsatisfiedRequirementsResult
                    && it.unsatisfiedRequirements.isNotEmpty()
                    && it.unsatisfiedRequirements.first().reason == Reason.MISSING_INFORMATION
        })
    }

    @Test
    fun game() {
        val information = InformationManagerImpl()

        information.registerInformation(informationId { tags { +"player" } }, Player)

        val manager = CommandManagerImpl()
        val env = ReflectionEnvironment(manager)

        env.registerCommands(env.fromClass(World::class, { it.newInstance() }, this), this)

        val printer = CommonPrinter(::println)

        printer.printAll(manager)
        printer.flush()

        val processor = Processors.createCommonProcessor(manager)

        processor.handle(processor.process(listOf("world", "setblock", "10", "10", "0", "stone"), this), information)
                .assertAll(listOf("setted block STONE at 10, 10, 0"))

        ReflectionEnvironment.registerGlobal(object : ArgumentTypeProvider {
            override fun <T> provide(type: TypeInfo<T>): ArgumentType<T>? {
                if (type == TypeInfo.of(SimplePlayer::class.java)) {
                    return ArgumentType({ true }, { SimplePlayer(it) }, { emptyList() }, null).cast(type)
                }

                return null
            }

        })

        processor.handle(processor.process(listOf("world", "tpto", "Adm", "A,B,C"), this), information)
                .assertAll(listOf("teleported A, B, C to Adm!"))
    }

    @Test
    fun testManager() {
        val manager = ReflectCommandManagerImpl()

        manager.registerClass(TpCommand::class.java, TpCommand(), this)

        val processor = Processors.createCommonProcessor(manager)

        processor.handle(processor.process(listOf("tp", "a", "c"), this))
                .assertAll(listOf("Teleported a to c!"))
    }

    @Test
    fun testInner() {
        val manager = ReflectCommandManagerImpl()

        manager.registerClassWithInner(InnerCommands::class.java, object : InstanceProvider {
            override fun <T> get(type: Class<T>): T {
                return type.newInstance()
            }
        }, this)

        val printer = CommonPrinter(::println)

        printer.printAll(manager)
        printer.flush()

        val processor = Processors.createCommonProcessor(manager)

        processor.handle(processor.process(listOf("a", "capitalize", "kwcommands"), this))
                .assertAll(listOf("Kwcommands"))

        processor.handle(processor.process(listOf("a", "b", "kwcommands"), this))
                .assertAll(listOf("KWCOMMANDS"))
    }

    @Test
    fun testOptionalInfo() {
        val information = InformationManagerImpl()

        val simplePlayer = SimplePlayer("Player9")

        information.registerInformation(informationId { tags { +"player" } }, simplePlayer)

        val manager = CommandManagerImpl()
        val env = ReflectionEnvironment(manager)
        env.registerCommands(env.fromClass(TestOptInfo::class, { it.newInstance() }, this), this)

        val printer = CommonPrinter(::println)

        printer.printAll(manager)
        printer.flush()

        val processor = Processors.createCommonProcessor(manager)

        val result = processor.handle(processor.process(listOf("getName"), this), information)

        result.assertAll(listOf(simplePlayer.name))
    }

}


@Cmd(name = "download", description = "Download settings")
class Download {

    @Arg(value = "url", requirements = arrayOf(
            Require(subject = Id(Player::class, "player"), data = "remote.download", testerType = PermissionRequirementTest::class)
    ))
    lateinit var url: String

    var connetions: Int = 0

}

@Cmd(name = "world", description = "World actions")
class World {

    @Cmd(name = "setblock", description = "Sets the block in position x, y, z",
            requirements = arrayOf(
                    Require(subject = Id(Player::class, "player"), data = "world.modify", testerType = PermissionRequirementTest::class)
            ))
    fun setBlock(@Arg("x") x: Int,
                 @Arg("y") y: Int,
                 @Arg("z") z: Int,
                 @Arg(value = "block", requirements = arrayOf(
                         Require(subject = Id(Player::class, "player"), data = "world.modify.block", testerType = PermissionRequirementTest::class)
                 )) block: Block): Any = "setted block $block at $x, $y, $z"

    @Cmd(name = "tpto", description = "Teleport [players] to [target] player")
    fun tpTo(@Arg("target") target: String, @Arg("players") players: List<SimplePlayer>): Any =
            "teleported ${players.map { it.name }.joinToString()} to $target!"
}


object Player {
    fun hasPermission(perm: String) = perm == "world.modify" || perm == "world.modify.block"
}

data class SimplePlayer(val name: String)

enum class Block {
    STONE,
    DIRT
}

class TpCommand {

    @Cmd(name = "tp", description = "Teleport a player to another")
    fun execute(@Arg("player") player: String, @Arg("target") target: String): Any =
            "Teleported $player to $target!"

}


val permissionRequirement = Requirement.create("world.modify", informationId { tags { +"player" } }, PermissionRequirementTest)


object PermissionRequirementTest : RequirementTester<Player, String> {
    override fun test(requirement: Requirement<Player, String>, information: Information<Player>): Boolean =
            information.value.hasPermission(requirement.required)
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
    fun handle(@Info(Id(tags = "player")) player: SimplePlayer,
               @Info playerInfo: Information<SimplePlayer>): String {
        Assert.assertEquals(player.name, playerInfo.value.name) // Ensure correctness?
        return playerInfo.value.name
    }
}