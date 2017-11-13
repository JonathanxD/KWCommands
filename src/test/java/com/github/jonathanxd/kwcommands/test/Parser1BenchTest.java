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
import com.github.jonathanxd.kwcommands.manager.CommandManager;
import com.github.jonathanxd.kwcommands.manager.CommandManagerImpl;
import com.github.jonathanxd.kwcommands.manager.InformationManagerVoid;
import com.github.jonathanxd.kwcommands.parser.CommandParser;
import com.github.jonathanxd.kwcommands.parser.CommandParserImpl;
import com.github.jonathanxd.kwcommands.processor.Processors;
import com.github.jonathanxd.kwcommands.reflect.annotation.Arg;
import com.github.jonathanxd.kwcommands.reflect.annotation.Cmd;
import com.github.jonathanxd.kwcommands.reflect.env.ReflectionEnvironment;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class Parser1BenchTest {
    private CommandManager manager;
    private ReflectionEnvironment environment;
    private CommandParser parser;
    private List<String> cmd;

    @Before
    public void setup() {
        this.manager = new CommandManagerImpl();
        this.environment = new ReflectionEnvironment(this.manager);
        this.parser = new CommandParserImpl(this.manager);
        this.manager.registerAll(
                this.environment.fromClass(Parser1BenchTest.class, c -> new Parser1BenchTest(), this),
                this
        );
        this.cmd = Collections3.listOf("bench", "9", "a", "b", "c", "--types", "simple", "unknown");
    }

    @Test
    public void parserBench() {
        this.parser.parse(this.cmd, this);
    }

    @Cmd(description = "Bench test")
    public Integer bench(@Arg("n") int n,
                         @Arg(value = "names", multiple = true) List<String> names,
                         @Arg(value = "type", multiple = true) List<Type> types) {
        return n + names.size() + types.size();
    }

    enum Type {
        SIMPLE,
        COMPLEX,
        UNKNOWN
    }

}
