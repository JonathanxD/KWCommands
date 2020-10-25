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

import com.github.jonathanxd.iutils.object.Either;
import com.github.jonathanxd.kwcommands.AIO;
import com.github.jonathanxd.kwcommands.argument.Argument;
import com.github.jonathanxd.kwcommands.fail.ParseFail;
import com.github.jonathanxd.kwcommands.information.InformationProvidersVoid;
import com.github.jonathanxd.kwcommands.processor.CommandProcessor;
import com.github.jonathanxd.kwcommands.processor.CommandResult;
import com.github.jonathanxd.kwcommands.reflect.annotation.Arg;
import com.github.jonathanxd.kwcommands.reflect.annotation.Cmd;
import com.github.jonathanxd.kwcommands.util.CommandResultAssertKt;
import com.github.jonathanxd.kwcommands.util.ThrownKt;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Objects;

public class ShortNameTest {

    @Test
    public void shortNameTest() {
        AIO aio = new AIO(this)
                .loadObj(this)
                .registerLoaded();

        CommandProcessor processor = aio.getProcessor();
        Either<ParseFail, List<CommandResult>> parse = processor.parseAndDispatch(
                "rm -rf hello", this, InformationProvidersVoid.INSTANCE);

        this.assertParse(parse);

        parse = processor.parseAndDispatch(
                "rm -rf --path hello", this, InformationProvidersVoid.INSTANCE);

        this.assertParse(parse);

        // Not supported yet, issue #20
        /*parse = processor.parseAndDispatch(
                "rm -rf --path=hello", this, InformationProvidersVoid.INSTANCE);

        this.assertParse(parse);*/
    }

    private void assertParse(Either<ParseFail, List<CommandResult>> parse) {
        if (parse.isLeft()) {
            ThrownKt.thrown(parse.getLeft());
        } else {
            Assert.assertFalse(parse.getRight().isEmpty());

            CommandResultAssertKt.assertArguments(parse.getRight(), argumentContainer -> {
                Argument<?> argument = argumentContainer.getArgument();

                switch (argument.getName()) {
                    case "recursive":
                        return Objects.equals(argumentContainer.getValue(), Boolean.TRUE);
                    case "force":
                        return Objects.equals(argumentContainer.getValue(), Boolean.TRUE);
                    case "path":
                        return Objects.equals(argumentContainer.getValue(), "hello");
                }

                return false;
            }, (message) -> {
                Assert.fail((String) message);
                return null;
            });
        }
    }

    @Cmd
    public void rm(@Arg(value = "recursive", optional = true) boolean recursive,
                   @Arg(value = "force", optional = true) boolean force,
                   @Arg("path") String path) {
    }

}
