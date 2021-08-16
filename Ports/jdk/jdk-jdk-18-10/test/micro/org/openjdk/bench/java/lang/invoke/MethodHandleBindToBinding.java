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
 * Benchmark assesses MethodHandle.bindTo() binding performance
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class MethodHandleBindToBinding {

    /*
       Implementation notes:
          - calls static method to avoid binding "this"
          - tests binding costs with multiple depth (C1, C2, C3)
          - baseline includes naive side effect store
          - lowering the binding cost will minimise both the spread between C1/C2/C3 and difference towards baseline
          - this test performance will never reach the baseline
     */

    private MethodHandle mhOrig;

    @Setup
    public void setup() throws Throwable {
        mhOrig = MethodHandles.lookup().findStatic(MethodHandleBindToBinding.class, "doWork",
                MethodType.methodType(Integer.class, Integer.class, Integer.class, Integer.class));
    }

    @Benchmark
    public Object baselineRaw() {
        return mhOrig;
    }

    @Benchmark
    public Object testBind_C1() throws Throwable {
        MethodHandle mhCurry1 = mhOrig.bindTo(1);
        return mhCurry1;
    }

    @Benchmark
    public Object testBind_C2() throws Throwable {
        MethodHandle mhCurry1 = mhOrig.bindTo(1);
        MethodHandle mhCurry2 = mhCurry1.bindTo(2);
        return mhCurry2;
    }

    @Benchmark
    public Object testBind_C3() throws Throwable {
        MethodHandle mhCurry1 = mhOrig.bindTo(1);
        MethodHandle mhCurry2 = mhCurry1.bindTo(2);
        MethodHandle mhCurry3 = mhCurry2.bindTo(3);
        return mhCurry3;
    }

    public static Integer doWork(Integer a, Integer b, Integer c) {
        return 31*(31*(31*a + b) + c);
    }

}
