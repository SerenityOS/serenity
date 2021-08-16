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
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * Benchmark to assess basic MethodHandle performance.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class MethodHandleBasicInvoke {

    /*
     * Implementation notes:
     *   - this is a very basic test, does not do any parameter conversion (in fact, no parameters at all)
     *   - baselines include calling method directly, and doing the same via reflection
     *   - baselineRaw is known to be super-fast with good inlining
     */

    private int i;
    private static MethodHandle mh;
    private static Method ref;
    private static MethodHandle mhUnreflect;

    @Setup
    public void setup() throws Throwable {
        mh = MethodHandles.lookup().findVirtual(MethodHandleBasicInvoke.class, "doWork", MethodType.methodType(int.class));

        ref = MethodHandleBasicInvoke.class.getMethod("doWork");
        ref.setAccessible(true);

        mhUnreflect = MethodHandles.lookup().unreflect(ref);
    }

    @Benchmark
    public int baselineRaw() throws Throwable {
        return doWork();
    }

    @Benchmark
    public int baselineReflect() throws Throwable {
        return (int) ref.invoke(this);
    }

    @Benchmark
    public int testMH_Plain_Invoke() throws Throwable {
        return (int) mh.invoke(this);
    }

    @Benchmark
    public int testMH_Plain_Exact() throws Throwable {
        return (int) mh.invokeExact(this);
    }

    @Benchmark
    public int testMH_Unreflect_Invoke() throws Throwable {
        return (int) mhUnreflect.invoke(this);
    }

    @Benchmark
    public int testMH_Unreflect_Exact() throws Throwable {
        return (int) mhUnreflect.invokeExact(this);
    }

    public int doWork() {
        return i++;
    }

}
