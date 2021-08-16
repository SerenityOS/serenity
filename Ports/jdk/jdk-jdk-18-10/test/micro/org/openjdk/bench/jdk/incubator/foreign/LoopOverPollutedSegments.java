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
package org.openjdk.bench.jdk.incubator.foreign;

import jdk.incubator.foreign.MemoryAccess;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
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

import java.lang.invoke.VarHandle;
import java.util.concurrent.TimeUnit;

import static jdk.incubator.foreign.MemoryLayout.PathElement.sequenceElement;
import static jdk.incubator.foreign.MemoryLayouts.JAVA_INT;

@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@State(org.openjdk.jmh.annotations.Scope.Thread)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(value = 3, jvmArgsAppend = { "--add-modules=jdk.incubator.foreign" })
public class LoopOverPollutedSegments {

    static final int ELEM_SIZE = 1_000_000;
    static final int CARRIER_SIZE = (int) JAVA_INT.byteSize();
    static final int ALLOC_SIZE = ELEM_SIZE * CARRIER_SIZE;

    static final Unsafe unsafe = Utils.unsafe;

    MemorySegment nativeSegment, heapSegmentBytes, heapSegmentFloats;
    byte[] arr;
    long addr;

    static final VarHandle intHandle = MemoryLayout.sequenceLayout(JAVA_INT).varHandle(int.class, MemoryLayout.PathElement.sequenceElement());


    @Setup
    public void setup() {
        addr = unsafe.allocateMemory(ALLOC_SIZE);
        for (int i = 0; i < ELEM_SIZE; i++) {
            unsafe.putInt(addr + (i * 4), i);
        }
        arr = new byte[ALLOC_SIZE];
        nativeSegment = MemorySegment.allocateNative(ALLOC_SIZE, 4, ResourceScope.newConfinedScope());
        heapSegmentBytes = MemorySegment.ofArray(new byte[ALLOC_SIZE]);
        heapSegmentFloats = MemorySegment.ofArray(new float[ELEM_SIZE]);

        for (int rep = 0 ; rep < 5 ; rep++) {
            for (int i = 0; i < ELEM_SIZE; i++) {
                unsafe.putInt(arr, Unsafe.ARRAY_BYTE_BASE_OFFSET + (i * 4), i);
                MemoryAccess.setIntAtIndex(nativeSegment, i, i);
                MemoryAccess.setFloatAtIndex(nativeSegment, i, i);
                intHandle.set(nativeSegment, (long)i, i);
                MemoryAccess.setIntAtIndex(heapSegmentBytes, i, i);
                MemoryAccess.setFloatAtIndex(heapSegmentBytes, i, i);
                intHandle.set(heapSegmentBytes, (long)i, i);
                MemoryAccess.setIntAtIndex(heapSegmentFloats, i, i);
                MemoryAccess.setFloatAtIndex(heapSegmentFloats, i, i);
                intHandle.set(heapSegmentFloats, (long)i, i);
            }
        }
    }

    @TearDown
    public void tearDown() {
        nativeSegment.scope().close();
        heapSegmentBytes = null;
        heapSegmentFloats = null;
        arr = null;
        unsafe.freeMemory(addr);
    }

    @Benchmark
    public int native_segment_VH() {
        int sum = 0;
        for (int k = 0; k < ELEM_SIZE; k++) {
            intHandle.set(nativeSegment, (long)k, k + 1);
            int v = (int) intHandle.get(nativeSegment, (long)k);
            sum += v;
        }
        return sum;
    }

    @Benchmark
    public int native_segment_static() {
        int sum = 0;
        for (int k = 0; k < ELEM_SIZE; k++) {
            MemoryAccess.setIntAtOffset(nativeSegment, k, k + 1);
            int v = MemoryAccess.getIntAtOffset(nativeSegment, k);
            sum += v;
        }
        return sum;
    }

    @Benchmark
    public int heap_segment_ints_VH() {
        int sum = 0;
        for (int k = 0; k < ELEM_SIZE; k++) {
            intHandle.set(heapSegmentBytes, (long)k, k + 1);
            int v = (int) intHandle.get(heapSegmentBytes, (long)k);
            sum += v;
        }
        return sum;
    }

    @Benchmark
    public int heap_segment_ints_static() {
        int sum = 0;
        for (int k = 0; k < ELEM_SIZE; k++) {
            MemoryAccess.setIntAtOffset(heapSegmentBytes, k, k + 1);
            int v = MemoryAccess.getIntAtOffset(heapSegmentBytes, k);
            sum += v;
        }
        return sum;
    }

    @Benchmark
    public int heap_segment_floats_VH() {
        int sum = 0;
        for (int k = 0; k < ELEM_SIZE; k++) {
            intHandle.set(heapSegmentFloats, (long)k, k + 1);
            int v = (int)intHandle.get(heapSegmentFloats, (long)k);
            sum += v;
        }
        return sum;
    }

    @Benchmark
    public int heap_segment_floats_static() {
        int sum = 0;
        for (int k = 0; k < ELEM_SIZE; k++) {
            MemoryAccess.setIntAtOffset(heapSegmentFloats, k, k + 1);
            int v = MemoryAccess.getIntAtOffset(heapSegmentFloats, k);
            sum += v;
        }
        return sum;
    }

    @Benchmark
    public int heap_unsafe() {
        int sum = 0;
        for (int k = 0; k < ALLOC_SIZE; k += 4) {
            unsafe.putInt(arr, k + Unsafe.ARRAY_BYTE_BASE_OFFSET, k + 1);
            int v = unsafe.getInt(arr, k + Unsafe.ARRAY_BYTE_BASE_OFFSET);
            sum += v;
        }
        return sum;
    }

    @Benchmark
    public int native_unsafe() {
        int sum = 0;
        for (int k = 0; k < ALLOC_SIZE; k += 4) {
            unsafe.putInt(addr + k, k + 1);
            int v = unsafe.getInt(addr + k);
            sum += v;
        }
        return sum;
    }
}
