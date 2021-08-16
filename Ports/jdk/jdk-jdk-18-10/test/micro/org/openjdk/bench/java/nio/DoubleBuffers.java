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
 *   test(Direct|Heap)(Bulk|Loop)(Get|Put)Double(View)?(Swap)?(RO)?
 *
 * This allows to easily run a subset of particular interest. For example:
 *   Direct only :- "org.openjdk.bench.java.nio.DoubleBuffers.testDirect.*"
 *   Bulk only   :- "org.openjdk.bench.java.nio.DoubleBuffers.test.*Bulk.*"
 *   Loop Put Swapped Views: -
 *      test(Direct|Heap)(Loop)(Put)Double(View)+(Swap)+"
 */
@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Fork(3)
public class DoubleBuffers {

    static final int CARRIER_BYTE_WIDTH = 8;

    @Param({"16", "1024", "131072"})
    private int size;

    public double doubleValue;
    public double[] doubleArray;

    public DoubleBuffer heapDoubleBuffer;
    public DoubleBuffer heapDoubleBufferRO;
    public DoubleBuffer heapByteBufferAsDoubleBufferView;
    public DoubleBuffer heapByteBufferAsDoubleBufferViewRO;
    public DoubleBuffer heapByteBufferAsDoubleBufferViewSwap;
    public DoubleBuffer heapByteBufferAsDoubleBufferViewSwapRO;
    public DoubleBuffer directByteBufferAsDoubleBufferView;
    public DoubleBuffer directByteBufferAsDoubleBufferViewRO;
    public DoubleBuffer directByteBufferAsDoubleBufferViewSwap;
    public DoubleBuffer directByteBufferAsDoubleBufferViewSwapRO;

    @Setup
    public void setup() {
        doubleArray = new double[size / CARRIER_BYTE_WIDTH];

        // explicitly allocated heap carrier buffer
        heapDoubleBuffer = DoubleBuffer.allocate(size / CARRIER_BYTE_WIDTH);
        heapDoubleBufferRO = DoubleBuffer.allocate(size / CARRIER_BYTE_WIDTH).asReadOnlyBuffer();

        // ByteBuffer views
        heapByteBufferAsDoubleBufferView     = ByteBuffer.allocate(size).order(nativeOrder()).asDoubleBuffer();
        heapByteBufferAsDoubleBufferViewRO   = ByteBuffer.allocate(size).order(nativeOrder()).asDoubleBuffer().asReadOnlyBuffer();
        directByteBufferAsDoubleBufferView   = ByteBuffer.allocateDirect(size).order(nativeOrder()).asDoubleBuffer();
        directByteBufferAsDoubleBufferViewRO = ByteBuffer.allocateDirect(size).order(nativeOrder()).asDoubleBuffer().asReadOnlyBuffer();

        // endianness swapped
        ByteOrder nonNativeOrder = nativeOrder() == BIG_ENDIAN ? LITTLE_ENDIAN : BIG_ENDIAN;
        heapByteBufferAsDoubleBufferViewSwap     = ByteBuffer.allocate(size).order(nonNativeOrder).asDoubleBuffer();
        heapByteBufferAsDoubleBufferViewSwapRO   = ByteBuffer.allocate(size).order(nonNativeOrder).asDoubleBuffer().asReadOnlyBuffer();
        directByteBufferAsDoubleBufferViewSwap   = ByteBuffer.allocateDirect(size).order(nonNativeOrder).asDoubleBuffer();
        directByteBufferAsDoubleBufferViewSwapRO = ByteBuffer.allocateDirect(size).order(nonNativeOrder).asDoubleBuffer().asReadOnlyBuffer();
    }

    // ---------------- HELPER METHODS

    private int innerLoopGetDouble(DoubleBuffer buf) {
        int r = 0;
        for (int i = 0; i < buf.capacity(); i++) {
            r += buf.get(i);
        }
        return r;
    }

    private void innerLoopPutDouble(DoubleBuffer buf) {
        for (int i = 0; i < buf.capacity(); i++) {
            buf.put(i, doubleValue);
        }
    }

    // -- Heap___

    @Benchmark
    public double[] testHeapBulkPutDouble() {
        heapDoubleBuffer.put(0, doubleArray);
        return doubleArray;
    }

    @Benchmark
    public double[] testHeapBulkGetDouble() {
        heapDoubleBuffer.get(0, doubleArray);
        return doubleArray;
    }

    @Benchmark
    public void testHeapLoopPutDouble() {
        innerLoopPutDouble(heapDoubleBuffer);
    }

    @Benchmark
    public int testHeapLoopGetDouble() {
        return innerLoopGetDouble(heapDoubleBuffer);
    }

