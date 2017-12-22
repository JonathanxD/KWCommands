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
import com.github.jonathanxd.kwcommands.AIO;
import com.github.jonathanxd.kwcommands.KWCommands;
import com.github.jonathanxd.kwcommands.argument.ArgumentType;
import com.github.jonathanxd.kwcommands.command.Command;
import com.github.jonathanxd.kwcommands.completion.Completion;
import com.github.jonathanxd.kwcommands.help.CommonHelpInfoHandler;
import com.github.jonathanxd.kwcommands.help.HelpInfoHandler;
import com.github.jonathanxd.kwcommands.manager.CommandManager;
import com.github.jonathanxd.kwcommands.manager.InformationManager;
import com.github.jonathanxd.kwcommands.manager.InformationManagerImpl;
import com.github.jonathanxd.kwcommands.printer.Printer;
import com.github.jonathanxd.kwcommands.printer.Printers;
import com.github.jonathanxd.kwcommands.processor.CommandProcessor;
import com.github.jonathanxd.kwcommands.reflect.annotation.Arg;
import com.github.jonathanxd.kwcommands.reflect.annotation.Cmd;
import com.github.jonathanxd.kwcommands.reflect.env.ReflectionEnvironment;
import com.github.jonathanxd.kwcommands.util.KLocale;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import kotlin.collections.ArraysKt;
import kotlin.jvm.functions.Function0;

public class CompletionTest {

    @Test
    public void test() {
        AIO aio = KWCommands.INSTANCE.createAio(this);
        CommandManager commandManager = aio.getCommandManager();
        CommandProcessor processor = aio.getProcessor();
        ReflectionEnvironment reflectionEnvironment = aio.getReflectionEnvironment();
        InformationManager informationManager = new InformationManagerImpl();
        Completion completion = aio.getCompletion();

        List<Command> commands = reflectionEnvironment.fromClass(CompletionTest.class, aClass -> this, this);

        for (Command command : commands) {
            commandManager.registerCommand(command, this);
        }

        KLocale.INSTANCE.getLocalizer().setDefaultLocale(KLocale.INSTANCE.getPtBr());
        HelpInfoHandler localizedHandler = new CommonHelpInfoHandler();
        Printer localizedSysOutWHF = Printers.INSTANCE.getSysOutWHF();

        List<String> complete;

        String x;

        x = "mapcmd 1 --values {a=\"man, i l u = {\", ";
        complete = completion.complete(x, null, informationManager);

        Assert.assertEquals(Collections3.listOf(), complete);

        x = "mapcmd 1 --values {a=C, ";
        complete = completion.complete(x, null, informationManager);

        Assert.assertEquals(Collections3.listOf("A", "B"), complete);

        x = "setmap 1 --values {name=Jonathan,values={age=18,languages=";
        complete = completion.complete(x, null, informationManager);

        Assert.assertEquals(Collections3.listOf("["), complete);

        x = "setmap 1 --values {name=Jonathan,values={age=18,";
        complete = completion.complete(x, null, informationManager);

        Assert.assertEquals(Collections3.listOf("languages"), complete);

        x = "setmap 1 --values {name=Jonathan,values={age=18,languages=[";
        complete = completion.complete(x, null, informationManager);

        List<String> expected = Collections3.listOf("]");
        expected.addAll(ArraysKt.map(Languages.values(), Languages::name));
        Assert.assertEquals(expected, complete);

        x = "setmap 1 --values {name=Jonathan,values={age=18,languages=[Ja";
        complete = completion.complete(x, null, informationManager);

        Assert.assertEquals(
                ArraysKt.map(Languages.values(), Languages::name).stream()
                        .filter(name -> name.startsWith("Ja"))
                        .collect(Collectors.toList()),
                complete);

        x = "";
        complete = completion.complete(x, null, informationManager);

        Assert.assertTrue(complete.containsAll(Collections3.listOf("mapcmd", "setmap")));

        x = "m";
        complete = completion.complete(x, null, informationManager);

        Assert.assertEquals(
                Collections3.listOf("mapcmd"),
                complete);

        x = "mc";
        complete = completion.complete(x, null, informationManager);

        Assert.assertTrue(complete.containsAll(Collections3.listOf("mapcmd", "setmap")));

        x = "mapcmd";
        complete = completion.complete(x, null, informationManager);

        Assert.assertEquals(
                Collections3.listOf(" "),
                complete);

        x = "mapcmd ";
        complete = completion.complete(x, null, informationManager);

        Assert.assertEquals(
                Collections3.listOf("a", "--n", "--values", "--n2"),
                complete);

        x = "mapcmd 1";
        complete = completion.complete(x, null, informationManager);

        Assert.assertEquals(
                Collections3.listOf(" "),
                complete);

        x = "mapcmd 1 ";
        complete = completion.complete(x, null, informationManager);

        Assert.assertEquals(
                Collections3.listOf("--values", "--n2", "{"),
                complete);

        x = "mapcmd 1 {";
        complete = completion.complete(x, null, informationManager);

        Assert.assertEquals(
                Collections3.listOf("}", "A", "B"),
                complete);

        x = "mapcmd 1 { ";
        complete = completion.complete(x, null, informationManager);

        Assert.assertEquals(
                Collections3.listOf("}", "A", "B"),
                complete);

        x = "completeTest1";
        complete = completion.complete(x, null, informationManager);

        Assert.assertEquals(
                Collections3.listOf(" "),
                complete);

        x = "completeTest1 ";
        complete = completion.complete(x, null, informationManager);

        Assert.assertTrue(Collections3.listOf(
                "completeTest2", "completeTest2_5", "setmap", "testE",
                "testEOpt", "mapcmd"
        ).containsAll(complete));

        x = "completeTest1 completeTest2";
        complete = completion.complete(x, null, informationManager);

        Assert.assertEquals(
                Collections3.listOf(" "),
                complete);

        x = "completeTest1 completeTest2 ";
        complete = completion.complete(x, null, informationManager);

        Assert.assertTrue(Collections3.listOf("completeTest3", "completeTest4", "setmap",
                "testE", "testEOpt", "mapcmd").containsAll(complete));

        x = "completeTest1 completeTest2 completeTest3";
        complete = completion.complete(x, null, informationManager);

        Assert.assertEquals(
                Collections3.listOf(" "),
                complete);

        x = "completeTest1 completeTest2 completeTest4";
        complete = completion.complete(x, null, informationManager);

        Assert.assertEquals(
                Collections3.listOf(" "),
                complete);

        x = "completeTest1 completeTest2 completeTest4 ";
        complete = completion.complete(x, null, informationManager);

        Assert.assertTrue(
                Collections3.listOf("completeTest1", "mapcmd", "setmap", "testE", "testEOpt", "--name")
                .containsAll(complete));

        x = "testE";
        complete = completion.complete(x, null, informationManager);

        Assert.assertEquals(
                Collections3.listOf(" "),
                complete);

        x = "testE ";
        complete = completion.complete(x, null, informationManager);

        Assert.assertEquals(
                Collections3.listOf("--value", "AAC", "AXD", "BBA", "BBD"),
                complete);

        x = "testE  ";
        complete = completion.complete(x, null, informationManager);

        Assert.assertEquals(
                Collections3.listOf(),
                complete);

        x = "testE --value";
        complete = completion.complete(x, null, informationManager);

        Assert.assertEquals(
                Collections3.listOf(" "),
                complete);

        x = "testE --value ";
        complete = completion.complete(x, null, informationManager);

        Assert.assertEquals(
                Collections3.listOf("AAC", "AXD", "BBA", "BBD"),
                complete);

        x = "testE --value A";
        complete = completion.complete(x, null, informationManager);

        Assert.assertEquals(
                Collections3.listOf("AAC", "AXD"),
                complete);

        x = "testE --value AA";
        complete = completion.complete(x, null, informationManager);

        Assert.assertEquals(
                Collections3.listOf("AAC"),
                complete);

        x = "testE --value AX";
        complete = completion.complete(x, null, informationManager);

        Assert.assertEquals(
                Collections3.listOf("AXD"),
                complete);

        x = "testE --value AXD";
        complete = completion.complete(x, null, informationManager);

        Assert.assertEquals(
                Collections3.listOf(" "),
                complete);

        x = "testEOpt --value";
        complete = completion.complete(x, null, informationManager);

        Assert.assertEquals(
                Collections3.listOf(" "),
                complete);

        x = "testEOpt --value ";
        complete = completion.complete(x, null, informationManager);

        Assert.assertEquals(
                Collections3.listOf("AAC", "AXD", "BBA", "BBD"),
                complete);

    }

