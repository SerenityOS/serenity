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
 * Benchmark assesses MethodHandles.insertArguments() performance
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class MethodHandlesInsertArguments {

    /**
     * Implementation notes:
     *   - this test is similar to MethodHandleBindToCurryBench
     *   - calling static method to have consistent arg list without receiver type
     *   - using volatile ints as arguments to prevent opportunistic optimizations
     *   - using Integers to limit argument conversion costs
     */

    private MethodHandle mhOrig;
    private MethodHandle mhCurry1;
    private MethodHandle mhCurry2;
    private MethodHandle mhCurry3;

    private volatile Integer arg1 = 42;
    private volatile Integer arg2 = 43;
    private volatile Integer arg3 = 44;

    @Setup
    public void setup() throws Throwable {
        mhOrig = MethodHandles.lookup().findStatic(MethodHandlesInsertArguments.class, "doWork", MethodType.methodType(int.class, Integer.class, Integer.class, Integer.class));
        mhCurry1 = MethodHandles.insertArguments(mhOrig, 0, arg1);
        mhCurry2 = MethodHandles.insertArguments(mhOrig, 0, arg1, arg2);
        mhCurry3 = MethodHandles.insertArguments(mhOrig, 0, arg1, arg2, arg3);
    }

    @Benchmark
    public int baselineRaw() throws Throwable {
        return doWork(arg1, arg2, arg3);
    }

    @Benchmark
    public int invoke_C0() throws Throwable {
        return (int) mhOrig.invokeExact(arg1, arg2, arg3);
    }

    @Benchmark
    public int invoke_C1() throws Throwable {
        return (int) mhCurry1.invokeExact(arg2, arg3);
    }

    @Benchmark
    public int invoke_C2() throws Throwable {
        return (int) mhCurry2.invokeExact(arg3);
    }

    @Benchmark
    public int invoke_C3() throws Throwable {
        return (int) mhCurry3.invokeExact();
    }

    public static int doWork(Integer a, Integer b, Integer c) {
        return 31*(31*(31*a + b) + c);
    }

}
