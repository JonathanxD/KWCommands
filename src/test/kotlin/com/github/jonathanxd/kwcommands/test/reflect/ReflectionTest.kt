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

import com.github.jonathanxd.kwcommands.exception.InformationMissingException
import com.github.jonathanxd.kwcommands.exception.UnsatisfiedRequirementException
import com.github.jonathanxd.kwcommands.information.Information
import com.github.jonathanxd.kwcommands.manager.CommandManagerImpl
import com.github.jonathanxd.kwcommands.manager.InformationManagerImpl
import com.github.jonathanxd.kwcommands.printer.CommonPrinter
import com.github.jonathanxd.kwcommands.processor.Processors
import com.github.jonathanxd.kwcommands.reflect.annotation.Arg
import com.github.jonathanxd.kwcommands.reflect.annotation.Cmd
import com.github.jonathanxd.kwcommands.reflect.annotation.Id
import com.github.jonathanxd.kwcommands.reflect.annotation.Require
import com.github.jonathanxd.kwcommands.reflect.env.ReflectionEnvironment
import com.github.jonathanxd.kwcommands.requirement.Requirement
import com.github.jonathanxd.kwcommands.requirement.RequirementTester
import com.github.jonathanxd.kwcommands.test.assertAll
import com.github.jonathanxd.kwcommands.util.printAll
import com.github.jonathanxd.kwcommands.util.registerInformation
import org.junit.Test

class ReflectionTest {

    @Test(expected = UnsatisfiedRequirementException::class)
    fun test() {
        val information = InformationManagerImpl()

        information.registerInformation(Information.Id(Player::class.java, arrayOf("player")), Player)

        val manager = CommandManagerImpl()
        val env = ReflectionEnvironment(manager)
        env.registerCommands(env.fromClass(Download::class, { it.newInstance() }, this), this)

        val printer = CommonPrinter(::println)

        printer.printAll(manager)

        val processor = Processors.createCommonProcessor(manager)

        processor.handle(processor.process(listOf("download", "https://askdsal.0/x.file", "10"), this), information)// ?
    }

    @Test(expected = InformationMissingException::class)
    fun testMissReq() {

        val manager = CommandManagerImpl()
        val env = ReflectionEnvironment(manager)
        env.registerCommands(env.fromClass(Download::class, { it.newInstance() }, this), this)

        val printer = CommonPrinter(::println)

        printer.printAll(manager)

        val processor = Processors.createCommonProcessor(manager)

        processor.handle(processor.process(listOf("download", "https://askdsal.0/x.file", "10"), this))// ?
    }

    @Test
    fun game() {
        val information = InformationManagerImpl()

        information.registerInformation(Information.Id(Player::class.java, arrayOf("player")), Player)

        val manager = CommandManagerImpl()
        val env = ReflectionEnvironment(manager)

        env.registerCommands(env.fromClass(World::class, { it.newInstance() }, this), this)

        val printer = CommonPrinter(::println)

        printer.printAll(manager)

        val processor = Processors.createCommonProcessor(manager)

        processor.handle(processor.process(listOf("world", "setblock", "10", "10", "0", "stone"), this), information)
                .assertAll(listOf("setted block STONE at 10, 10, 0"))
    }

}


@Cmd(name = "download", description = "Download settings")
class Download {

    @Arg(value = "url", requirements = arrayOf(
            Require(subject = Id(Player::class, "player"), data = "remote.download", infoType = Player::class, testerType = PermissionRequirementTest::class)
    ))
    lateinit var url: String

    var connetions: Int = 0

}

@Cmd(name = "world", description = "World actions")
class World {

    @Cmd(name = "setblock", description = "Sets the block in position x, y, z",
            requirements = arrayOf(
                    Require(subject = Id(Player::class, "player"), data = "world.modify", infoType = Player::class, testerType = PermissionRequirementTest::class)
            ))
    fun setBlock(@Arg("x") x: Int,
                 @Arg("y") y: Int,
                 @Arg("z") z: Int,
                 @Arg(value = "block", requirements = arrayOf(
                         Require(subject = Id(Player::class, "player"), data = "world.modify.block", infoType = Player::class, testerType = PermissionRequirementTest::class)
                 )) block: Block): Any {
        return "setted block $block at $x, $y, $z"
    }
}


object Player {
    fun hasPermission(perm: String) = perm == "world.modify" || perm == "world.modify.block"
}

enum class Block {
    STONE,
    DIRT
}


val permissionRequirement = Requirement.create("world.modify", Information.Id(Player::class.java, arrayOf("player")), PermissionRequirementTest)


object PermissionRequirementTest : RequirementTester<Player, String> {
    override fun test(requirement: Requirement<Player, String>, information: Information<Player>) {
        if (!information.value.hasPermission(requirement.required))
            throw UnsatisfiedRequirementException("Player ${information.value} does not have permission ${requirement.required}")
    }

}