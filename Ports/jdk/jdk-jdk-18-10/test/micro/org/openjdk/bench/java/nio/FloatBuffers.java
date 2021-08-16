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
 *   test(Direct|Heap)(Bulk|Loop)(Get|Put)Float(View)?(Swap)?(RO)?
 *
 * This allows to easily run a subset of particular interest. For example:
 *   Direct only :- "org.openjdk.bench.java.nio.FloatBuffers.testDirect.*"
 *   Bulk only   :- "org.openjdk.bench.java.nio.FloatBuffers.test.*Bulk.*"
 *   Loop Put Swapped Views: -
 *      test(Direct|Heap)(Loop)(Put)Float(View)+(Swap)+"
 */
@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Fork(3)
public class FloatBuffers {

    static final int CARRIER_BYTE_WIDTH = 4;

    @Param({"16", "1024", "131072"})
    private int size;

    public float floatValue;
    public float[] floatArray;

    public FloatBuffer heapFloatBuffer;
    public FloatBuffer heapFloatBufferRO;
    public FloatBuffer heapByteBufferAsFloatBufferView;
    public FloatBuffer heapByteBufferAsFloatBufferViewRO;
    public FloatBuffer heapByteBufferAsFloatBufferViewSwap;
    public FloatBuffer heapByteBufferAsFloatBufferViewSwapRO;
    public FloatBuffer directByteBufferAsFloatBufferView;
    public FloatBuffer directByteBufferAsFloatBufferViewRO;
    public FloatBuffer directByteBufferAsFloatBufferViewSwap;
    public FloatBuffer directByteBufferAsFloatBufferViewSwapRO;

    @Setup
    public void setup() {
        floatArray = new float[size / CARRIER_BYTE_WIDTH];

        // explicitly allocated heap carrier buffer
        heapFloatBuffer = FloatBuffer.allocate(size / CARRIER_BYTE_WIDTH);
        heapFloatBufferRO = FloatBuffer.allocate(size / CARRIER_BYTE_WIDTH).asReadOnlyBuffer();

        // ByteBuffer views
        heapByteBufferAsFloatBufferView     = ByteBuffer.allocate(size).order(nativeOrder()).asFloatBuffer();
        heapByteBufferAsFloatBufferViewRO   = ByteBuffer.allocate(size).order(nativeOrder()).asFloatBuffer().asReadOnlyBuffer();
        directByteBufferAsFloatBufferView   = ByteBuffer.allocateDirect(size).order(nativeOrder()).asFloatBuffer();
        directByteBufferAsFloatBufferViewRO = ByteBuffer.allocateDirect(size).order(nativeOrder()).asFloatBuffer().asReadOnlyBuffer();

        // endianness swapped
        ByteOrder nonNativeOrder = nativeOrder() == BIG_ENDIAN ? LITTLE_ENDIAN : BIG_ENDIAN;
        heapByteBufferAsFloatBufferViewSwap     = ByteBuffer.allocate(size).order(nonNativeOrder).asFloatBuffer();
        heapByteBufferAsFloatBufferViewSwapRO   = ByteBuffer.allocate(size).order(nonNativeOrder).asFloatBuffer().asReadOnlyBuffer();
        directByteBufferAsFloatBufferViewSwap   = ByteBuffer.allocateDirect(size).order(nonNativeOrder).asFloatBuffer();
        directByteBufferAsFloatBufferViewSwapRO = ByteBuffer.allocateDirect(size).order(nonNativeOrder).asFloatBuffer().asReadOnlyBuffer();
    }

    // ---------------- HELPER METHODS

    private int innerLoopGetFloat(FloatBuffer buf) {
        int r = 0;
        for (int i = 0; i < buf.capacity(); i++) {
            r += buf.get(i);
        }
        return r;
    }

    private void innerLoopPutFloat(FloatBuffer buf) {
        for (int i = 0; i < buf.capacity(); i++) {
            buf.put(i, floatValue);
        }
    }

    // -- Heap___

    @Benchmark
    public float[] testHeapBulkPutFloat() {
        heapFloatBuffer.put(0, floatArray);
        return floatArray;
    }

    @Benchmark
    public float[] testHeapBulkGetFloat() {
        heapFloatBuffer.get(0, floatArray);
        return floatArray;
    }

    @Benchmark
    public void testHeapLoopPutFloat() {
        innerLoopPutFloat(heapFloatBuffer);
    }

    @Benchmark
    public int testHeapLoopGetFloat() {
        return innerLoopGetFloat(heapFloatBuffer);
    }

