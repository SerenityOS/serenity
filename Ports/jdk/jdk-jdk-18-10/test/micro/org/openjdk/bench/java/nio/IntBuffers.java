/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.bench.java.nio;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import java.nio.*;
import java.util.concurrent.TimeUnit;
import static java.nio.ByteOrder.*;

/**
 * Benchmark for memory access operations on java.nio.Buffer ( and its views )
 *
 * A large number of variants are covered. The individual benchmarks conform to
 * the following convention:
 *   test(Direct|Heap)(Bulk|Loop)(Get|Put)Int(View)?(Swap)?(RO)?
 *
 * This allows to easily run a subset of particular interest. For example:
 *   Direct only :- "org.openjdk.bench.java.nio.IntBuffers.testDirect.*"
 *   Bulk only   :- "org.openjdk.bench.java.nio.IntBuffers.test.*Bulk.*"
 *   Loop Put Swapped Views: -
 *      test(Direct|Heap)(Loop)(Put)Int(View)+(Swap)+"
 */
@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Fork(3)
public class IntBuffers {

    static final int CARRIER_BYTE_WIDTH = 4;

    @Param({"16", "1024", "131072"})
    private int size;

    public int intValue;
    public int[] intArray;

    public IntBuffer heapIntBuffer;
    public IntBuffer heapIntBufferRO;
    public IntBuffer heapByteBufferAsIntBufferView;
    public IntBuffer heapByteBufferAsIntBufferViewRO;
    public IntBuffer heapByteBufferAsIntBufferViewSwap;
    public IntBuffer heapByteBufferAsIntBufferViewSwapRO;
    public IntBuffer directByteBufferAsIntBufferView;
    public IntBuffer directByteBufferAsIntBufferViewRO;
    public IntBuffer directByteBufferAsIntBufferViewSwap;
    public IntBuffer directByteBufferAsIntBufferViewSwapRO;

    @Setup
    public void setup() {
        intArray = new int[size / CARRIER_BYTE_WIDTH];

        // explicitly allocated heap carrier buffer
        heapIntBuffer = IntBuffer.allocate(size / CARRIER_BYTE_WIDTH);
        heapIntBufferRO = IntBuffer.allocate(size / CARRIER_BYTE_WIDTH).asReadOnlyBuffer();

        // ByteBuffer views
        heapByteBufferAsIntBufferView     = ByteBuffer.allocate(size).order(nativeOrder()).asIntBuffer();
        heapByteBufferAsIntBufferViewRO   = ByteBuffer.allocate(size).order(nativeOrder()).asIntBuffer().asReadOnlyBuffer();
        directByteBufferAsIntBufferView   = ByteBuffer.allocateDirect(size).order(nativeOrder()).asIntBuffer();
        directByteBufferAsIntBufferViewRO = ByteBuffer.allocateDirect(size).order(nativeOrder()).asIntBuffer().asReadOnlyBuffer();

        // endianness swapped
        ByteOrder nonNativeOrder = nativeOrder() == BIG_ENDIAN ? LITTLE_ENDIAN : BIG_ENDIAN;
        heapByteBufferAsIntBufferViewSwap     = ByteBuffer.allocate(size).order(nonNativeOrder).asIntBuffer();
        heapByteBufferAsIntBufferViewSwapRO   = ByteBuffer.allocate(size).order(nonNativeOrder).asIntBuffer().asReadOnlyBuffer();
        directByteBufferAsIntBufferViewSwap   = ByteBuffer.allocateDirect(size).order(nonNativeOrder).asIntBuffer();
        directByteBufferAsIntBufferViewSwapRO = ByteBuffer.allocateDirect(size).order(nonNativeOrder).asIntBuffer().asReadOnlyBuffer();
    }

    // ---------------- HELPER METHODS

    private int innerLoopGetInt(IntBuffer buf) {
        int r = 0;
        for (int i = 0; i < buf.capacity(); i++) {
            r += buf.get(i);
        }
        return r;
    }

    private void innerLoopPutInt(IntBuffer buf) {
        for (int i = 0; i < buf.capacity(); i++) {
            buf.put(i, intValue);
        }
    }

    // -- Heap___

    @Benchmark
    public int[] testHeapBulkPutInt() {
        heapIntBuffer.put(0, intArray);
        return intArray;
    }

    @Benchmark
    public int[] testHeapBulkGetInt() {
        heapIntBuffer.get(0, intArray);
        return intArray;
    }

