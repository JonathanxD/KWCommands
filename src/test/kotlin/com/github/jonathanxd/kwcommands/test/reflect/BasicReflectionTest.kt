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

import com.github.jonathanxd.kwcommands.manager.instanceProvider
import com.github.jonathanxd.kwcommands.processor.Processors
import com.github.jonathanxd.kwcommands.reflect.annotation.Arg
import com.github.jonathanxd.kwcommands.reflect.annotation.Cmd
import com.github.jonathanxd.kwcommands.reflect.annotation.Exclude
import com.github.jonathanxd.kwcommands.reflect.env.ReflectionEnvironment
import org.junit.Test

class BasicReflectionTest {

    @Test
    fun test() {
        val processor = Processors.createCommonProcessor()

        val reflect = ReflectionEnvironment(processor.parser.commandManager)

        reflect.registerCommands(reflect.fromClass(Gun::class.java, instanceProvider { it.newInstance() },
                this), this)

        processor.parseAndDispatch("gun 40 20", this)
        processor.parseAndDispatch("gun shoot", this)
    }

}

@Cmd(description = "A gun")
class Gun {

    @Arg(optional = true)
    var ammo: Int = 20
        set(value) {
            this.currentAmmo = value
            field = value
        }

    @Arg(optional = true)
    var cartridges: Int = 10
        set(value) {
            this.currentCartridges = value
            field = value
        }

    @Exclude
    var currentAmmo: Int = this.ammo
    @Exclude
    var currentCartridges: Int = this.cartridges


    @Cmd(description = "Shoot")
    fun shoot() {
        if (currentAmmo == 0) {
            println("No ammo")
            return
        }

        --currentAmmo

        println("Shoot. $currentAmmo/$currentCartridges")

        if (currentAmmo == 0 && currentCartridges > 0) {
            currentAmmo = ammo
            currentCartridges--
        }
    }

}