    @Cmd(description = "Complete Test 1")
    public void completeTest1() {

    }

    @Cmd(description = "Vararg test")
    public void mapcmd(@Arg("n") int n,
                       @Arg(value = "values", multiple = true) Map<E, X> values,
                       @Arg("n2") int n2) {

    }

    // setmap {nome=Jonathan,valores={idade=18, linguagens_principais=[Java, Kotlin]}

    @Cmd(description = "Set map test")
    public void setmap(@Arg("n") int n,
                       @Arg(value = "values", multiple = true,
                               argumentType = MyMapArgTypeProvider.class) Map<String, Object> values) {

    }

    @Cmd(parents = "mapcmd")
    public void a(@Arg("s") String s) {

    }

    @Cmd(description = "Complete Test 2", parents = {"completeTest1"})
    public void completeTest2() {

    }

    @Cmd(description = "Complete Test 2.5", parents = {"completeTest1"})
    public void completeTest2_5() {

    }

    @Cmd(description = "Complete Test 3", parents = {"completeTest1", "completeTest2"})
    public void completeTest3() {

    }

    @Cmd(description = "Complete Test 4", parents = {"completeTest1", "completeTest2"})
    public void completeTest4(@Arg(value = "name", optional = true) String name) {

    }

    @Cmd
    public void testE(@Arg("value") EX ex) {

    }

    @Cmd
    public void testEOpt(@Arg(value = "value", optional = true) EX ex) {

    }

    public static enum E {
        A,
        B
    }

    public static enum X {
        C,
        D
    }

    public static enum EX {
        AAC,
        AXD,
        BBA,
        BBD
    }

    public static class MyMapArgTypeProvider implements Function0<ArgumentType<?, ?>> {

        public static final MyMapArgTypeProvider INSTANCE = new MyMapArgTypeProvider();

        @Override
        public ArgumentType<?, ?> invoke() {
            return MyMapValidatorKt.getMyMapArgumentType();
        }
    }


}