    @Benchmark
    public void testHeapLoopPutInt() {
        innerLoopPutInt(heapIntBuffer);
    }

    @Benchmark
    public int testHeapLoopGetInt() {
        return innerLoopGetInt(heapIntBuffer);
    }

    // -- Heap_View_Swap_RO

    @Benchmark
    public int[] testHeapBulkGetIntViewSwapRO() {
        heapByteBufferAsIntBufferViewSwapRO.get(0, intArray);
        return intArray;
    }

    @Benchmark
    public int testHeapLoopGetIntViewSwapRO() {
        return innerLoopGetInt(heapByteBufferAsIntBufferViewSwapRO);
    }

    // -- Heap_View_Swap_

    @Benchmark
    public int[] testHeapBulkPutIntViewSwap() {
        heapByteBufferAsIntBufferViewSwap.put(0, intArray);
        return intArray;
    }

    @Benchmark
    public int[] testHeapBulkGetIntViewSwap() {
        heapByteBufferAsIntBufferViewSwap.get(0, intArray);
        return intArray;
    }

    @Benchmark
    public void testHeapLoopPutIntViewSwap() {
        innerLoopPutInt(heapByteBufferAsIntBufferViewSwap);
    }

    @Benchmark
    public int testHeapLoopGetIntViewSwap() {
        return innerLoopGetInt(heapByteBufferAsIntBufferViewSwap);
    }

    // -- Heap_View__RO

    @Benchmark
    public int[] testHeapBulkGetIntViewRO() {
        heapByteBufferAsIntBufferViewRO.get(0, intArray);
        return intArray;
    }

    @Benchmark
    public int testHeapLoopGetIntViewRO() {
        return innerLoopGetInt(heapByteBufferAsIntBufferViewRO);
    }

    // -- Heap_View__

    @Benchmark
    public int[] testHeapBulkPutIntView() {
        heapByteBufferAsIntBufferView.put(0, intArray);
        return intArray;
    }

    @Benchmark
    public int[] testHeapBulkGetIntView() {
        heapByteBufferAsIntBufferView.get(0, intArray);
        return intArray;
    }

    @Benchmark
    public void testHeapLoopPutIntView() {
        innerLoopPutInt(heapByteBufferAsIntBufferView);
    }

    @Benchmark
    public int testHeapLoopGetIntView() {
        return innerLoopGetInt(heapByteBufferAsIntBufferView);
    }

    // -- Direct_View_Swap_RO

    @Benchmark
    public int[] testDirectBulkGetIntViewSwapRO() {
        directByteBufferAsIntBufferViewSwapRO.get(0, intArray);
        return intArray;
    }

    @Benchmark
    public int testDirectLoopGetIntViewSwapRO() {
        return innerLoopGetInt(directByteBufferAsIntBufferViewSwapRO);
    }

    // -- Direct_View_Swap_

    @Benchmark
    public int[] testDirectBulkPutIntViewSwap() {
        directByteBufferAsIntBufferViewSwap.put(0, intArray);
        return intArray;
    }

    @Benchmark
    public int[] testDirectBulkGetIntViewSwap() {
        directByteBufferAsIntBufferViewSwap.get(0, intArray);
        return intArray;
    }

    @Benchmark
    public void testDirectLoopPutIntViewSwap() {
        innerLoopPutInt(directByteBufferAsIntBufferViewSwap);
    }

    @Benchmark
    public int testDirectLoopGetIntViewSwap() {
        return innerLoopGetInt(directByteBufferAsIntBufferViewSwap);
    }

    // -- Direct_View__RO

    @Benchmark
    public int[] testDirectBulkGetIntViewRO() {
        directByteBufferAsIntBufferViewRO.get(0, intArray);
        return intArray;
    }

    @Benchmark
    public int testDirectLoopGetIntViewRO() {
        return innerLoopGetInt(directByteBufferAsIntBufferViewRO);
    }

    // -- Direct_View__

    @Benchmark
    public int[] testDirectBulkPutIntView() {
        directByteBufferAsIntBufferView.put(0, intArray);
        return intArray;
    }

    @Benchmark
    public int[] testDirectBulkGetIntView() {
        directByteBufferAsIntBufferView.get(0, intArray);
        return intArray;
    }

    @Benchmark
    public void testDirectLoopPutIntView() {
        innerLoopPutInt(directByteBufferAsIntBufferView);
    }

    @Benchmark
    public int testDirectLoopGetIntView() {
        return innerLoopGetInt(directByteBufferAsIntBufferView);
    }
}
