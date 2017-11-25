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
import com.github.jonathanxd.iutils.map.MapUtils;
import com.github.jonathanxd.iutils.object.Either;
import com.github.jonathanxd.kwcommands.command.Command;
import com.github.jonathanxd.kwcommands.exception.CommandException;
import com.github.jonathanxd.kwcommands.fail.CommandInputParseFail;
import com.github.jonathanxd.kwcommands.fail.InvalidInputForArgumentFail;
import com.github.jonathanxd.kwcommands.fail.ParseFail;
import com.github.jonathanxd.kwcommands.help.CommonHelpInfoHandler;
import com.github.jonathanxd.kwcommands.help.HelpInfoHandler;
import com.github.jonathanxd.kwcommands.manager.CommandManager;
import com.github.jonathanxd.kwcommands.manager.CommandManagerImpl;
import com.github.jonathanxd.kwcommands.manager.InformationManager;
import com.github.jonathanxd.kwcommands.manager.InformationManagerImpl;
import com.github.jonathanxd.kwcommands.parser.ValidatedElement;
import com.github.jonathanxd.kwcommands.printer.Printer;
import com.github.jonathanxd.kwcommands.printer.Printers;
import com.github.jonathanxd.kwcommands.processor.CommandProcessor;
import com.github.jonathanxd.kwcommands.processor.CommandResult;
import com.github.jonathanxd.kwcommands.processor.Processors;
import com.github.jonathanxd.kwcommands.reflect.annotation.Arg;
import com.github.jonathanxd.kwcommands.reflect.annotation.Cmd;
import com.github.jonathanxd.kwcommands.reflect.env.ReflectionEnvironment;
import com.github.jonathanxd.kwcommands.util.KLocale;
import com.github.jonathanxd.kwcommands.util.MapTokenExpectedFail;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class MapTest {

    private int n = Integer.MIN_VALUE;
    private Map<String, String> values = null;
    private int n2 = Integer.MIN_VALUE;

    @Test
    public void mapTest() {
        CommandManager commandManager = new CommandManagerImpl();
        CommandProcessor processor = Processors.createCommonProcessor(commandManager);
        ReflectionEnvironment reflectionEnvironment = new ReflectionEnvironment(commandManager);
        InformationManager informationManager = new InformationManagerImpl();

        List<Command> commands = reflectionEnvironment.fromClass(MapTest.class, aClass -> this, this);

        commandManager.registerAll(commands, this);

        KLocale.INSTANCE.getLocalizer().setDefaultLocale(KLocale.INSTANCE.getPtBr());
        HelpInfoHandler localizedHandler = new CommonHelpInfoHandler();
        Printer localizedSysOutWHF = Printers.INSTANCE.getSysOutWHF();

        try {
            processor.parseAndDispatch(
                    "mapcmd 897 { project = KWCommands } --n2 -8",
                    this,
                    informationManager);

            Assert.assertEquals(897, this.n);
            Assert.assertEquals(MapUtils.mapOf("project", "KWCommands"), this.values);
            Assert.assertEquals(-8, this.n2);

            processor.parseAndDispatch(
                    "mapcmd " +
                            "1 " +
                            "--values {a=\"man, i l u = {\", uhu=s} " +
                            "--n2 2",
                    this,
                    informationManager);

            Assert.assertEquals(1, this.n);
            Assert.assertEquals(MapUtils.mapOf("a", "man, i l u = {", "uhu", "s"), this.values);
            Assert.assertEquals(2, this.n2);

            Either<ParseFail, List<CommandResult>> parse = processor.parseAndDispatch(
                    "mapcmd " +
                            "1 " +
                            "--values {a=\"man, i l u = {\", uhu=[s,b]{a=b}b} " +
                            //"--values {a=\"man, \"" +
                            "--n2 2",
                    this,
                    informationManager);

            localizedHandler.handleFail(parse.getLeft(), localizedSysOutWHF);

            Assert.assertTrue(parse.isLeft());
            Assert.assertTrue(parse.getLeft() instanceof CommandInputParseFail);
            CommandInputParseFail fail = (CommandInputParseFail) parse.getLeft();
            Assert.assertTrue(fail.getFail() instanceof MapTokenExpectedFail);

            MapTokenExpectedFail mapTokenFail = (MapTokenExpectedFail) fail.getFail();
            Assert.assertTrue(mapTokenFail.getTokens().equals(Collections3.listOf(',', '}')));

            parse = processor.parseAndDispatch(
                    "mapcmd " +
                            "1 " +
                            "--values {a=\"man, i l u = {\", uhu=[s,b]} " +
                            //"--values {a=\"man, \"" +
                            "--n2 2",
                    this,
                    informationManager);


            localizedHandler.handleFail(parse.getLeft(), localizedSysOutWHF);

            Assert.assertTrue(parse.isLeft());
            Assert.assertTrue(parse.getLeft() instanceof InvalidInputForArgumentFail);
            InvalidInputForArgumentFail invalidInput = (InvalidInputForArgumentFail) parse.getLeft();

            List<ValidatedElement> invalids = invalidInput.getValidation().getInvalids();

            Assert.assertTrue(invalids.size() == 1);
            Assert.assertEquals(43, invalids.get(0).getInput().getStart());
            Assert.assertEquals(47, invalids.get(0).getInput().getEnd());
            Assert.assertEquals("[s,b]", invalids.get(0).getInput().getString());
        } catch (CommandException e) {
            localizedHandler.handleCommandException(e, localizedSysOutWHF);
            e.printStackTrace();
        }
    }

    @Cmd(description = "Vararg test")
    public void mapcmd(@Arg("n") int n,
                       @Arg(value = "values", multiple = true) Map<String, String> values,
                       @Arg("n2") int n2) {
        this.n = n;
        this.values = values;
        this.n2 = n2;
    }

}
