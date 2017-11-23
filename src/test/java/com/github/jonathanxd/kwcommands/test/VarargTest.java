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
package com.github.jonathanxd.kwcommands.test;

import com.github.jonathanxd.iutils.collection.Collections3;
import com.github.jonathanxd.kwcommands.command.Command;
import com.github.jonathanxd.kwcommands.exception.CommandException;
import com.github.jonathanxd.kwcommands.help.CommonHelpInfoHandler;
import com.github.jonathanxd.kwcommands.help.HelpInfoHandler;
import com.github.jonathanxd.kwcommands.manager.CommandManager;
import com.github.jonathanxd.kwcommands.manager.CommandManagerImpl;
import com.github.jonathanxd.kwcommands.manager.InformationManager;
import com.github.jonathanxd.kwcommands.manager.InformationManagerImpl;
import com.github.jonathanxd.kwcommands.printer.CommonPrinter;
import com.github.jonathanxd.kwcommands.printer.Printers;
import com.github.jonathanxd.kwcommands.processor.CommandProcessor;
import com.github.jonathanxd.kwcommands.processor.Processors;
import com.github.jonathanxd.kwcommands.reflect.annotation.Arg;
import com.github.jonathanxd.kwcommands.reflect.annotation.Cmd;
import com.github.jonathanxd.kwcommands.reflect.env.ReflectionEnvironment;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class VarargTest {
    private int n = Integer.MIN_VALUE;
    private List<String> names = null;
    private int n2 = Integer.MIN_VALUE;

    @Test
    public void varargTest() {
        CommandManager commandManager = new CommandManagerImpl();
        CommandProcessor processor = Processors.createCommonProcessor(commandManager);
        ReflectionEnvironment reflectionEnvironment = new ReflectionEnvironment(commandManager);
        InformationManager informationManager = new InformationManagerImpl();

        List<Command> commands = reflectionEnvironment.fromClass(VarargTest.class,
                aClass -> this, this);

        commandManager.registerAll(commands, this);

        HelpInfoHandler handler = new CommonHelpInfoHandler();
        CommonPrinter sysOutWHF = Printers.INSTANCE.getSysOutWHF();

        try {
            processor.processAndDispatch(
                    "varargcmd 1 hey man --n2 5",
                    this,
                    informationManager);

            Assert.assertEquals(1, this.n);
            Assert.assertEquals(Collections3.listOf("hey", "man"), this.names);
            Assert.assertEquals(5, this.n2);

            processor.processAndDispatch(
                    "varargcmd 1 --names hey kw --n2 -4",
                    this,
                    informationManager);

            Assert.assertEquals(1, this.n);
            Assert.assertEquals(Collections3.listOf("hey", "kw"), this.names);
            Assert.assertEquals(-4, this.n2);
        } catch (CommandException e) {
            handler.handleCommandException(e, sysOutWHF);
        }
    }

    @Cmd(description = "Vararg test")
    public void varargcmd(@Arg("n") int n,
                          @Arg(value = "names", multiple = true) List<String> names,
                          @Arg("n2") int n2) {
        this.n = n;
        this.names = names;
        this.n2 = n2;
    }

}
