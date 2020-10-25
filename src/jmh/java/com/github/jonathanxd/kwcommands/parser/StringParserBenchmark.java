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
package com.github.jonathanxd.kwcommands.parser;

import com.github.jonathanxd.iutils.type.TypeInfo;
import com.github.jonathanxd.kwcommands.argument.ListArgumentType;
import com.github.jonathanxd.kwcommands.argument.MapArgumentType;
import com.github.jonathanxd.kwcommands.util.CommonArgTypesKt;
import com.github.jonathanxd.kwcommands.util.IndexedSourcedCharIter;
import com.github.jonathanxd.kwcommands.util.SourcedCharIterator;
import com.github.jonathanxd.kwcommands.util.StringParseKt;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.List;
import java.util.Map;

@State(Scope.Benchmark)
@Warmup(iterations = 10)
@Fork(5)
public class StringParserBenchmark {

    private SourcedCharIterator iter;
    private SourcedCharIterator mapIter;
    private SourcedCharIterator listIter;

    private MapArgumentType<Object, Object> mapArgType;
    private ListArgumentType<Object> listArgumentType;

    @Setup
    public void setup() {
        this.iter = new IndexedSourcedCharIter("HelloWorld");
        this.mapIter = new IndexedSourcedCharIter("{a=hello, b=world, c=[uau, this, is, a, list]}");
        this.listIter = new IndexedSourcedCharIter("[uau, this, is, a, list]");
        this.mapArgType = new MapArgumentType<>(
                CommonArgTypesKt.getAnyArgumentType(),
                CommonArgTypesKt.getAnyArgumentType(),
                TypeInfo.builderOf(Map.class).of(Object.class, Object.class).<Map<Object, Object>>buildGeneric()
        );
        this.listArgumentType = new ListArgumentType<>(
                CommonArgTypesKt.getAnyArgumentType(),
                TypeInfo.builderOf(List.class).of(Object.class).<List<Object>>buildGeneric()
        );
    }

    private <T> T fail() {
        throw new IllegalStateException();
    }

    @Benchmark
    public void parserBench() {
        this.iter.restore(SourcedCharIterator.Companion.getStateZero());
        StringParseKt.parseSingleInput(this.iter/*, CommonArgTypesKt.getStringArgumentType()*/)
                .rightOrGet(this::fail);
    }

    @Benchmark
    public void mapParserBench() {
        this.mapIter.restore(SourcedCharIterator.Companion.getStateZero());
        StringParseKt.parseMapInput(this.mapIter/*, this.mapArgType*/)
                .rightOrGet(this::fail);
    }

    @Benchmark
    public void listParserBench() {
        this.listIter.restore(SourcedCharIterator.Companion.getStateZero());
        StringParseKt.parseListInput(this.listIter/*, this.listArgumentType*/)
                .rightOrGet(this::fail);
    }

}
