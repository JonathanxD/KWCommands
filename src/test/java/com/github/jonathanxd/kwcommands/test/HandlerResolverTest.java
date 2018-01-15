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
import com.github.jonathanxd.kwcommands.AIO;
import com.github.jonathanxd.kwcommands.KWCommands;
import com.github.jonathanxd.kwcommands.command.CommandContainer;
import com.github.jonathanxd.kwcommands.command.Handler;
import com.github.jonathanxd.kwcommands.dispatch.DispatchHandler;
import com.github.jonathanxd.kwcommands.fail.ParseFail;
import com.github.jonathanxd.kwcommands.information.InformationProvidersKt;
import com.github.jonathanxd.kwcommands.manager.CommandManager;
import com.github.jonathanxd.kwcommands.information.InformationProviders;
import com.github.jonathanxd.kwcommands.information.InformationProvidersImpl;
import com.github.jonathanxd.kwcommands.parser.CommandParser;
import com.github.jonathanxd.kwcommands.printer.Printers;
import com.github.jonathanxd.kwcommands.processor.CommandResult;
import com.github.jonathanxd.kwcommands.processor.ResultHandler;
import com.github.jonathanxd.kwcommands.processor.ValueResult;
import com.github.jonathanxd.kwcommands.reflect.HandlerResolver;
import com.github.jonathanxd.kwcommands.reflect.annotation.Arg;
import com.github.jonathanxd.kwcommands.reflect.annotation.Cmd;
import com.github.jonathanxd.kwcommands.reflect.annotation.Info;
import com.github.jonathanxd.kwcommands.reflect.element.Element;
import com.github.jonathanxd.kwcommands.reflect.element.MethodElement;
import com.github.jonathanxd.kwcommands.util.PrinterKt;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.List;

public class HandlerResolverTest {

    @Test
    public void handlerResolverTest() {
        AIO aio = KWCommands.INSTANCE.createAio(this);

        aio.getReflectionEnvironment().registerHandlerResolver(new MyHResolver());

        aio.loadObj(this)
                .registerLoaded();

        InformationProviders informationProviders = new InformationProvidersImpl();

        informationProviders.registerRecommendations(aio.getCommandManager(), aio.getParser(), aio.getDispatcher());

        Either<ParseFail, List<CommandResult>> firstA =
                aio.parseAndDispatch("first A", informationProviders);

        PrinterKt.handleFailAndThrow(aio.getHelp(), firstA, Printers.INSTANCE.getSysOutWHF());
        Assert.assertTrue(firstA.isRight());
        List<CommandResult> right = firstA.getRight();
        Assert.assertTrue(right.size() == 1);
        CommandResult commandResult1 = right.get(0);

        Assert.assertTrue(commandResult1 instanceof ValueResult);

        Object v1 = ((ValueResult) commandResult1).getValue();

        Assert.assertEquals(">> 1", v1);
    }


    @Cmd
    public int first(@Arg(value = "name") String name,
                     @Info CommandManager manager,
                     @Info CommandParser parser) {
        return 1;
    }

    static final class FirstHandler implements Handler {

        private final HandlerResolverTest instance;

        FirstHandler(HandlerResolverTest instance) {
            this.instance = instance;
        }

        @NotNull
        @Override
        public Object handle(@NotNull CommandContainer commandContainer,
                             @NotNull InformationProviders informationProviders,
                             @NotNull ResultHandler resultHandler) {
            return ">> "+this.instance.first(
                    commandContainer.getArgumentValue("name", TypeInfo.of(String.class)),
                    informationProviders.findOrEmpty(InformationProvidersKt.COMMAND_MANAGER_ID).getValue(),
                    informationProviders.findOrEmpty(InformationProvidersKt.COMMAND_PARSER_ID).getValue());
        }
    }

    static class MyHResolver implements HandlerResolver {

        @Nullable
        @Override
        public Object resolve(@NotNull Element element) {
            if (element instanceof MethodElement && ((MethodElement) element).getMethod().getName().equals("first")) {
                return new FirstHandler((HandlerResolverTest) element.getInstance());
            }

            return null;
        }

        @Nullable
        @Override
        public DispatchHandler resolveDispatchHandler(@Nullable Object instance,
                                                      @NotNull Method method,
                                                      @NotNull List<? extends Class<?>> filter) {
            return null;
        }
    }
}
