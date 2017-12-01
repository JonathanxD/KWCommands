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
package com.github.jonathanxd.kwcommands.parser;

import com.github.jonathanxd.kwcommands.manager.CommandManager;
import com.github.jonathanxd.kwcommands.manager.CommandManagerImpl;
import com.github.jonathanxd.kwcommands.reflect.annotation.Arg;
import com.github.jonathanxd.kwcommands.reflect.annotation.Cmd;
import com.github.jonathanxd.kwcommands.reflect.env.ReflectionEnvironment;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.List;

@State(Scope.Benchmark)
@Warmup(iterations = 5)
@Fork(5)
/*
Result "com.github.jonathanxd.kwcommands.parser.Parser2Benchmark.parserBench":
  215428.369 ±(99.9%) 4290.711 ops/s [Average]
  (min, avg, max) = (125469.768, 215428.369, 228256.324), stdev = 12651.260
  CI (99.9%): [211137.658, 219719.081] (assumes normal distribution)


# Run complete. Total time: 00:02:13

Benchmark                      Mode  Cnt       Score      Error  Units
Parser2Benchmark.parserBench  thrpt  100  215428.369 ± 4290.711  ops/s

On my machine

1.3:

Result "com.github.jonathanxd.kwcommands.parser.Parser2Benchmark.parserBench":
  49539.071 ±(99.9%) 1201.100 ops/s [Average]
  (min, avg, max) = (28773.418, 49539.071, 52600.516), stdev = 3541.470
  CI (99.9%): [48337.971, 50740.170] (assumes normal distribution)


# Run complete. Total time: 00:02:13

Benchmark                      Mode  Cnt      Score      Error  Units
Parser2Benchmark.parserBench  thrpt  100  49539.071 ± 1201.100  ops/s

 */
public class Parser2Benchmark {

    private CommandManager manager;
    private ReflectionEnvironment environment;
    private CommandParser parser;
    private String cmd;

    @Setup
    public void setup() {
        this.manager = new CommandManagerImpl();
        this.environment = new ReflectionEnvironment(this.manager);
        this.parser = new CommandParserImpl(this.manager);
        this.manager.registerAll(
                this.environment.fromClass(Parser2Benchmark.class, c -> new Parser2Benchmark(), this),
                this
        );
        this.cmd = "bench 9 a b c --types simple unknown";
    }

    @Benchmark
    public void parserBench() {
        this.parser.parse(this.cmd, this);
    }

    @Cmd(description = "Bench test")
    public Integer bench(@Arg("n") int n,
                         @Arg(value = "names", multiple = true) List<String> names,
                         @Arg(value = "types", multiple = true) List<Type> types) {
        return n + names.size() + types.size();
    }

    enum Type {
        SIMPLE,
        COMPLEX,
        UNKNOWN
    }

}
