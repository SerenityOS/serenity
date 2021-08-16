/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.bench.java.lang.invoke;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.concurrent.TimeUnit;

/**
 * Benchmark assesses MethodHandles.filterArguments() performance
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class MethodHandlesFilterArgs {

    /**
     * Implementation notes:
     *   - calling static method to have consistent arg list without receiver type
     *   - using volatile ints as arguments to prevent opportunistic optimizations
     *   - using Integers to limit argument conversion costs
     *   - the filter is empty to measure infra costs, not the filter itself
     *   - baselines should be comparable for each method if optimized enough
     */

    private MethodHandle orig;
    private MethodHandle modified1;
    private MethodHandle modified2;
    private MethodHandle modified3;

    private volatile Integer arg1 = 42;
    private volatile Integer arg2 = 43;
    private volatile Integer arg3 = 44;

    @Setup
    public void setup() throws Throwable {
        orig = MethodHandles.lookup().findStatic(MethodHandlesFilterArgs.class, "doWork", MethodType.methodType(int.class, Integer.class, Integer.class, Integer.class));
        MethodHandle filter = MethodHandles.lookup().findStatic(MethodHandlesFilterArgs.class, "filter", MethodType.methodType(Integer.class, Integer.class));
        modified1 = MethodHandles.filterArguments(orig, 0, filter);
        modified2 = MethodHandles.filterArguments(orig, 0, filter, filter);
        modified3 = MethodHandles.filterArguments(orig, 0, filter, filter, filter);
    }

    @Benchmark
    public int baselineRaw() throws Throwable {
        return doWork(arg1, arg2, arg3);
    }

    @Benchmark
    public int baselineRawConvert() throws Throwable {
        return doWork(filter(arg1), filter(arg2), filter(arg3));
    }

    @Benchmark
    public int baselineMH() throws Throwable {
        return (int) orig.invokeExact(arg1, arg2, arg3);
    }

    @Benchmark
    public int baselineMHConvert() throws Throwable {
        return (int) orig.invokeExact(filter(arg1), filter(arg2), filter(arg3));
    }

    @Benchmark
    public int testInvoke_M1() throws Throwable {
        return (int) modified1.invokeExact(arg1, arg2, arg3);
    }

    @Benchmark
    public int testInvoke_M2() throws Throwable {
        return (int) modified2.invokeExact(arg1, arg2, arg3);
    }

    @Benchmark
    public int testInvoke_M3() throws Throwable {
        return (int) modified3.invokeExact(arg1, arg2, arg3);
    }

    public static Integer filter(Integer a) {
        return a;
    }

    public static int doWork(Integer a, Integer b, Integer c) {
        return 31*(31*(31*a + b) + c);
    }

}
