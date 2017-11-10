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
import com.github.jonathanxd.iutils.exception.RethrowException;
import com.github.jonathanxd.iutils.type.TypeInfo;
import com.github.jonathanxd.kwcommands.argument.Argument;
import com.github.jonathanxd.kwcommands.argument.ArgumentContainer;
import com.github.jonathanxd.kwcommands.command.Command;
import com.github.jonathanxd.kwcommands.command.CommandContainer;
import com.github.jonathanxd.kwcommands.command.Handler;
import com.github.jonathanxd.kwcommands.help.CommonHelpInfoHandler;
import com.github.jonathanxd.kwcommands.information.Information;
import com.github.jonathanxd.kwcommands.json.DefaultJsonParser;
import com.github.jonathanxd.kwcommands.json.JsonCommandParser;
import com.github.jonathanxd.kwcommands.json.MapTypeResolver;
import com.github.jonathanxd.kwcommands.json.TypeResolverKt;
import com.github.jonathanxd.kwcommands.manager.CommandManager;
import com.github.jonathanxd.kwcommands.manager.CommandManagerImpl;
import com.github.jonathanxd.kwcommands.manager.InformationManager;
import com.github.jonathanxd.kwcommands.manager.InformationManagerImpl;
import com.github.jonathanxd.kwcommands.parser.Input;
import com.github.jonathanxd.kwcommands.printer.CommonPrinter;
import com.github.jonathanxd.kwcommands.processor.CommandProcessor;
import com.github.jonathanxd.kwcommands.processor.CommandResult;
import com.github.jonathanxd.kwcommands.processor.Processors;
import com.github.jonathanxd.kwcommands.processor.ResultHandler;
import com.github.jonathanxd.kwcommands.reflect.annotation.Arg;
import com.github.jonathanxd.kwcommands.reflect.env.ReflectionEnvironment;
import com.github.jonathanxd.kwcommands.requirement.Requirement;
import com.github.jonathanxd.kwcommands.requirement.RequirementTester;
import com.github.jonathanxd.kwcommands.argument.Validator;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import kotlin.Unit;

public class JsonTest {

    private static final String JSON = "{\n" +
            "  \"name\": \"register\",\n" +
            "  \"description\": \"Registers the user\",\n" +
            "  \"handler\": \"MyHandler\",\n" +
            "  \"arguments\": [\n" +
            "    {\n" +
            "      \"id\": \"name\",\n" +
            "      \"type\": \"String\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": \"email\",\n" +
            "      \"type\": \"String\",\n" +
            "      \"validator\": \"EmailValidator\"\n" +
            "    }\n" +
            "  ],\n" +
            "  \"requiredInfo\": [\n" +
            "    {\n" +
            "      \"id\": { \"tags\": [\"player\"], \"type\": \"Player\" }\n" +
            "    }\n" +
            "  ],\n" +
            "  \"requirements\": [\n" +
            "    {\n" +
            "      \"info\": { \"tags\": [\"player\"], \"type\": \"Player\" },\n" +
            "      \"tester\": \"ReqTester\",\n" +
            "      \"data\": \"perm.register\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";

    private String name;
    private String email;
    private int called = 0;

    public static void registerEssentials(Object o, MapTypeResolver resolver) {
        resolver.set("Player", Player.class);
        resolver.set("String", String.class);
        resolver.set("MyHandler", MyHandler.class);
        resolver.set("EmailValidator", EmailValidator.class);
        resolver.set("ReqTester", ReqTester.class);
        resolver.getLoaders().add(o.getClass().getClassLoader());
    }

    @Test
    public void jsonWithReflectionTest() {
        MapTypeResolver mapTypeResolver = new MapTypeResolver();
        TypeResolverKt.registerDefaults(mapTypeResolver);
        registerEssentials(this, mapTypeResolver);

        JsonCommandParser parser = new DefaultJsonParser(mapTypeResolver);

        CommandManager manager = new CommandManagerImpl();

        ReflectionEnvironment reflectionEnvironment = new ReflectionEnvironment(manager);

        mapTypeResolver.getSingletonInstances().put(MyHandler.class, getRegisterMethodHandler(reflectionEnvironment));

        Command command = parser.parseCommand(JSON);

        manager.registerCommand(command, this);

        CommandProcessor processor = Processors.createCommonProcessor(manager);

        InformationManagerImpl informationManager = new InformationManagerImpl();

        informationManager.<JsonTest.Player>registerInformation(new Information.Id<>(TypeInfo.of(JsonTest.Player.class),
                new String[] { "player"}), s -> s.equals("perm.register"), "player requesting register.");

        final String pname = "huh";
        final String pemail = "huh@email.com";

        List<CommandResult> commandResults = processor.processAndHandle(
                Collections3.listOf("register", pname, pemail),
                this, informationManager);

        CommonHelpInfoHandler commonHelpInfoHandler = new CommonHelpInfoHandler();

        commonHelpInfoHandler.handleResults(commandResults, new CommonPrinter(s -> {
            System.out.println(s);
            return Unit.INSTANCE;
        }, false));

        Assert.assertEquals(pname, this.name);
        Assert.assertEquals(pemail, this.email);
        Assert.assertEquals(1, this.called);

    }


    private Handler getRegisterMethodHandler(ReflectionEnvironment reflectionEnvironment) {
        try {
            return reflectionEnvironment.createHandler(this,
                    this.getClass().getDeclaredMethod("register", String.class, String.class));
        } catch (NoSuchMethodException e) {
            throw RethrowException.rethrow(e);
        }
    }

    public void register(@Arg("name") String name, @Arg("email") String email) {
        this.name = name;
        this.email = email;
        this.called++;
    }

    public static interface Player {
        boolean hasPermission(String s);
    }

    public static class MyHandler implements Handler {

        public static final MyHandler INSTANCE = new MyHandler();

        @NotNull
        @Override
        public Object handle(@NotNull CommandContainer commandContainer,
                             @NotNull InformationManager informationManager,
                             @NotNull ResultHandler resultHandler) {
            return null;
        }
    }

    public static class ReqTester implements RequirementTester<Player, String> {
        public static final ReqTester INSTANCE = new ReqTester();

        @Override
        public boolean test(@NotNull Requirement<Player, String> requirement,
                            @NotNull Information<? extends Player> information) {
            return information.getValue().hasPermission(requirement.getRequired());
        }
    }


    public static class EmailValidator implements Validator {
        public static final EmailValidator INSTANCE = new EmailValidator();

        private static final Pattern REGEX = Pattern.compile("(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])");

        private static final Predicate<String> pred = REGEX.asPredicate();

        @Override
        public boolean invoke(@NotNull List<? extends ArgumentContainer<?>> parsed,
                              @NotNull Argument<?> current,
                              @NotNull Input value) {
            return pred.test(value.getValue());
        }

    }
}
