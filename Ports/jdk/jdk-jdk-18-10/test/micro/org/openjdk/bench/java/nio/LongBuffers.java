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
 *   test(Direct|Heap)(Bulk|Loop)(Get|Put)Long(View)?(Swap)?(RO)?
 *
 * This allows to easily run a subset of particular interest. For example:
 *   Direct only :- "org.openjdk.bench.java.nio.LongBuffers.testDirect.*"
 *   Bulk only   :- "org.openjdk.bench.java.nio.LongBuffers.test.*Bulk.*"
 *   Loop Put Swapped Views: -
 *      test(Direct|Heap)(Loop)(Put)Long(View)+(Swap)+"
 */
@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Fork(3)
public class LongBuffers {

    static final int CARRIER_BYTE_WIDTH = 8;

    @Param({"16", "1024", "131072"})
    private int size;

    public long longValue;
    public long[] longArray;

    public LongBuffer heapLongBuffer;
    public LongBuffer heapLongBufferRO;
    public LongBuffer heapByteBufferAsLongBufferView;
    public LongBuffer heapByteBufferAsLongBufferViewRO;
    public LongBuffer heapByteBufferAsLongBufferViewSwap;
    public LongBuffer heapByteBufferAsLongBufferViewSwapRO;
    public LongBuffer directByteBufferAsLongBufferView;
    public LongBuffer directByteBufferAsLongBufferViewRO;
    public LongBuffer directByteBufferAsLongBufferViewSwap;
    public LongBuffer directByteBufferAsLongBufferViewSwapRO;

    @Setup
    public void setup() {
        longArray = new long[size / CARRIER_BYTE_WIDTH];

        // explicitly allocated heap carrier buffer
        heapLongBuffer = LongBuffer.allocate(size / CARRIER_BYTE_WIDTH);
        heapLongBufferRO = LongBuffer.allocate(size / CARRIER_BYTE_WIDTH).asReadOnlyBuffer();

        // ByteBuffer views
        heapByteBufferAsLongBufferView     = ByteBuffer.allocate(size).order(nativeOrder()).asLongBuffer();
        heapByteBufferAsLongBufferViewRO   = ByteBuffer.allocate(size).order(nativeOrder()).asLongBuffer().asReadOnlyBuffer();
        directByteBufferAsLongBufferView   = ByteBuffer.allocateDirect(size).order(nativeOrder()).asLongBuffer();
        directByteBufferAsLongBufferViewRO = ByteBuffer.allocateDirect(size).order(nativeOrder()).asLongBuffer().asReadOnlyBuffer();

        // endianness swapped
        ByteOrder nonNativeOrder = nativeOrder() == BIG_ENDIAN ? LITTLE_ENDIAN : BIG_ENDIAN;
        heapByteBufferAsLongBufferViewSwap     = ByteBuffer.allocate(size).order(nonNativeOrder).asLongBuffer();
        heapByteBufferAsLongBufferViewSwapRO   = ByteBuffer.allocate(size).order(nonNativeOrder).asLongBuffer().asReadOnlyBuffer();
        directByteBufferAsLongBufferViewSwap   = ByteBuffer.allocateDirect(size).order(nonNativeOrder).asLongBuffer();
        directByteBufferAsLongBufferViewSwapRO = ByteBuffer.allocateDirect(size).order(nonNativeOrder).asLongBuffer().asReadOnlyBuffer();
    }

    // ---------------- HELPER METHODS

    private int innerLoopGetLong(LongBuffer buf) {
        int r = 0;
        for (int i = 0; i < buf.capacity(); i++) {
            r += buf.get(i);
        }
        return r;
    }

    private void innerLoopPutLong(LongBuffer buf) {
        for (int i = 0; i < buf.capacity(); i++) {
            buf.put(i, longValue);
        }
    }

    // -- Heap___

    @Benchmark
    public long[] testHeapBulkPutLong() {
        heapLongBuffer.put(0, longArray);
        return longArray;
    }

    @Benchmark
    public long[] testHeapBulkGetLong() {
        heapLongBuffer.get(0, longArray);
        return longArray;
    }

    @Benchmark
    public void testHeapLoopPutLong() {
        innerLoopPutLong(heapLongBuffer);
    }

    @Benchmark
    public int testHeapLoopGetLong() {
        return innerLoopGetLong(heapLongBuffer);
    }

