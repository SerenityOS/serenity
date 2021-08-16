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
 *   test(Direct|Heap)(Bulk|Loop)(Get|Put)Short(View)?(Swap)?(RO)?
 *
 * This allows to easily run a subset of particular interest. For example:
 *   Direct only :- "org.openjdk.bench.java.nio.ShortBuffers.testDirect.*"
 *   Bulk only   :- "org.openjdk.bench.java.nio.ShortBuffers.test.*Bulk.*"
 *   Loop Put Swapped Views: -
 *      test(Direct|Heap)(Loop)(Put)Short(View)+(Swap)+"
 */
@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Fork(3)
public class ShortBuffers {

    static final int CARRIER_BYTE_WIDTH = 2;

    @Param({"16", "1024", "131072"})
    private int size;

    public short shortValue;
    public short[] shortArray;

    public ShortBuffer heapShortBuffer;
    public ShortBuffer heapShortBufferRO;
    public ShortBuffer heapByteBufferAsShortBufferView;
    public ShortBuffer heapByteBufferAsShortBufferViewRO;
    public ShortBuffer heapByteBufferAsShortBufferViewSwap;
    public ShortBuffer heapByteBufferAsShortBufferViewSwapRO;
    public ShortBuffer directByteBufferAsShortBufferView;
    public ShortBuffer directByteBufferAsShortBufferViewRO;
    public ShortBuffer directByteBufferAsShortBufferViewSwap;
    public ShortBuffer directByteBufferAsShortBufferViewSwapRO;

    @Setup
    public void setup() {
        shortArray = new short[size / CARRIER_BYTE_WIDTH];

        // explicitly allocated heap carrier buffer
        heapShortBuffer = ShortBuffer.allocate(size / CARRIER_BYTE_WIDTH);
        heapShortBufferRO = ShortBuffer.allocate(size / CARRIER_BYTE_WIDTH).asReadOnlyBuffer();

        // ByteBuffer views
        heapByteBufferAsShortBufferView     = ByteBuffer.allocate(size).order(nativeOrder()).asShortBuffer();
        heapByteBufferAsShortBufferViewRO   = ByteBuffer.allocate(size).order(nativeOrder()).asShortBuffer().asReadOnlyBuffer();
        directByteBufferAsShortBufferView   = ByteBuffer.allocateDirect(size).order(nativeOrder()).asShortBuffer();
        directByteBufferAsShortBufferViewRO = ByteBuffer.allocateDirect(size).order(nativeOrder()).asShortBuffer().asReadOnlyBuffer();

        // endianness swapped
        ByteOrder nonNativeOrder = nativeOrder() == BIG_ENDIAN ? LITTLE_ENDIAN : BIG_ENDIAN;
        heapByteBufferAsShortBufferViewSwap     = ByteBuffer.allocate(size).order(nonNativeOrder).asShortBuffer();
        heapByteBufferAsShortBufferViewSwapRO   = ByteBuffer.allocate(size).order(nonNativeOrder).asShortBuffer().asReadOnlyBuffer();
        directByteBufferAsShortBufferViewSwap   = ByteBuffer.allocateDirect(size).order(nonNativeOrder).asShortBuffer();
        directByteBufferAsShortBufferViewSwapRO = ByteBuffer.allocateDirect(size).order(nonNativeOrder).asShortBuffer().asReadOnlyBuffer();
    }

    // ---------------- HELPER METHODS

    private int innerLoopGetShort(ShortBuffer buf) {
        int r = 0;
        for (int i = 0; i < buf.capacity(); i++) {
            r += buf.get(i);
        }
        return r;
    }

    private void innerLoopPutShort(ShortBuffer buf) {
        for (int i = 0; i < buf.capacity(); i++) {
            buf.put(i, shortValue);
        }
    }

    // -- Heap___

    @Benchmark
    public short[] testHeapBulkPutShort() {
        heapShortBuffer.put(0, shortArray);
        return shortArray;
    }

    @Benchmark
    public short[] testHeapBulkGetShort() {
        heapShortBuffer.get(0, shortArray);
        return shortArray;
    }

    @Benchmark
    public void testHeapLoopPutShort() {
        innerLoopPutShort(heapShortBuffer);
    }

    @Benchmark
    public int testHeapLoopGetShort() {
        return innerLoopGetShort(heapShortBuffer);
    }

