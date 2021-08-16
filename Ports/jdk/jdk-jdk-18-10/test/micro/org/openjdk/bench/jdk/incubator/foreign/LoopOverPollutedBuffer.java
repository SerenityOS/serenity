/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
package org.openjdk.bench.jdk.incubator.foreign;


import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import sun.misc.Unsafe;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.concurrent.TimeUnit;

import static jdk.incubator.foreign.MemoryLayouts.JAVA_INT;

@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@State(org.openjdk.jmh.annotations.Scope.Thread)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(value = 3, jvmArgsAppend = { "--add-modules=jdk.incubator.foreign" })
public class LoopOverPollutedBuffer {

    static final int ELEM_SIZE = 1_000_000;
    static final int CARRIER_SIZE = (int) JAVA_INT.byteSize();
    static final int ALLOC_SIZE = ELEM_SIZE * CARRIER_SIZE;

    static final Unsafe unsafe = Utils.unsafe;

    ByteBuffer dbb = ByteBuffer.allocateDirect(ALLOC_SIZE).order(ByteOrder.nativeOrder());
    byte[] arr = new byte[ALLOC_SIZE];
    ByteBuffer hbb = ByteBuffer.wrap(arr).order(ByteOrder.nativeOrder());
    FloatBuffer hfb = hbb.asFloatBuffer();


    @Setup
    public void setup() {
        for (int i = 0; i < ELEM_SIZE; i++) {
            dbb.putFloat(i * 4, i);
            hbb.putFloat(i * 4, i);
        }
        for (int i = 0; i < ELEM_SIZE; i++) {
            hfb.put(i, i);
        }
    }

    @TearDown
    public void tearDown() {
        unsafe.invokeCleaner(dbb);
        arr = null;
        hbb = null;
        hfb = null;
    }

    @Benchmark
    public int direct_byte_buffer_get_float() {
        int sum = 0;
        for (int k = 0; k < ELEM_SIZE; k++) {
            dbb.putFloat(k, (float)k + 1);
            float v = dbb.getFloat(k * 4);
            sum += (int)v;
        }
        return sum;
    }

    @Benchmark
    public int heap_byte_buffer_get_int() {
        int sum = 0;
        for (int k = 0; k < ELEM_SIZE; k++) {
            hbb.putInt(k, k + 1);
            int v = hbb.getInt(k * 4);
            sum += v;
        }
        return sum;
    }

    @Benchmark
    public int unsafe_get_float() {
        int sum = 0;
        for (int k = 0; k < ALLOC_SIZE; k += 4) {
            unsafe.putFloat(arr, k + Unsafe.ARRAY_BYTE_BASE_OFFSET, k + 1);
            float v = unsafe.getFloat(arr, k + Unsafe.ARRAY_BYTE_BASE_OFFSET);
            sum += (int)v;
        }
        return sum;
    }
}
