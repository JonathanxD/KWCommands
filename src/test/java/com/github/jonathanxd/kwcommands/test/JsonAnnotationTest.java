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

import com.github.jonathanxd.iutils.object.Either;
import com.github.jonathanxd.iutils.type.TypeInfo;
import com.github.jonathanxd.kwcommands.command.Command;
import com.github.jonathanxd.kwcommands.fail.ParseFail;
import com.github.jonathanxd.kwcommands.help.CommonHelpInfoHandler;
import com.github.jonathanxd.kwcommands.information.Information;
import com.github.jonathanxd.kwcommands.json.CmdJson;
import com.github.jonathanxd.kwcommands.json.CmdJsonType;
import com.github.jonathanxd.kwcommands.json.DefaultJsonParser;
import com.github.jonathanxd.kwcommands.json.MapTypeResolver;
import com.github.jonathanxd.kwcommands.manager.CommandManager;
import com.github.jonathanxd.kwcommands.manager.CommandManagerImpl;
import com.github.jonathanxd.kwcommands.manager.InformationManager;
import com.github.jonathanxd.kwcommands.manager.InformationManagerImpl;
import com.github.jonathanxd.kwcommands.printer.CommonPrinter;
import com.github.jonathanxd.kwcommands.printer.Printer;
import com.github.jonathanxd.kwcommands.processor.CommandProcessor;
import com.github.jonathanxd.kwcommands.processor.CommandResult;
import com.github.jonathanxd.kwcommands.processor.Processors;
import com.github.jonathanxd.kwcommands.reflect.annotation.Arg;
import com.github.jonathanxd.kwcommands.reflect.env.ReflectionEnvironment;
import com.github.jonathanxd.kwcommands.util.KLocale;
import com.github.jonathanxd.kwcommands.util.PrinterKt;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

import kotlin.Unit;

@CmdJson(type = CmdJsonType.RESOURCE, value = "/register.json")
public class JsonAnnotationTest {

    private String name;
    private String email;
    private int called = 0;

    @Test
    public void jsonAnnotationTest() {
        MapTypeResolver mapTypeResolver = new MapTypeResolver();

        JsonTest.registerEssentials(this, mapTypeResolver);

        CommandManager manager = new CommandManagerImpl();

        ReflectionEnvironment reflectionEnvironment = new ReflectionEnvironment(manager);

        List<Command> commands = reflectionEnvironment
                .fromJsonClass(JsonAnnotationTest.class, f -> this, f -> new DefaultJsonParser(mapTypeResolver));

        for (Command command : commands) {
            manager.registerCommand(command, this);
        }

        CommandProcessor processor = Processors.createCommonProcessor(manager);

        InformationManagerImpl informationManager = new InformationManagerImpl();

        informationManager.<JsonTest.Player>registerInformation(new Information.Id<>(TypeInfo.of(JsonTest.Player.class),
                new String[]{"player"}), s -> s.equals("perm.register"), "player requesting register.");

        final String pname = "huh";
        final String pemail = "huh@email.com";

        this.handle("register " + pname + " " + pemail, processor, informationManager);

        Assert.assertEquals(pname, this.name);
        Assert.assertEquals(pemail, this.email);
        Assert.assertEquals(1, this.called);

        this.handle("register any " + pname, processor, informationManager);

        Assert.assertEquals(pname, this.name);
        Assert.assertEquals(null, this.email);
        Assert.assertEquals(2, this.called);

        Printer printer = new CommonPrinter(KLocale.INSTANCE.getLocalizer(),
                s -> {
                    System.out.println(s);
                    return Unit.INSTANCE;
                }, true);

        for (Command command : commands) {
            PrinterKt.printAll(printer, command);
        }

        printer.flush();

    }

    private List<CommandResult> handle(String commandString,
                                       CommandProcessor processor,
                                       InformationManager informationManager) {

        Either<ParseFail, List<CommandResult>> commandResults = processor.parseAndDispatch(
                commandString,
                this, informationManager);

        CommonHelpInfoHandler commonHelpInfoHandler = new CommonHelpInfoHandler();

        commonHelpInfoHandler.handleResults(commandResults.getRight(), new CommonPrinter(KLocale.INSTANCE.getLocalizer(),
                s -> {
                    System.out.println(s);
                    return Unit.INSTANCE;
                }, false));

        return commandResults.getRight();



    }

    public void register(@Arg("name") String name, @Arg("email") String email) {
        this.called++;
        this.name = name;
        this.email = email;
    }

    public void any(@Arg("name") String name) {
        this.called++;
        this.name = name;
        this.email = null;
    }
}
