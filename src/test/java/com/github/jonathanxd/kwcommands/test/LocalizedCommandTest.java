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

import com.github.jonathanxd.iutils.collection.Collections3;
import com.github.jonathanxd.iutils.localization.Locale;
import com.github.jonathanxd.iutils.localization.json.JsonLocaleLoader;
import com.github.jonathanxd.iutils.object.Either;
import com.github.jonathanxd.iutils.text.localizer.TextLocalizer;
import com.github.jonathanxd.kwcommands.AIO;
import com.github.jonathanxd.kwcommands.KWCommands;
import com.github.jonathanxd.kwcommands.command.Command;
import com.github.jonathanxd.kwcommands.fail.ParseFail;
import com.github.jonathanxd.kwcommands.information.InformationProvidersVoid;
import com.github.jonathanxd.kwcommands.json.CmdJson;
import com.github.jonathanxd.kwcommands.printer.Printers;
import com.github.jonathanxd.kwcommands.processor.CommandResult;
import com.github.jonathanxd.kwcommands.processor.ValueResult;
import com.github.jonathanxd.kwcommands.reflect.annotation.Arg;
import com.github.jonathanxd.kwcommands.reflect.annotation.Cmd;
import com.github.jonathanxd.kwcommands.util.KLocale;
import com.github.jonathanxd.kwcommands.util.LocalizerKt;
import com.github.jonathanxd.kwcommands.util.PrinterKt;

import org.junit.Assert;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

public class LocalizedCommandTest {

    @Test
    public void localizedTest() {
        AIO aio = KWCommands.INSTANCE.createAio(this)
                .loadObj(this)
                .registerLoaded();

        TextLocalizer localizer = KLocale.INSTANCE.getLocalizer();

        Locale locale = localizer.getLocale();

        Path path = Paths.get("test_lang");

        JsonLocaleLoader.JSON_LOCALE_LOADER
                .loadFromResource(locale, path, this.getClass().getClassLoader());

        Either<ParseFail, List<CommandResult>> first_second =
                aio.parseAndDispatch("first__ second 2", InformationProvidersVoid.INSTANCE,
                        localizer);

        PrinterKt.handleFailAndThrow(aio.getHelp(), first_second, Printers.INSTANCE.getSysOutWHF());
        Assert.assertTrue(first_second.isRight());
        List<CommandResult> right = first_second.getRight();
        Assert.assertEquals(2, right.size());
        CommandResult commandResult1 = right.get(0);
        CommandResult commandResult2 = right.get(1);

        Assert.assertTrue(commandResult1 instanceof ValueResult);
        Assert.assertTrue(commandResult2 instanceof ValueResult);

        Object v1 = ((ValueResult) commandResult1).getValue();
        Object v2 = ((ValueResult) commandResult2).getValue();

        Assert.assertEquals(1, v1);
        Assert.assertEquals(2, v2);

        PrinterKt.handleFailAndThrow(aio.getHelp(),
                aio.parseAndDispatch("fst second 2", InformationProvidersVoid.INSTANCE,
                        localizer),
                Printers.INSTANCE.getSysOutWHF());

        PrinterKt.handleFailAndThrow(aio.getHelp(),
                aio.parseAndDispatch("frs second 2", InformationProvidersVoid.INSTANCE,
                        localizer),
                Printers.INSTANCE.getSysOutWHF());

        Command first = Objects.requireNonNull(aio.getCommandManager().getCommand("first", null, localizer));

        Assert.assertEquals(Collections3.listOf("frs", "fst"), LocalizerKt.resolveAliasComponent(first, localizer));

        Command test = Objects.requireNonNull(aio.getCommandManager().getCommand("test", null, localizer));

        Assert.assertEquals("first__", LocalizerKt.resolveNameComponent(test, localizer));
        Assert.assertEquals("Love yourself", LocalizerKt.resolveDescription(test, localizer));
        Assert.assertEquals(Collections3.listOf("frs", "fst"), LocalizerKt.resolveAliasComponent(test, localizer));
    }


    @Cmd(nameComponent = "#command.first.name", aliasComponent = "#command.first.alias")
    public int first(@Arg(value = "name", optional = true) String name) {
        return 1;
    }

    @Cmd
    public int second(@Arg(value = "number") int n) {
        return 2;
    }

    @CmdJson("/test.json")
    public int test() {
        return 3;
    }
}
