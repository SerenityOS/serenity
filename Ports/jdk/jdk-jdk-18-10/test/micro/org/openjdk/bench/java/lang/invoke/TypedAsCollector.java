/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@State(org.openjdk.jmh.annotations.Scope.Thread)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
public class TypedAsCollector {

    static final MethodHandle MH_COLLECT_OBJECT = MethodHandles.identity(Object[].class).asCollector(Object[].class, 3);
    static final MethodHandle MH_COLLECT_STRING = MethodHandles.identity(String[].class).asCollector(String[].class, 3);
    static final MethodHandle MH_COLLECT_INT = MethodHandles.identity(int[].class).asCollector(int[].class, 3);

    // uses a different code path to construct the collector
    static final MethodHandle MH_COLLECT_OBJECT_HA = MethodHandles.identity(Object[].class).asCollector(Object[].class, 12);
    static final MethodHandle MH_COLLECT_STRING_HA = MethodHandles.identity(String[].class).asCollector(String[].class, 12);
    static final MethodHandle MH_COLLECT_INT_HA = MethodHandles.identity(int[].class).asCollector(int[].class, 12);

    @Benchmark
    public Object[] testObjectCollect() throws Throwable {
        return (Object[]) MH_COLLECT_OBJECT.invokeExact((Object) "A", (Object) "B", (Object) "C");
    }

    @Benchmark
    public Object[] testStringCollect() throws Throwable {
        return (String[]) MH_COLLECT_STRING.invokeExact("A", "B", "C");
    }

    @Benchmark
    public int[] testIntCollect() throws Throwable {
        return (int[]) MH_COLLECT_INT.invokeExact(1, 2, 3);
    }

    @Benchmark
    public Object[] testObjectCollectHighArity() throws Throwable {
        return (Object[]) MH_COLLECT_OBJECT_HA.invokeExact(
                (Object) "A", (Object) "B", (Object) "C", (Object) "D", (Object) "E", (Object) "F",
                (Object) "G", (Object) "H", (Object) "I", (Object) "J", (Object) "K", (Object) "L");
    }

    @Benchmark
    public Object[] testStringCollectHighArity() throws Throwable {
        return (String[]) MH_COLLECT_STRING_HA.invokeExact(
                 "A",  "B",  "C",  "D",  "E",  "F",
                 "G",  "H",  "I",  "J",  "K",  "L");
    }

    @Benchmark
    public int[] testIntCollectHighArity() throws Throwable {
        return (int[]) MH_COLLECT_INT_HA.invokeExact(
                1, 2, 3, 4, 5, 6,
                7, 8, 9, 10, 11, 12);
    }

}