/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.openjdk.bench.java.util;

import org.openjdk.jmh.annotations.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Microbenchmarks for List.of fixed vs varargs implementations.
 * Run with -Xint to avoid JIT compilation, in order to test
 * common use cases of these methods being called from static
 * initializers. Use parallel GC and set initial heap size to avoid
 * GC during runs.
 */
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(value = 3, jvmArgsAppend = { "-verbose:gc", "-XX:+UseParallelGC", "-Xms4g", "-Xmx4g", "-Xint" })
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
public class ListArgs {

    @Benchmark
    public Object list00() {
        return List.of();
    }

    @Benchmark
    public Object list01() {
        return List.of("a");
    }

    @Benchmark
    public Object list02() {
        return List.of("a", "b");
    }

    @Benchmark
    public Object list03() {
        return List.of("a", "b", "c");
    }

    @Benchmark
    public Object list04() {
        return List.of("a", "b", "c", "d");
    }

    @Benchmark
    public Object list05() {
        return List.of("a", "b", "c", "d", "e");
    }

    @Benchmark
    public Object list06() {
        return List.of("a", "b", "c", "d", "e",
                       "f");
    }

    @Benchmark
    public Object list07() {
        return List.of("a", "b", "c", "d", "e",
                       "f", "g");
    }

    @Benchmark
    public Object list08() {
        return List.of("a", "b", "c", "d", "e",
                       "f", "g", "h");
    }

    @Benchmark
    public Object list09() {
        return List.of("a", "b", "c", "d", "e",
                       "f", "g", "h", "i");
    }

    @Benchmark
    public Object list10() {
        return List.of("a", "b", "c", "d", "e",
                       "f", "g", "h", "i", "j");
    }

    @Benchmark
    public Object list11() {
        return List.of("a", "b", "c", "d", "e",
                       "f", "g", "h", "i", "j", "k");
    }
}