    // -- Heap_View_Swap_RO

    @Benchmark
    public long[] testHeapBulkGetLongViewSwapRO() {
        heapByteBufferAsLongBufferViewSwapRO.get(0, longArray);
        return longArray;
    }

    @Benchmark
    public int testHeapLoopGetLongViewSwapRO() {
        return innerLoopGetLong(heapByteBufferAsLongBufferViewSwapRO);
    }

    // -- Heap_View_Swap_

    @Benchmark
    public long[] testHeapBulkPutLongViewSwap() {
        heapByteBufferAsLongBufferViewSwap.put(0, longArray);
        return longArray;
    }

    @Benchmark
    public long[] testHeapBulkGetLongViewSwap() {
        heapByteBufferAsLongBufferViewSwap.get(0, longArray);
        return longArray;
    }

    @Benchmark
    public void testHeapLoopPutLongViewSwap() {
        innerLoopPutLong(heapByteBufferAsLongBufferViewSwap);
    }

    @Benchmark
    public int testHeapLoopGetLongViewSwap() {
        return innerLoopGetLong(heapByteBufferAsLongBufferViewSwap);
    }

    // -- Heap_View__RO

    @Benchmark
    public long[] testHeapBulkGetLongViewRO() {
        heapByteBufferAsLongBufferViewRO.get(0, longArray);
        return longArray;
    }

    @Benchmark
    public int testHeapLoopGetLongViewRO() {
        return innerLoopGetLong(heapByteBufferAsLongBufferViewRO);
    }

    // -- Heap_View__

    @Benchmark
    public long[] testHeapBulkPutLongView() {
        heapByteBufferAsLongBufferView.put(0, longArray);
        return longArray;
    }

    @Benchmark
    public long[] testHeapBulkGetLongView() {
        heapByteBufferAsLongBufferView.get(0, longArray);
        return longArray;
    }

    @Benchmark
    public void testHeapLoopPutLongView() {
        innerLoopPutLong(heapByteBufferAsLongBufferView);
    }

    @Benchmark
    public int testHeapLoopGetLongView() {
        return innerLoopGetLong(heapByteBufferAsLongBufferView);
    }

    // -- Direct_View_Swap_RO

    @Benchmark
    public long[] testDirectBulkGetLongViewSwapRO() {
        directByteBufferAsLongBufferViewSwapRO.get(0, longArray);
        return longArray;
    }

    @Benchmark
    public int testDirectLoopGetLongViewSwapRO() {
        return innerLoopGetLong(directByteBufferAsLongBufferViewSwapRO);
    }

    // -- Direct_View_Swap_

    @Benchmark
    public long[] testDirectBulkPutLongViewSwap() {
        directByteBufferAsLongBufferViewSwap.put(0, longArray);
        return longArray;
    }

    @Benchmark
    public long[] testDirectBulkGetLongViewSwap() {
        directByteBufferAsLongBufferViewSwap.get(0, longArray);
        return longArray;
    }

    @Benchmark
    public void testDirectLoopPutLongViewSwap() {
        innerLoopPutLong(directByteBufferAsLongBufferViewSwap);
    }

    @Benchmark
    public int testDirectLoopGetLongViewSwap() {
        return innerLoopGetLong(directByteBufferAsLongBufferViewSwap);
    }

    // -- Direct_View__RO

    @Benchmark
    public long[] testDirectBulkGetLongViewRO() {
        directByteBufferAsLongBufferViewRO.get(0, longArray);
        return longArray;
    }

    @Benchmark
    public int testDirectLoopGetLongViewRO() {
        return innerLoopGetLong(directByteBufferAsLongBufferViewRO);
    }

    // -- Direct_View__

    @Benchmark
    public long[] testDirectBulkPutLongView() {
        directByteBufferAsLongBufferView.put(0, longArray);
        return longArray;
    }

    @Benchmark
    public long[] testDirectBulkGetLongView() {
        directByteBufferAsLongBufferView.get(0, longArray);
        return longArray;
    }

    @Benchmark
    public void testDirectLoopPutLongView() {
        innerLoopPutLong(directByteBufferAsLongBufferView);
    }

    @Benchmark
    public int testDirectLoopGetLongView() {
        return innerLoopGetLong(directByteBufferAsLongBufferView);
    }
}
