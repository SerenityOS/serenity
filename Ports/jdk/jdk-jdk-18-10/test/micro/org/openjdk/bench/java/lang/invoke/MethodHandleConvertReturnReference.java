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
 * Benchmark assesses runtime argument conversion performance for MethodHandles.
 * This particular test checks the return type conversion: void -> Integer.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class MethodHandleConvertReturnReference {

    /*
     * Implementation notes:
     *
     * Baseline is invokeExact call, which presumably measures the performance without argument conversion.
     *
     * The test is subdivided into three subtests, gradually doing more work:
     *   - 1_Convert: calls MH.asType to do actual conversion
     *   - 2_MTConvert: instantiates MT, and then calls MH.asType to do actual conversion
     *   - 3_Call: calls MH.invoke, requesting argument conversion
     *
     * Calling virtual method to guarantee side-effects on value, without trading off scalability.
     * This test should be executed in non-shared mode.
     */

    public int value;

    private MethodHandle mh;
    private MethodType newType;

    @Setup
    public void setup() throws Throwable {
        mh = MethodHandles.lookup().findVirtual(MethodHandleConvertReturnReference.class, "target", MethodType.methodType(void.class));
        newType = MethodType.methodType(Integer.class, MethodHandleConvertReturnReference.class);
    }

    @Benchmark
    public void baselineExact() throws Throwable {
        mh.invokeExact(this);
    }

    @Benchmark
    public MethodHandle test_1_Convert() throws Throwable {
        return mh.asType(newType);
    }

    @Benchmark
    public MethodHandle test_2_MTConvert() throws Throwable {
        return mh.asType(MethodType.methodType(Integer.class, MethodHandleConvertReturnReference.class));
    }

    @Benchmark
    public Object test_3_Call() throws Throwable {
        return mh.invoke(this);
    }

    public void target() {
        value++;
    }

}
