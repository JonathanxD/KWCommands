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

import org.junit.Test;

import java.util.List;
import java.util.Map;

import kotlin.jvm.functions.Function0;

public class CompletionTest {

    @Test
    public void test() {
        AIO aio = KWCommands.INSTANCE.createAio();
        CommandManager commandManager = aio.getCommandManager();
        CommandProcessor processor = aio.getProcessor();
        ReflectionEnvironment reflectionEnvironment = aio.getReflectionEnvironment();
        InformationManager informationManager = new InformationManagerImpl();
        Completion completion = aio.getCompletion();

        //a b c [a
        // a = command
        // b = command
        // c = argument
        // [a = argument

        List<Command> commands = reflectionEnvironment.fromClass(CompletionTest.class, aClass -> this, this);

        commandManager.registerAll(commands, this);

        KLocale.INSTANCE.getLocalizer().setDefaultLocale(KLocale.INSTANCE.getPtBr());
        HelpInfoHandler localizedHandler = new CommonHelpInfoHandler();
        Printer localizedSysOutWHF = Printers.INSTANCE.getSysOutWHF();

        List<String> complete;

        /*String s00 = "mapcmd 1 --values {a=\"man, i l u = {\", uhu=[s,b]} --n2 2";
        String s0 = "mapcmd 1 --values {a=\"man, i l u = {\", uhu=[s,b]";
        String s = "mapcmd 1 --values {a=\"man, i l u = {\", A=";

        complete = completion.complete(s, null);

        System.out.println("== Complete ==");

        for (String s1 : complete) {
            System.out.println(s1);
        }

        System.out.println("== Complete ==");*/


        String x;
/*
        x = "mapcmd 1 --values {a=\"man, i l u = {\", ";
        complete = completion.complete(x, null);

        System.out.println("== Complete ==");

        for (String s1 : complete) {
            System.out.println(s1);
        }

        System.out.println("== Complete ==");
*/

        x = "setmap 1 --values {name=Jonathan,values={age=18,languages=[a";
        complete = completion.complete(x, null);

        System.out.println("== Complete ==");

        for (String s1 : complete) {
            System.out.println(s1);
        }

        System.out.println("== Complete ==");
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

    public static class MyMapArgTypeProvider implements Function0<ArgumentType<?, ?>> {

        public static final MyMapArgTypeProvider INSTANCE = new MyMapArgTypeProvider();

        @Override
        public ArgumentType<?, ?> invoke() {
            return MyMapValidatorKt.getMyMapArgumentType();
        }
    }

    public static enum E {
        A,
        B
    }

    public static enum X {
        C,
        D
    }


}