    // -- Heap_View_Swap_RO

    @Benchmark
    public short[] testHeapBulkGetShortViewSwapRO() {
        heapByteBufferAsShortBufferViewSwapRO.get(0, shortArray);
        return shortArray;
    }

    @Benchmark
    public int testHeapLoopGetShortViewSwapRO() {
        return innerLoopGetShort(heapByteBufferAsShortBufferViewSwapRO);
    }

    // -- Heap_View_Swap_

    @Benchmark
    public short[] testHeapBulkPutShortViewSwap() {
        heapByteBufferAsShortBufferViewSwap.put(0, shortArray);
        return shortArray;
    }

    @Benchmark
    public short[] testHeapBulkGetShortViewSwap() {
        heapByteBufferAsShortBufferViewSwap.get(0, shortArray);
        return shortArray;
    }

    @Benchmark
    public void testHeapLoopPutShortViewSwap() {
        innerLoopPutShort(heapByteBufferAsShortBufferViewSwap);
    }

    @Benchmark
    public int testHeapLoopGetShortViewSwap() {
        return innerLoopGetShort(heapByteBufferAsShortBufferViewSwap);
    }

    // -- Heap_View__RO

    @Benchmark
    public short[] testHeapBulkGetShortViewRO() {
        heapByteBufferAsShortBufferViewRO.get(0, shortArray);
        return shortArray;
    }

    @Benchmark
    public int testHeapLoopGetShortViewRO() {
        return innerLoopGetShort(heapByteBufferAsShortBufferViewRO);
    }

    // -- Heap_View__

    @Benchmark
    public short[] testHeapBulkPutShortView() {
        heapByteBufferAsShortBufferView.put(0, shortArray);
        return shortArray;
    }

    @Benchmark
    public short[] testHeapBulkGetShortView() {
        heapByteBufferAsShortBufferView.get(0, shortArray);
        return shortArray;
    }

    @Benchmark
    public void testHeapLoopPutShortView() {
        innerLoopPutShort(heapByteBufferAsShortBufferView);
    }

    @Benchmark
    public int testHeapLoopGetShortView() {
        return innerLoopGetShort(heapByteBufferAsShortBufferView);
    }

    // -- Direct_View_Swap_RO

    @Benchmark
    public short[] testDirectBulkGetShortViewSwapRO() {
        directByteBufferAsShortBufferViewSwapRO.get(0, shortArray);
        return shortArray;
    }

    @Benchmark
    public int testDirectLoopGetShortViewSwapRO() {
        return innerLoopGetShort(directByteBufferAsShortBufferViewSwapRO);
    }

    // -- Direct_View_Swap_

    @Benchmark
    public short[] testDirectBulkPutShortViewSwap() {
        directByteBufferAsShortBufferViewSwap.put(0, shortArray);
        return shortArray;
    }

    @Benchmark
    public short[] testDirectBulkGetShortViewSwap() {
        directByteBufferAsShortBufferViewSwap.get(0, shortArray);
        return shortArray;
    }

    @Benchmark
    public void testDirectLoopPutShortViewSwap() {
        innerLoopPutShort(directByteBufferAsShortBufferViewSwap);
    }

    @Benchmark
    public int testDirectLoopGetShortViewSwap() {
        return innerLoopGetShort(directByteBufferAsShortBufferViewSwap);
    }

    // -- Direct_View__RO

    @Benchmark
    public short[] testDirectBulkGetShortViewRO() {
        directByteBufferAsShortBufferViewRO.get(0, shortArray);
        return shortArray;
    }

    @Benchmark
    public int testDirectLoopGetShortViewRO() {
        return innerLoopGetShort(directByteBufferAsShortBufferViewRO);
    }

    // -- Direct_View__

    @Benchmark
    public short[] testDirectBulkPutShortView() {
        directByteBufferAsShortBufferView.put(0, shortArray);
        return shortArray;
    }

    @Benchmark
    public short[] testDirectBulkGetShortView() {
        directByteBufferAsShortBufferView.get(0, shortArray);
        return shortArray;
    }

    @Benchmark
    public void testDirectLoopPutShortView() {
        innerLoopPutShort(directByteBufferAsShortBufferView);
    }

    @Benchmark
    public int testDirectLoopGetShortView() {
        return innerLoopGetShort(directByteBufferAsShortBufferView);
    }
}
