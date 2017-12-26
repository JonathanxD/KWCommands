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
package com.github.jonathanxd.kwcommands.test.welcome;

import com.github.jonathanxd.iutils.type.TypeInfo;
import com.github.jonathanxd.kwcommands.command.Command;
import com.github.jonathanxd.kwcommands.help.CommonHelpInfoHandler;
import com.github.jonathanxd.kwcommands.help.HelpInfoHandler;
import com.github.jonathanxd.kwcommands.information.Information;
import com.github.jonathanxd.kwcommands.json.DefaultJsonParser;
import com.github.jonathanxd.kwcommands.json.MapTypeResolver;
import com.github.jonathanxd.kwcommands.manager.CommandManager;
import com.github.jonathanxd.kwcommands.manager.CommandManagerImpl;
import com.github.jonathanxd.kwcommands.manager.InformationProviders;
import com.github.jonathanxd.kwcommands.manager.InformationProvidersImpl;
import com.github.jonathanxd.kwcommands.printer.CommonPrinter;
import com.github.jonathanxd.kwcommands.printer.Printers;
import com.github.jonathanxd.kwcommands.processor.CommandProcessor;
import com.github.jonathanxd.kwcommands.processor.Processors;
import com.github.jonathanxd.kwcommands.reflect.env.ReflectionEnvironment;

import org.junit.Test;

import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

public class WelcomeTests {
    @Test
    public void test10() {
        CommandManager commandManager = new CommandManagerImpl();
        CommandProcessor processor = Processors.createCommonProcessor(commandManager);
        ReflectionEnvironment reflectionEnvironment = new ReflectionEnvironment(commandManager);
        InformationProviders informationProviders = new InformationProvidersImpl();

        this.register(informationProviders, "1.0");

        List<Command> commands = reflectionEnvironment.fromClass(Welcome10.class, aClass -> new Welcome10(), this);

        commandManager.registerAll(commands, this);

        HelpInfoHandler handler = this.getHelper();
        CommonPrinter sysOutWHF = Printers.INSTANCE.getSysOutWHF();

        processor.parseAndDispatch("welcome", this, informationProviders).ifLeft(f -> {
            handler.handleFail(f, sysOutWHF);
        });
    }

    @Test
    public void test11() {
        CommandManager commandManager = new CommandManagerImpl();
        CommandProcessor processor = Processors.createCommonProcessor(commandManager);
        ReflectionEnvironment reflectionEnvironment = new ReflectionEnvironment(commandManager);
        InformationProviders informationProviders = new InformationProvidersImpl();
        MapTypeResolver mapTypeResolver = new MapTypeResolver();

        mapTypeResolver.set("Logger", Logger.class);
        mapTypeResolver.set("User", User.class);
        mapTypeResolver.set("PermissionTester", PermissionTester.class);

        this.register(informationProviders, "1.1");

        List<Command> commands = reflectionEnvironment.fromJsonClass(
                Welcome11.class,
                aClass -> new Welcome11(),
                aClass -> new DefaultJsonParser(mapTypeResolver));

        commandManager.registerAll(commands, this);

        HelpInfoHandler handler = this.getHelper();
        CommonPrinter sysOutWHF = Printers.INSTANCE.getSysOutWHF();

        processor.parseAndDispatch("mycommands welcome", this, informationProviders).ifLeft(f -> {
            handler.handleFail(f, sysOutWHF);
        });
    }

    private HelpInfoHandler getHelper() {
        return new CommonHelpInfoHandler();
    }

    private void register(InformationProviders informationProviders, String version) {
        Information.Id<Logger> loggerId = new Information.Id<>(TypeInfo.of(Logger.class), new String[]{"logger"});
        Information.Id<User> userId = new Information.Id<>(TypeInfo.of(User.class), new String[]{"user"});

        informationProviders.registerInformation(loggerId, this.getLogger(version), "Default provided logger");
        informationProviders.registerInformation(userId, this.getUser(), "Default provided user");
    }

    private Logger getLogger(String version) {
        return Logger.getLogger("WelcomeTest " + version);
    }

    private User getUser() {
        return new User() {
            @Override
            public boolean hasPermission(String permission) {
                return Objects.equals(permission, "repo.kwcommands.access");
            }

            @Override
            public String getName() {
                return "KWCommands";
            }
        };
    }
}