    // -- Heap_View_Swap_RO

    @Benchmark
    public float[] testHeapBulkGetFloatViewSwapRO() {
        heapByteBufferAsFloatBufferViewSwapRO.get(0, floatArray);
        return floatArray;
    }

    @Benchmark
    public int testHeapLoopGetFloatViewSwapRO() {
        return innerLoopGetFloat(heapByteBufferAsFloatBufferViewSwapRO);
    }

    // -- Heap_View_Swap_

    @Benchmark
    public float[] testHeapBulkPutFloatViewSwap() {
        heapByteBufferAsFloatBufferViewSwap.put(0, floatArray);
        return floatArray;
    }

    @Benchmark
    public float[] testHeapBulkGetFloatViewSwap() {
        heapByteBufferAsFloatBufferViewSwap.get(0, floatArray);
        return floatArray;
    }

    @Benchmark
    public void testHeapLoopPutFloatViewSwap() {
        innerLoopPutFloat(heapByteBufferAsFloatBufferViewSwap);
    }

    @Benchmark
    public int testHeapLoopGetFloatViewSwap() {
        return innerLoopGetFloat(heapByteBufferAsFloatBufferViewSwap);
    }

    // -- Heap_View__RO

    @Benchmark
    public float[] testHeapBulkGetFloatViewRO() {
        heapByteBufferAsFloatBufferViewRO.get(0, floatArray);
        return floatArray;
    }

    @Benchmark
    public int testHeapLoopGetFloatViewRO() {
        return innerLoopGetFloat(heapByteBufferAsFloatBufferViewRO);
    }

    // -- Heap_View__

    @Benchmark
    public float[] testHeapBulkPutFloatView() {
        heapByteBufferAsFloatBufferView.put(0, floatArray);
        return floatArray;
    }

    @Benchmark
    public float[] testHeapBulkGetFloatView() {
        heapByteBufferAsFloatBufferView.get(0, floatArray);
        return floatArray;
    }

    @Benchmark
    public void testHeapLoopPutFloatView() {
        innerLoopPutFloat(heapByteBufferAsFloatBufferView);
    }

    @Benchmark
    public int testHeapLoopGetFloatView() {
        return innerLoopGetFloat(heapByteBufferAsFloatBufferView);
    }

    // -- Direct_View_Swap_RO

    @Benchmark
    public float[] testDirectBulkGetFloatViewSwapRO() {
        directByteBufferAsFloatBufferViewSwapRO.get(0, floatArray);
        return floatArray;
    }

    @Benchmark
    public int testDirectLoopGetFloatViewSwapRO() {
        return innerLoopGetFloat(directByteBufferAsFloatBufferViewSwapRO);
    }

    // -- Direct_View_Swap_

    @Benchmark
    public float[] testDirectBulkPutFloatViewSwap() {
        directByteBufferAsFloatBufferViewSwap.put(0, floatArray);
        return floatArray;
    }

    @Benchmark
    public float[] testDirectBulkGetFloatViewSwap() {
        directByteBufferAsFloatBufferViewSwap.get(0, floatArray);
        return floatArray;
    }

    @Benchmark
    public void testDirectLoopPutFloatViewSwap() {
        innerLoopPutFloat(directByteBufferAsFloatBufferViewSwap);
    }

    @Benchmark
    public int testDirectLoopGetFloatViewSwap() {
        return innerLoopGetFloat(directByteBufferAsFloatBufferViewSwap);
    }

    // -- Direct_View__RO

    @Benchmark
    public float[] testDirectBulkGetFloatViewRO() {
        directByteBufferAsFloatBufferViewRO.get(0, floatArray);
        return floatArray;
    }

    @Benchmark
    public int testDirectLoopGetFloatViewRO() {
        return innerLoopGetFloat(directByteBufferAsFloatBufferViewRO);
    }

    // -- Direct_View__

    @Benchmark
    public float[] testDirectBulkPutFloatView() {
        directByteBufferAsFloatBufferView.put(0, floatArray);
        return floatArray;
    }

    @Benchmark
    public float[] testDirectBulkGetFloatView() {
        directByteBufferAsFloatBufferView.get(0, floatArray);
        return floatArray;
    }

    @Benchmark
    public void testDirectLoopPutFloatView() {
        innerLoopPutFloat(directByteBufferAsFloatBufferView);
    }

    @Benchmark
    public int testDirectLoopGetFloatView() {
        return innerLoopGetFloat(directByteBufferAsFloatBufferView);
    }
}
