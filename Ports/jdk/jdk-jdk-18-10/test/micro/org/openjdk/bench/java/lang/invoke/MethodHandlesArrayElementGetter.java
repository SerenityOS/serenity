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
import java.util.concurrent.TimeUnit;

/**
 * Benchmark assesses the performance of MethodHandles.arrayElementGetter
 *
 * @author Aleksey Shipilev (aleksey.shipilev@oracle.com)
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class MethodHandlesArrayElementGetter {

    /**
     * Implementation notes:
     *   - creating simple array, and accessing the middle element
     *   - might have done iteration over array, but that will measure pipelining effects instead
     *   - volatile modifier on array breaks the DCE, which would otherwise eliminate the array load
     *   - the rationale for array size and access pattern is coherent to SetterBench
     */

    private static final int SIZE = 1024;
    private static final int POS = SIZE/2;

    private static MethodHandle mh;
    private volatile int[] array;

    @Setup
    public void setup() throws Throwable {
        array = new int[SIZE];
        for (int i = 0; i < SIZE; i++) {
            array[i] = i;
        }
        mh = MethodHandles.arrayElementGetter(int[].class);
    }

    @Benchmark
    public MethodHandle testCreate() {
        return MethodHandles.arrayElementGetter(int[].class);
    }

    @Benchmark
    public int baselineRaw() {
        return access(array, POS);
    }

    @Benchmark
    public int testGetter() throws Throwable {
        return (int) mh.invoke(array, POS);
    }

    public int access(int[] array, int pos) {
        return array[pos];
    }

}
