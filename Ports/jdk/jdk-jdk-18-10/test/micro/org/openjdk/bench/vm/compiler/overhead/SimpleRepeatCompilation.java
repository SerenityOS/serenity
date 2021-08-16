/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

package org.openjdk.bench.vm.compiler.overhead;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.util.concurrent.TimeUnit;
import java.util.Arrays;

/**
 * The purpose of these microbenchmarks is to use RepeatCompilation
 * to produce a benchmark that focuses on the overhead of various JIT
 * compilations themselves.
 */

@State(Scope.Benchmark)
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(value = 10, warmups = 1)
public class SimpleRepeatCompilation {

    public static final String MIXHASH_METHOD
     = "-XX:CompileCommand=option,org/openjdk/bench/vm/compiler/overhead/SimpleRepeatCompilation.mixHashCode,intx,RepeatCompilation,500";

    @Benchmark
    @Fork(jvmArgsAppend={"-Xbatch", MIXHASH_METHOD})
    public int mixHashCode_repeat() {
        return loop_hashCode();
    }

    @Benchmark
    @Fork(jvmArgsAppend={"-Xbatch", "-XX:-TieredCompilation", MIXHASH_METHOD})
    public int mixHashCode_repeat_c2() {
        return loop_hashCode();
    }

    @Benchmark
    @Fork(jvmArgsAppend={"-Xbatch", "-XX:TieredStopAtLevel=1", MIXHASH_METHOD})
    public int mixHashCode_repeat_c1() {
        return loop_hashCode();
    }

    @Benchmark
    @Fork(jvmArgsAppend={"-Xbatch"})
    public int mixHashCode_baseline() {
        return loop_hashCode();
    }

    public int loop_hashCode() {
        int value = 0;
        for (int i = 0; i < 1_000_000; i++) {
            value += mixHashCode("simple_string");
        }
        return value;
    }

    public int mixHashCode(String value) {
        int h = value.hashCode();
        for (int i = 0; i < value.length(); i++) {
            h = value.charAt(i) ^ h;
        }
        return h;
    }

    public static final String TRIVIAL_MATH_METHOD
     = "-XX:CompileCommand=option,org/openjdk/bench/vm/compiler/overhead/SimpleRepeatCompilation.trivialMath,intx,RepeatCompilation,2000";

    @Benchmark
    @Fork(jvmArgsAppend={"-Xbatch",TRIVIAL_MATH_METHOD})
    public int trivialMath_repeat() {
        return loop_trivialMath();
    }

    @Benchmark
    @Fork(jvmArgsAppend={"-Xbatch", "-XX:-TieredCompilation", TRIVIAL_MATH_METHOD})
    public int trivialMath_repeat_c2() {
        return loop_trivialMath();
    }

    @Benchmark
    @Fork(jvmArgsAppend={"-Xbatch", "-XX:TieredStopAtLevel=1", TRIVIAL_MATH_METHOD})
    public int trivialMath_repeat_c1() {
        return loop_trivialMath();
    }

    @Benchmark
    @Fork(jvmArgsAppend={"-Xbatch"})
    public int trivialMath_baseline() {
        return loop_trivialMath();
    }

    public int loop_trivialMath() {
        int value = 0;
        for (int i = 0; i < 1_000_000; i++) {
            value += trivialMath(i, i - 1);
        }
        return value;
    }

    public int trivialMath(int a, int b) {
        return a * b + a;
    }


    public static final String LARGE_METHOD
     = "-XX:CompileCommand=option,org/openjdk/bench/vm/compiler/overhead/SimpleRepeatCompilation.largeMethod,intx,RepeatCompilation,100";

    @Benchmark
    @Fork(jvmArgsAppend={"-Xbatch",LARGE_METHOD})
    public int largeMethod_repeat() {
        return loop_largeMethod();
    }

    @Benchmark
    @Fork(jvmArgsAppend={"-Xbatch", "-XX:-TieredCompilation", LARGE_METHOD})
    public int largeMethod_repeat_c2() {
        return loop_largeMethod();
    }

    @Benchmark
    @Fork(jvmArgsAppend={"-Xbatch", "-XX:TieredStopAtLevel=1", LARGE_METHOD})
    public int largeMethod_repeat_c1() {
        return loop_largeMethod();
    }

    @Benchmark
    @Fork(jvmArgsAppend={"-Xbatch"})
    public int largeMethod_baseline() {
        return loop_largeMethod();
    }

    public int loop_largeMethod() {
        int value = 0;
        for (int i = 0; i < 50_000; i++) {
            value += largeMethod(i, i - 1);
        }
        return value;
    }

    // Meaningless but largish method with plenty of locals
    // to put more stress on register allocation
    public int largeMethod(int a, int b) {
        int c = b + 17;
        int d = a + b + 6;
        int e = c + d + 99;
        int f = d + e + 919;
        long val = 0;
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                for (int k = 0; k < 2; k++) {
                    a -= b;
                    b += c;
                    c -= d;
                    d += e;
                    e -= a;
                    val = a - b + c - d + e - f;
                }
            }
        }
        int g = b;
        int h = a;
        int l = c;
        int m = d;
        int n = d;
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                for (int k = 0; k < 2; k++) {
                    g += b;
                    h -= c;
                    l += d;
                    m -= e;
                    e += a;
                    val = g + h + l + m + n;
                }
            }
        }
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                for (int k = 0; k < 2; k++) {
                    a -= b;
                    b += c;
                    c -= d;
                    d += e;
                    e -= a;
                    val = a - b - c - d - e - f;
                }
            }
        }
        int o = b;
        int p = a;
        int q = c;
        int r = d;
        int s = d;
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                for (int k = 0; k < 2; k++) {
                    o += b;
                    p += c;
                    q += d;
                    r += e;
                    s += a;
                    val = o + p + q + r + s;
                }
            }
        }
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                for (int k = 0; k < 2; k++) {
                    g += b;
                    h -= c;
                    l += d;
                    m -= e;
                    e += a;
                    val = g + h + l + m + n;
                }
            }
        }
        return (int)(val ^ (val >> 32L));
    }
}
