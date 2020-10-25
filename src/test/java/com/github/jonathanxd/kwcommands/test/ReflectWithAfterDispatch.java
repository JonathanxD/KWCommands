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
package com.github.jonathanxd.kwcommands.test;

import com.github.jonathanxd.kwcommands.AIO;
import com.github.jonathanxd.kwcommands.KWCommands;
import com.github.jonathanxd.kwcommands.dispatch.DispatchHandler;
import com.github.jonathanxd.kwcommands.information.InformationProvidersImpl;
import com.github.jonathanxd.kwcommands.processor.CommandResult;
import com.github.jonathanxd.kwcommands.processor.ValueResult;
import com.github.jonathanxd.kwcommands.reflect.annotation.AfterDispatch;
import com.github.jonathanxd.kwcommands.reflect.annotation.Arg;
import com.github.jonathanxd.kwcommands.reflect.annotation.Cmd;
import com.github.jonathanxd.kwcommands.reflect.env.ReflectionEnvironment;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

import kotlin.Unit;

public class ReflectWithAfterDispatch {

    private static int handled = 0;
    private static int handled2 = 0;

    @Test
    public void test() {
        Say say = new Say();
        Calc calc = new Calc();

        AIO aio = KWCommands.INSTANCE.createAio(this);
        ReflectionEnvironment reflectionEnvironment = aio.getReflectionEnvironment();
        reflectionEnvironment.registerCommands(
                reflectionEnvironment.fromClass(Say.class, c -> say, this), this);
        reflectionEnvironment.registerCommands(
                reflectionEnvironment.fromClass(Calc.class, c -> calc, this), this);

        List<DispatchHandler> dispatchHandlers = reflectionEnvironment.dispatchHandlersFrom(
                Say.class, c -> say);

        aio.getDispatcher()
                .registerDispatchHandlers(dispatchHandlers);


        aio.getProcessor().parseAndDispatch("say hello calc plus 4 4", this,
                new InformationProvidersImpl());

        Assert.assertEquals(2, handled);
        Assert.assertEquals(1, handled2);
    }

    @Cmd(name = "say")
    public static class Say {
        @Cmd
        public String hello() {
            return "Hello";
        }

        @AfterDispatch
        public void all(List<CommandResult> results) {
            handled = (int) results.stream()
                    .filter(i -> !(i instanceof ValueResult) || ((ValueResult) i).getValue() != Unit.INSTANCE)
                    .count();
        }

        @AfterDispatch(filter = Say.class)
        public void onlySay(List<CommandResult> results) {
            handled2 = (int) results.stream()
                    .filter(i -> !(i instanceof ValueResult) || ((ValueResult) i).getValue() != Unit.INSTANCE)
                    .count();
        }
    }

    @Cmd(name = "calc")
    public static class Calc {
        @Cmd
        public int plus(@Arg("a") int a,
                        @Arg("b") int b) {
            return a + b;
        }
    }
}
