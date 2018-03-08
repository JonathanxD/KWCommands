/*
 *      KWCommands - New generation of WCommands written in Kotlin <https://github.com/JonathanxD/KWCommands>
 *
 *         The MIT License (MIT)
 *
 *      Copyright (c) 2018 JonathanxD
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
import com.github.jonathanxd.iutils.text.Text;
import com.github.jonathanxd.iutils.type.TypeInfo;
import com.github.jonathanxd.iutils.type.TypeParameterProvider;
import com.github.jonathanxd.kwcommands.AIO;
import com.github.jonathanxd.kwcommands.KWCommands;
import com.github.jonathanxd.kwcommands.argument.Argument;
import com.github.jonathanxd.kwcommands.argument.ArgumentContainer;
import com.github.jonathanxd.kwcommands.argument.Arguments;
import com.github.jonathanxd.kwcommands.argument.ListArgumentType;
import com.github.jonathanxd.kwcommands.argument.MapArgumentType;
import com.github.jonathanxd.kwcommands.command.Command;
import com.github.jonathanxd.kwcommands.command.CommandContainer;
import com.github.jonathanxd.kwcommands.command.CommandContext;
import com.github.jonathanxd.kwcommands.command.Handler;
import com.github.jonathanxd.kwcommands.completion.CompletionImpl;
import com.github.jonathanxd.kwcommands.information.InformationProviders;
import com.github.jonathanxd.kwcommands.information.InformationProvidersVoid;
import com.github.jonathanxd.kwcommands.processor.ResultHandler;
import com.github.jonathanxd.kwcommands.reflect.annotation.Arg;
import com.github.jonathanxd.kwcommands.reflect.annotation.Cmd;
import com.github.jonathanxd.kwcommands.reflect.annotation.Ctx;
import com.github.jonathanxd.kwcommands.reflect.annotation.DynamicArgs;
import com.github.jonathanxd.kwcommands.util.CommonArgTypesKt;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import kotlin.Unit;

public class DynamicArgumentsTest {

    private boolean dispatched = false;

    @Test
    public void dynamicArgumentsTest() {
        AIO aio = KWCommands.INSTANCE.createAio(this);

        aio.registerCommand(Command.builder()
                .name("create")
                .handler(new MyHandler())
                .description(Text.single("Create"))
                .arguments(new MyArguments())
                .build());

        CompletionImpl completion = aio.getCompletion();

        List<String> complete = completion.complete("create ", this, InformationProvidersVoid.INSTANCE);

        Assert.assertEquals(Collections3.listOf("--type", "TypeA", "TypeB"), complete);

        complete = completion.complete("create TypeA ", this, InformationProvidersVoid.INSTANCE);

        Assert.assertEquals(Collections3.listOf("--list", "["), complete);

        complete = completion.complete("create TypeB ", this, InformationProvidersVoid.INSTANCE);

        Assert.assertEquals(Collections3.listOf("--map", "{"), complete);

        complete = completion.complete("create TypeB {", this, InformationProvidersVoid.INSTANCE);

        Assert.assertEquals(Collections3.listOf("}", "TypeA", "TypeB"), complete);

        complete = completion.complete("create TypeB {TypeA=c", this, InformationProvidersVoid.INSTANCE);

        Assert.assertEquals(Collections3.listOf(",", "}"), complete);

        complete = completion.complete("create TypeB {TypeA=c,", this, InformationProvidersVoid.INSTANCE);

        Assert.assertEquals(Collections3.listOf("TypeA", "TypeB"), complete);

        complete = completion.complete("create TypeB {TypeA=c}", this, InformationProvidersVoid.INSTANCE);

        Assert.assertEquals(Collections3.listOf(), complete);

        complete = completion.complete("create TypeB {TypeA=c} ", this, InformationProvidersVoid.INSTANCE);

        Assert.assertEquals(Collections3.listOf(), complete);
    }

    @Test
    public void reflectDynamicArgumentsTest() {
        AIO aio = KWCommands.INSTANCE.createAio(this);

        aio.loadObj(this).registerLoaded();

        aio.parseAndDispatch("create TypeA [A, 1]", InformationProvidersVoid.INSTANCE);
        Assert.assertTrue(this.dispatched);
        this.reset();

        aio.parseAndDispatch("create TypeB {TypeB=1}", InformationProvidersVoid.INSTANCE);
        Assert.assertTrue(this.dispatched);
        this.reset();
    }

    @Test
    public void reflectDynamicArgumentsCtxTest() {
        AIO aio = KWCommands.INSTANCE.createAio(this);

        aio.loadObj(this).registerLoaded();

        aio.parseAndDispatch("create2 TypeA [A, 1]", InformationProvidersVoid.INSTANCE);
        Assert.assertTrue(this.dispatched);
        this.reset();

        aio.parseAndDispatch("create2 TypeB {TypeB=1}", InformationProvidersVoid.INSTANCE);
        Assert.assertTrue(this.dispatched);
        this.reset();
    }

    private void reset() {
        this.dispatched = false;
    }

    @Cmd
    @DynamicArgs(MyArguments.class)
    public void create(@Arg("type") MyType type,
                       @Arg("list") List<String> list,
                       @Arg("map") Map<MyType, String> map) {
        this.dispatched = true;

        if (type == MyType.TypeA) {
            Assert.assertEquals(null, map);
            Assert.assertEquals(Collections3.listOf("A", "1"), list);
        } else {
            Assert.assertEquals(null, list);
            Assert.assertEquals(MapUtils.mapOf(MyType.TypeB, "1"), map);
        }
    }

    @Cmd
    @DynamicArgs(MyArguments.class)
    public void create2(@Arg("type") MyType type,
                        @Ctx CommandContext context) {
        this.dispatched = true;

        if (type == MyType.TypeA) {
            Assert.assertEquals(Collections3.listOf("A", "1"),
                    context.getArgById("list"));
        } else {
            Assert.assertEquals(MapUtils.mapOf(MyType.TypeB, "1"),
                    context.getArgById("map"));
        }
    }


    public static enum MyType {
        TypeA,
        TypeB
    }

    private static class MyHandler implements Handler {

        @NotNull
        @Override
        public Object handle(@NotNull CommandContainer commandContainer,
                             @NotNull InformationProviders informationProviders,
                             @NotNull ResultHandler resultHandler) {
            return Unit.INSTANCE;
        }
    }

    public static class MyArguments implements Arguments {

        public static final MyArguments INSTANCE = new MyArguments();

        private final TypeInfo<List<String>> tp = new TypeParameterProvider<List<String>>() {
        }.createTypeInfo();
        private final TypeInfo<Map<MyType, String>> tp2 =
                new TypeParameterProvider<Map<MyType, String>>() {
                }.createTypeInfo();

        private final Argument<MyType> typeArgument = Argument.<MyType>builder()
                .argumentType(CommonArgTypesKt.enumArgumentType(MyType.class))
                .name("type")
                .build();

        private final Argument<?> listArgument = Argument.builder()
                .argumentType(new ListArgumentType<>(CommonArgTypesKt.stringArgumentType(), tp))
                .name("list")
                .build();

        private final Argument<?> mapArgument = Argument.builder()
                .argumentType(new MapArgumentType<>(
                        CommonArgTypesKt.enumArgumentType(MyType.class),
                        CommonArgTypesKt.stringArgumentType(),
                        tp2))
                .name("map")
                .build();

        @NotNull
        @Override
        public List<Argument<?>> getRemainingArguments(@NotNull List<? extends ArgumentContainer<?>> current) {

            if (current.isEmpty())
                return this.getRemainingArguments();

            if (current.size() > 1)
                return Collections.emptyList();

            ArgumentContainer<?> argumentContainer = current.get(0);

            if (argumentContainer.getArgument() == this.typeArgument) {
                MyType m = (MyType) argumentContainer.getValue();

                if (m == MyType.TypeA)
                    return Collections.singletonList(this.listArgument);
                else
                    return Collections.singletonList(this.mapArgument);
            }

            return Collections.emptyList();
        }

        @NotNull
        @Override
        public List<Argument<?>> getRemainingArguments() {
            return Collections3.listOf(this.typeArgument);
        }

        @NotNull
        @Override
        public List<Argument<?>> getAll() {
            return Collections3.listOf(this.typeArgument, this.listArgument, this.mapArgument);
        }
    }
}
