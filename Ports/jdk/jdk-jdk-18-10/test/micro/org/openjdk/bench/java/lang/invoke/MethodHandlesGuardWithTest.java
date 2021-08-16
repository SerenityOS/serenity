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
 * Benchmark assesses MethodHandles.guardWithTest() performance
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class MethodHandlesGuardWithTest {

    /**
     * Implementation notes:
     *   - using volatile ints as arguments to prevent opportunistic optimizations
     *   - using Integers to limit argument conversion costs
     *   - tested method should perform no worse than the baseline
     */

    private MethodHandle mhWork1;
    private MethodHandle mhWork2;
    private MethodHandle guard;
    private boolean choice;

    private volatile Integer arg1 = 42;
    private volatile Integer arg2 = 43;
    private volatile Integer arg3 = 44;

    @Setup
    public void setup() throws Throwable {
        MethodType mt = MethodType.methodType(int.class, Integer.class, Integer.class, Integer.class);
        mhWork1 = MethodHandles.lookup().findVirtual(MethodHandlesGuardWithTest.class, "doWork1", mt);
        mhWork2 = MethodHandles.lookup().findVirtual(MethodHandlesGuardWithTest.class, "doWork2", mt);

        MethodHandle chooser = MethodHandles.lookup().findVirtual(MethodHandlesGuardWithTest.class, "chooser", MethodType.methodType(boolean.class));
        guard = MethodHandles.guardWithTest(chooser, mhWork1, mhWork2);
    }

    @Benchmark
    public int baselineManual() throws Throwable {
        if (choice) {
            return (int) mhWork1.invokeExact(this, arg1, arg2, arg3);
        } else {
            return (int) mhWork2.invokeExact(this, arg1, arg2, arg3);
        }
    }

    @Benchmark
    public int testInvoke() throws Throwable {
        return (int) guard.invoke(this, arg1, arg2, arg3);
    }

    public boolean chooser() {
        choice = !choice;
        return choice;
    }

    public int doWork1(Integer a, Integer b, Integer c) {
        return 31*(31*(31*a + b) + c);
    }

    public int doWork2(Integer a, Integer b, Integer c) {
        return 31*(31*(31*c + b) + a);
    }
}
