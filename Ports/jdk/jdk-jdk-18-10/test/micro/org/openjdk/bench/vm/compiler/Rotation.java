/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2021, Arm Limited. All rights reserved.
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

package org.openjdk.bench.vm.compiler;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Fork(value = 3)
@Measurement(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@CompilerControl(CompilerControl.Mode.DONT_INLINE)
public class Rotation {

    private static final int COUNT = 5000;

    @State(Scope.Benchmark)
    public static class MyState {
        public int xi = 10;
        public int yi = 24;
    }

    @Benchmark
    public void xorRotateRight(MyState s, Blackhole blackhole) {
        int x = s.xi;
        int y = s.yi;
        for (int i = 0; i < COUNT; i++) {
            y = x ^ ((y >>> 5) | (y << -5));
        }
        blackhole.consume(y);
    }

    @Benchmark
    public void bicRotateRight(MyState s, Blackhole blackhole) {
        int x = s.xi;
        int y = s.yi;
        for (int i = 0; i < COUNT; i++) {
            y = x & (-1 ^ ((y >>> 5) | (y << -5)));
        }
        blackhole.consume(y);
    }

    @Benchmark
    public void eonRotateRight(MyState s, Blackhole blackhole) {
        int x = s.xi;
        int y = s.yi;
        for (int i = 0; i < COUNT; i++) {
            y = x ^ (-1 ^ ((y >>> 5) | (y << -5)));
        }
        blackhole.consume(y);
    }

    @Benchmark
    public void ornRotateRight(MyState s, Blackhole blackhole) {
        int x = s.xi;
        int y = s.yi;
        for (int i = 0; i < COUNT; i++) {
            y = x | (-1 ^ ((y >>> 5) | (y << -5)));
        }
        blackhole.consume(y);
    }

    @Benchmark
    public void andRotateRight(MyState s, Blackhole blackhole) {
        int x = s.xi;
        int y = s.yi;
        for (int i = 0; i < COUNT; i++) {
            y = x & ((y >>> 5) | (y << -5));
        }
        blackhole.consume(y);
    }
}