    // -- Heap_View_Swap_RO

    @Benchmark
    public double[] testHeapBulkGetDoubleViewSwapRO() {
        heapByteBufferAsDoubleBufferViewSwapRO.get(0, doubleArray);
        return doubleArray;
    }

    @Benchmark
    public int testHeapLoopGetDoubleViewSwapRO() {
        return innerLoopGetDouble(heapByteBufferAsDoubleBufferViewSwapRO);
    }

    // -- Heap_View_Swap_

    @Benchmark
    public double[] testHeapBulkPutDoubleViewSwap() {
        heapByteBufferAsDoubleBufferViewSwap.put(0, doubleArray);
        return doubleArray;
    }

    @Benchmark
    public double[] testHeapBulkGetDoubleViewSwap() {
        heapByteBufferAsDoubleBufferViewSwap.get(0, doubleArray);
        return doubleArray;
    }

    @Benchmark
    public void testHeapLoopPutDoubleViewSwap() {
        innerLoopPutDouble(heapByteBufferAsDoubleBufferViewSwap);
    }

    @Benchmark
    public int testHeapLoopGetDoubleViewSwap() {
        return innerLoopGetDouble(heapByteBufferAsDoubleBufferViewSwap);
    }

    // -- Heap_View__RO

    @Benchmark
    public double[] testHeapBulkGetDoubleViewRO() {
        heapByteBufferAsDoubleBufferViewRO.get(0, doubleArray);
        return doubleArray;
    }

    @Benchmark
    public int testHeapLoopGetDoubleViewRO() {
        return innerLoopGetDouble(heapByteBufferAsDoubleBufferViewRO);
    }

    // -- Heap_View__

    @Benchmark
    public double[] testHeapBulkPutDoubleView() {
        heapByteBufferAsDoubleBufferView.put(0, doubleArray);
        return doubleArray;
    }

    @Benchmark
    public double[] testHeapBulkGetDoubleView() {
        heapByteBufferAsDoubleBufferView.get(0, doubleArray);
        return doubleArray;
    }

    @Benchmark
    public void testHeapLoopPutDoubleView() {
        innerLoopPutDouble(heapByteBufferAsDoubleBufferView);
    }

    @Benchmark
    public int testHeapLoopGetDoubleView() {
        return innerLoopGetDouble(heapByteBufferAsDoubleBufferView);
    }

    // -- Direct_View_Swap_RO

    @Benchmark
    public double[] testDirectBulkGetDoubleViewSwapRO() {
        directByteBufferAsDoubleBufferViewSwapRO.get(0, doubleArray);
        return doubleArray;
    }

    @Benchmark
    public int testDirectLoopGetDoubleViewSwapRO() {
        return innerLoopGetDouble(directByteBufferAsDoubleBufferViewSwapRO);
    }

    // -- Direct_View_Swap_

    @Benchmark
    public double[] testDirectBulkPutDoubleViewSwap() {
        directByteBufferAsDoubleBufferViewSwap.put(0, doubleArray);
        return doubleArray;
    }

    @Benchmark
    public double[] testDirectBulkGetDoubleViewSwap() {
        directByteBufferAsDoubleBufferViewSwap.get(0, doubleArray);
        return doubleArray;
    }

    @Benchmark
    public void testDirectLoopPutDoubleViewSwap() {
        innerLoopPutDouble(directByteBufferAsDoubleBufferViewSwap);
    }

    @Benchmark
    public int testDirectLoopGetDoubleViewSwap() {
        return innerLoopGetDouble(directByteBufferAsDoubleBufferViewSwap);
    }

    // -- Direct_View__RO

    @Benchmark
    public double[] testDirectBulkGetDoubleViewRO() {
        directByteBufferAsDoubleBufferViewRO.get(0, doubleArray);
        return doubleArray;
    }

    @Benchmark
    public int testDirectLoopGetDoubleViewRO() {
        return innerLoopGetDouble(directByteBufferAsDoubleBufferViewRO);
    }

    // -- Direct_View__

    @Benchmark
    public double[] testDirectBulkPutDoubleView() {
        directByteBufferAsDoubleBufferView.put(0, doubleArray);
        return doubleArray;
    }

    @Benchmark
    public double[] testDirectBulkGetDoubleView() {
        directByteBufferAsDoubleBufferView.get(0, doubleArray);
        return doubleArray;
    }

    @Benchmark
    public void testDirectLoopPutDoubleView() {
        innerLoopPutDouble(directByteBufferAsDoubleBufferView);
    }

    @Benchmark
    public int testDirectLoopGetDoubleView() {
        return innerLoopGetDouble(directByteBufferAsDoubleBufferView);
    }
}
