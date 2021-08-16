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
 *   test(Direct|Heap)(Bulk|Loop)(Get|Put)(Byte|Char|Short|Int|Long|Float|Double)(Swap)?(RO)?
 *
 * This allows to easily run a subset of particular interest. For example:
 *   Direct only :- "org.openjdk.bench.java.nio.ByteBuffers.testDirect.*"
 *   Bulk only   :- "org.openjdk.bench.java.nio.ByteBuffers.test.*Bulk.*"
 *   Loop Put Swapped Views: -
 *      test(Direct|Heap)(Loop)(Put)Byte(View)+(Swap)+"
 */
@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Fork(3)
public class ByteBuffers {

    static final int CARRIER_BYTE_WIDTH = 1;

    @Param({"16", "1024", "131072"})
    private int size;

    public byte byteValue;
    public char charValue;
    public short shortValue;
    public int intValue;
    public long longValue;
    public float floatValue;
    public double doubleValue;
    public byte[] byteArray;

    public ByteBuffer heapByteBuffer;
    public ByteBuffer heapByteBufferRO;
    public ByteBuffer directByteBuffer;
    public ByteBuffer directByteBufferRO;
    public ByteBuffer heapByteBufferSwap;
    public ByteBuffer heapByteBufferSwapRO;
    public ByteBuffer directByteBufferSwap;
    public ByteBuffer directByteBufferSwapRO;

    @Setup
    public void setup() {
        byteArray = new byte[size / CARRIER_BYTE_WIDTH];

        // explicitly allocated heap carrier buffer
        heapByteBuffer = ByteBuffer.allocate(size / CARRIER_BYTE_WIDTH);
        heapByteBufferRO = ByteBuffer.allocate(size / CARRIER_BYTE_WIDTH).asReadOnlyBuffer();

        heapByteBufferSwap     = ByteBuffer.allocate(size / CARRIER_BYTE_WIDTH).order(LITTLE_ENDIAN);
        heapByteBufferSwapRO   = ByteBuffer.allocate(size / CARRIER_BYTE_WIDTH).order(LITTLE_ENDIAN).asReadOnlyBuffer();
        directByteBuffer       = ByteBuffer.allocateDirect(size / CARRIER_BYTE_WIDTH);
        directByteBufferRO     = ByteBuffer.allocateDirect(size / CARRIER_BYTE_WIDTH).asReadOnlyBuffer();
        directByteBufferSwap   = ByteBuffer.allocateDirect(size / CARRIER_BYTE_WIDTH).order(LITTLE_ENDIAN);
        directByteBufferSwapRO = ByteBuffer.allocateDirect(size / CARRIER_BYTE_WIDTH).order(LITTLE_ENDIAN).asReadOnlyBuffer();
    }


    // -- Heap___

    @Benchmark
    public byte[] testHeapBulkPutByte() {
        heapByteBuffer.put(0, byteArray);
        return byteArray;
    }

    @Benchmark
    public byte[] testHeapBulkGetByte() {
        heapByteBuffer.get(0, byteArray);
        return byteArray;
    }

    // -- Heap_Byte_Swap_RO

    @Benchmark
    public int testHeapLoopGetByteSwapRO() {
        int r = 0;
        for (int i = 0; i < heapByteBufferSwapRO.capacity(); i+=1) {
            r += heapByteBufferSwapRO.get(i);
        }
        return r;
    }

    // -- Heap_Byte_Swap_

    @Benchmark
    public void testHeapLoopPutByteSwap() {
        for (int i = 0; i < heapByteBufferSwap.capacity(); i+=1) {
            heapByteBufferSwap.put(i, byteValue);
        }
    }

    @Benchmark
    public int testHeapLoopGetByteSwap() {
        int r = 0;
        for (int i = 0; i < heapByteBufferSwap.capacity(); i+=1) {
            r += heapByteBufferSwap.get(i);
        }
        return r;
    }

    // -- Heap_Byte__RO

    @Benchmark
    public int testHeapLoopGetByteRO() {
        int r = 0;
        for (int i = 0; i < heapByteBufferRO.capacity(); i+=1) {
            r += heapByteBufferRO.get(i);
        }
        return r;
    }

    // -- Heap_Byte__

    @Benchmark
    public void testHeapLoopPutByte() {
        for (int i = 0; i < heapByteBuffer.capacity(); i+=1) {
            heapByteBuffer.put(i, byteValue);
        }
    }

    @Benchmark
    public int testHeapLoopGetByte() {
        int r = 0;
        for (int i = 0; i < heapByteBuffer.capacity(); i+=1) {
            r += heapByteBuffer.get(i);
        }
        return r;
    }

    // -- Direct_Byte_Swap_RO

    @Benchmark
    public int testDirectLoopGetByteSwapRO() {
        int r = 0;
        for (int i = 0; i < directByteBufferSwapRO.capacity(); i+=1) {
            r += directByteBufferSwapRO.get(i);
        }
        return r;
    }

    // -- Direct_Byte_Swap_

    @Benchmark
    public void testDirectLoopPutByteSwap() {
        for (int i = 0; i < directByteBufferSwap.capacity(); i+=1) {
            directByteBufferSwap.put(i, byteValue);
        }
    }

    @Benchmark
    public int testDirectLoopGetByteSwap() {
        int r = 0;
        for (int i = 0; i < directByteBufferSwap.capacity(); i+=1) {
            r += directByteBufferSwap.get(i);
        }
        return r;
    }

    // -- Direct_Byte__RO

    @Benchmark
    public int testDirectLoopGetByteRO() {
        int r = 0;
        for (int i = 0; i < directByteBufferRO.capacity(); i+=1) {
            r += directByteBufferRO.get(i);
        }
        return r;
    }

    // -- Direct_Byte__

    @Benchmark
    public void testDirectLoopPutByte() {
        for (int i = 0; i < directByteBuffer.capacity(); i+=1) {
            directByteBuffer.put(i, byteValue);
        }
    }

    @Benchmark
    public int testDirectLoopGetByte() {
        int r = 0;
        for (int i = 0; i < directByteBuffer.capacity(); i+=1) {
            r += directByteBuffer.get(i);
        }
        return r;
    }

    // -- Heap_Char_Swap_RO

    @Benchmark
    public int testHeapLoopGetCharSwapRO() {
        int r = 0;
        for (int i = 0; i < heapByteBufferSwapRO.capacity(); i+=2) {
            r += heapByteBufferSwapRO.getChar(i);
        }
        return r;
    }

    // -- Heap_Char_Swap_

    @Benchmark
    public void testHeapLoopPutCharSwap() {
        for (int i = 0; i < heapByteBufferSwap.capacity(); i+=2) {
            heapByteBufferSwap.putChar(i, charValue);
        }
    }

    @Benchmark
    public int testHeapLoopGetCharSwap() {
        int r = 0;
        for (int i = 0; i < heapByteBufferSwap.capacity(); i+=2) {
            r += heapByteBufferSwap.getChar(i);
        }
        return r;
    }

    // -- Heap_Char__RO

    @Benchmark
    public int testHeapLoopGetCharRO() {
        int r = 0;
        for (int i = 0; i < heapByteBufferRO.capacity(); i+=2) {
            r += heapByteBufferRO.getChar(i);
        }
        return r;
    }

    // -- Heap_Char__

    @Benchmark
    public void testHeapLoopPutChar() {
        for (int i = 0; i < heapByteBuffer.capacity(); i+=2) {
            heapByteBuffer.putChar(i, charValue);
        }
    }

    @Benchmark
    public int testHeapLoopGetChar() {
        int r = 0;
        for (int i = 0; i < heapByteBuffer.capacity(); i+=2) {
            r += heapByteBuffer.getChar(i);
        }
        return r;
    }

    // -- Direct_Char_Swap_RO

    @Benchmark
    public int testDirectLoopGetCharSwapRO() {
        int r = 0;
        for (int i = 0; i < directByteBufferSwapRO.capacity(); i+=2) {
            r += directByteBufferSwapRO.getChar(i);
        }
        return r;
    }

    // -- Direct_Char_Swap_

    @Benchmark
    public void testDirectLoopPutCharSwap() {
        for (int i = 0; i < directByteBufferSwap.capacity(); i+=2) {
            directByteBufferSwap.putChar(i, charValue);
        }
    }

    @Benchmark
    public int testDirectLoopGetCharSwap() {
        int r = 0;
        for (int i = 0; i < directByteBufferSwap.capacity(); i+=2) {
            r += directByteBufferSwap.getChar(i);
        }
        return r;
    }

    // -- Direct_Char__RO

    @Benchmark
    public int testDirectLoopGetCharRO() {
        int r = 0;
        for (int i = 0; i < directByteBufferRO.capacity(); i+=2) {
            r += directByteBufferRO.getChar(i);
        }
        return r;
    }

    // -- Direct_Char__

    @Benchmark
    public void testDirectLoopPutChar() {
        for (int i = 0; i < directByteBuffer.capacity(); i+=2) {
            directByteBuffer.putChar(i, charValue);
        }
    }

    @Benchmark
    public int testDirectLoopGetChar() {
        int r = 0;
        for (int i = 0; i < directByteBuffer.capacity(); i+=2) {
            r += directByteBuffer.getChar(i);
        }
        return r;
    }

    // -- Heap_Short_Swap_RO

    @Benchmark
    public int testHeapLoopGetShortSwapRO() {
        int r = 0;
        for (int i = 0; i < heapByteBufferSwapRO.capacity(); i+=2) {
            r += heapByteBufferSwapRO.getShort(i);
        }
        return r;
    }

    // -- Heap_Short_Swap_

    @Benchmark
    public void testHeapLoopPutShortSwap() {
        for (int i = 0; i < heapByteBufferSwap.capacity(); i+=2) {
            heapByteBufferSwap.putShort(i, shortValue);
        }
    }

    @Benchmark
    public int testHeapLoopGetShortSwap() {
        int r = 0;
        for (int i = 0; i < heapByteBufferSwap.capacity(); i+=2) {
            r += heapByteBufferSwap.getShort(i);
        }
        return r;
    }

    // -- Heap_Short__RO

    @Benchmark
    public int testHeapLoopGetShortRO() {
        int r = 0;
        for (int i = 0; i < heapByteBufferRO.capacity(); i+=2) {
            r += heapByteBufferRO.getShort(i);
        }
        return r;
    }

    // -- Heap_Short__

    @Benchmark
    public void testHeapLoopPutShort() {
        for (int i = 0; i < heapByteBuffer.capacity(); i+=2) {
            heapByteBuffer.putShort(i, shortValue);
        }
    }

    @Benchmark
    public int testHeapLoopGetShort() {
        int r = 0;
        for (int i = 0; i < heapByteBuffer.capacity(); i+=2) {
            r += heapByteBuffer.getShort(i);
        }
        return r;
    }

    // -- Direct_Short_Swap_RO

    @Benchmark
    public int testDirectLoopGetShortSwapRO() {
        int r = 0;
        for (int i = 0; i < directByteBufferSwapRO.capacity(); i+=2) {
            r += directByteBufferSwapRO.getShort(i);
        }
        return r;
    }

    // -- Direct_Short_Swap_

    @Benchmark
    public void testDirectLoopPutShortSwap() {
        for (int i = 0; i < directByteBufferSwap.capacity(); i+=2) {
            directByteBufferSwap.putShort(i, shortValue);
        }
    }

    @Benchmark
    public int testDirectLoopGetShortSwap() {
        int r = 0;
        for (int i = 0; i < directByteBufferSwap.capacity(); i+=2) {
            r += directByteBufferSwap.getShort(i);
        }
        return r;
    }

    // -- Direct_Short__RO

    @Benchmark
    public int testDirectLoopGetShortRO() {
        int r = 0;
        for (int i = 0; i < directByteBufferRO.capacity(); i+=2) {
            r += directByteBufferRO.getShort(i);
        }
        return r;
    }

    // -- Direct_Short__

    @Benchmark
    public void testDirectLoopPutShort() {
        for (int i = 0; i < directByteBuffer.capacity(); i+=2) {
            directByteBuffer.putShort(i, shortValue);
        }
    }

    @Benchmark
    public int testDirectLoopGetShort() {
        int r = 0;
        for (int i = 0; i < directByteBuffer.capacity(); i+=2) {
            r += directByteBuffer.getShort(i);
        }
        return r;
    }

    // -- Heap_Int_Swap_RO

    @Benchmark
    public int testHeapLoopGetIntSwapRO() {
        int r = 0;
        for (int i = 0; i < heapByteBufferSwapRO.capacity(); i+=4) {
            r += heapByteBufferSwapRO.getInt(i);
        }
        return r;
    }

    // -- Heap_Int_Swap_

    @Benchmark
    public void testHeapLoopPutIntSwap() {
        for (int i = 0; i < heapByteBufferSwap.capacity(); i+=4) {
            heapByteBufferSwap.putInt(i, intValue);
        }
    }

    @Benchmark
    public int testHeapLoopGetIntSwap() {
        int r = 0;
        for (int i = 0; i < heapByteBufferSwap.capacity(); i+=4) {
            r += heapByteBufferSwap.getInt(i);
        }
        return r;
    }

    // -- Heap_Int__RO

    @Benchmark
    public int testHeapLoopGetIntRO() {
        int r = 0;
        for (int i = 0; i < heapByteBufferRO.capacity(); i+=4) {
            r += heapByteBufferRO.getInt(i);
        }
        return r;
    }

    // -- Heap_Int__

    @Benchmark
    public void testHeapLoopPutInt() {
        for (int i = 0; i < heapByteBuffer.capacity(); i+=4) {
            heapByteBuffer.putInt(i, intValue);
        }
    }

    @Benchmark
    public int testHeapLoopGetInt() {
        int r = 0;
        for (int i = 0; i < heapByteBuffer.capacity(); i+=4) {
            r += heapByteBuffer.getInt(i);
        }
        return r;
    }

    // -- Direct_Int_Swap_RO

    @Benchmark
    public int testDirectLoopGetIntSwapRO() {
        int r = 0;
        for (int i = 0; i < directByteBufferSwapRO.capacity(); i+=4) {
            r += directByteBufferSwapRO.getInt(i);
        }
        return r;
    }

    // -- Direct_Int_Swap_

    @Benchmark
    public void testDirectLoopPutIntSwap() {
        for (int i = 0; i < directByteBufferSwap.capacity(); i+=4) {
            directByteBufferSwap.putInt(i, intValue);
        }
    }

    @Benchmark
    public int testDirectLoopGetIntSwap() {
        int r = 0;
        for (int i = 0; i < directByteBufferSwap.capacity(); i+=4) {
            r += directByteBufferSwap.getInt(i);
        }
        return r;
    }

    // -- Direct_Int__RO

    @Benchmark
    public int testDirectLoopGetIntRO() {
        int r = 0;
        for (int i = 0; i < directByteBufferRO.capacity(); i+=4) {
            r += directByteBufferRO.getInt(i);
        }
        return r;
    }

    // -- Direct_Int__

    @Benchmark
    public void testDirectLoopPutInt() {
        for (int i = 0; i < directByteBuffer.capacity(); i+=4) {
            directByteBuffer.putInt(i, intValue);
        }
    }

    @Benchmark
    public int testDirectLoopGetInt() {
        int r = 0;
        for (int i = 0; i < directByteBuffer.capacity(); i+=4) {
            r += directByteBuffer.getInt(i);
        }
        return r;
    }

    // -- Heap_Long_Swap_RO

    @Benchmark
    public int testHeapLoopGetLongSwapRO() {
        int r = 0;
        for (int i = 0; i < heapByteBufferSwapRO.capacity(); i+=8) {
            r += heapByteBufferSwapRO.getLong(i);
        }
        return r;
    }

    // -- Heap_Long_Swap_

    @Benchmark
    public void testHeapLoopPutLongSwap() {
        for (int i = 0; i < heapByteBufferSwap.capacity(); i+=8) {
            heapByteBufferSwap.putLong(i, longValue);
        }
    }

    @Benchmark
    public int testHeapLoopGetLongSwap() {
        int r = 0;
        for (int i = 0; i < heapByteBufferSwap.capacity(); i+=8) {
            r += heapByteBufferSwap.getLong(i);
        }
        return r;
    }

    // -- Heap_Long__RO

    @Benchmark
    public int testHeapLoopGetLongRO() {
        int r = 0;
        for (int i = 0; i < heapByteBufferRO.capacity(); i+=8) {
            r += heapByteBufferRO.getLong(i);
        }
        return r;
    }

    // -- Heap_Long__

    @Benchmark
    public void testHeapLoopPutLong() {
        for (int i = 0; i < heapByteBuffer.capacity(); i+=8) {
            heapByteBuffer.putLong(i, longValue);
        }
    }

    @Benchmark
    public int testHeapLoopGetLong() {
        int r = 0;
        for (int i = 0; i < heapByteBuffer.capacity(); i+=8) {
            r += heapByteBuffer.getLong(i);
        }
        return r;
    }

    // -- Direct_Long_Swap_RO

    @Benchmark
    public int testDirectLoopGetLongSwapRO() {
        int r = 0;
        for (int i = 0; i < directByteBufferSwapRO.capacity(); i+=8) {
            r += directByteBufferSwapRO.getLong(i);
        }
        return r;
    }

    // -- Direct_Long_Swap_

    @Benchmark
    public void testDirectLoopPutLongSwap() {
        for (int i = 0; i < directByteBufferSwap.capacity(); i+=8) {
            directByteBufferSwap.putLong(i, longValue);
        }
    }

    @Benchmark
    public int testDirectLoopGetLongSwap() {
        int r = 0;
        for (int i = 0; i < directByteBufferSwap.capacity(); i+=8) {
            r += directByteBufferSwap.getLong(i);
        }
        return r;
    }

    // -- Direct_Long__RO

    @Benchmark
    public int testDirectLoopGetLongRO() {
        int r = 0;
        for (int i = 0; i < directByteBufferRO.capacity(); i+=8) {
            r += directByteBufferRO.getLong(i);
        }
        return r;
    }

    // -- Direct_Long__

    @Benchmark
    public void testDirectLoopPutLong() {
        for (int i = 0; i < directByteBuffer.capacity(); i+=8) {
            directByteBuffer.putLong(i, longValue);
        }
    }

    @Benchmark
    public int testDirectLoopGetLong() {
        int r = 0;
        for (int i = 0; i < directByteBuffer.capacity(); i+=8) {
            r += directByteBuffer.getLong(i);
        }
        return r;
    }

    // -- Heap_Float_Swap_RO

    @Benchmark
    public int testHeapLoopGetFloatSwapRO() {
        int r = 0;
        for (int i = 0; i < heapByteBufferSwapRO.capacity(); i+=4) {
            r += heapByteBufferSwapRO.getFloat(i);
        }
        return r;
    }

    // -- Heap_Float_Swap_

    @Benchmark
    public void testHeapLoopPutFloatSwap() {
        for (int i = 0; i < heapByteBufferSwap.capacity(); i+=4) {
            heapByteBufferSwap.putFloat(i, floatValue);
        }
    }

    @Benchmark
    public int testHeapLoopGetFloatSwap() {
        int r = 0;
        for (int i = 0; i < heapByteBufferSwap.capacity(); i+=4) {
            r += heapByteBufferSwap.getFloat(i);
        }
        return r;
    }

    // -- Heap_Float__RO

    @Benchmark
    public int testHeapLoopGetFloatRO() {
        int r = 0;
        for (int i = 0; i < heapByteBufferRO.capacity(); i+=4) {
            r += heapByteBufferRO.getFloat(i);
        }
        return r;
    }

    // -- Heap_Float__

    @Benchmark
    public void testHeapLoopPutFloat() {
        for (int i = 0; i < heapByteBuffer.capacity(); i+=4) {
            heapByteBuffer.putFloat(i, floatValue);
        }
    }

    @Benchmark
    public int testHeapLoopGetFloat() {
        int r = 0;
        for (int i = 0; i < heapByteBuffer.capacity(); i+=4) {
            r += heapByteBuffer.getFloat(i);
        }
        return r;
    }

    // -- Direct_Float_Swap_RO

    @Benchmark
    public int testDirectLoopGetFloatSwapRO() {
        int r = 0;
        for (int i = 0; i < directByteBufferSwapRO.capacity(); i+=4) {
            r += directByteBufferSwapRO.getFloat(i);
        }
        return r;
    }

    // -- Direct_Float_Swap_

    @Benchmark
    public void testDirectLoopPutFloatSwap() {
        for (int i = 0; i < directByteBufferSwap.capacity(); i+=4) {
            directByteBufferSwap.putFloat(i, floatValue);
        }
    }

    @Benchmark
    public int testDirectLoopGetFloatSwap() {
        int r = 0;
        for (int i = 0; i < directByteBufferSwap.capacity(); i+=4) {
            r += directByteBufferSwap.getFloat(i);
        }
        return r;
    }

    // -- Direct_Float__RO

    @Benchmark
    public int testDirectLoopGetFloatRO() {
        int r = 0;
        for (int i = 0; i < directByteBufferRO.capacity(); i+=4) {
            r += directByteBufferRO.getFloat(i);
        }
        return r;
    }

    // -- Direct_Float__

    @Benchmark
    public void testDirectLoopPutFloat() {
        for (int i = 0; i < directByteBuffer.capacity(); i+=4) {
            directByteBuffer.putFloat(i, floatValue);
        }
    }

    @Benchmark
    public int testDirectLoopGetFloat() {
        int r = 0;
        for (int i = 0; i < directByteBuffer.capacity(); i+=4) {
            r += directByteBuffer.getFloat(i);
        }
        return r;
    }

    // -- Heap_Double_Swap_RO

    @Benchmark
    public int testHeapLoopGetDoubleSwapRO() {
        int r = 0;
        for (int i = 0; i < heapByteBufferSwapRO.capacity(); i+=8) {
            r += heapByteBufferSwapRO.getDouble(i);
        }
        return r;
    }

    // -- Heap_Double_Swap_

    @Benchmark
    public void testHeapLoopPutDoubleSwap() {
        for (int i = 0; i < heapByteBufferSwap.capacity(); i+=8) {
            heapByteBufferSwap.putDouble(i, doubleValue);
        }
    }

    @Benchmark
    public int testHeapLoopGetDoubleSwap() {
        int r = 0;
        for (int i = 0; i < heapByteBufferSwap.capacity(); i+=8) {
            r += heapByteBufferSwap.getDouble(i);
        }
        return r;
    }

    // -- Heap_Double__RO

    @Benchmark
    public int testHeapLoopGetDoubleRO() {
        int r = 0;
        for (int i = 0; i < heapByteBufferRO.capacity(); i+=8) {
            r += heapByteBufferRO.getDouble(i);
        }
        return r;
    }

    // -- Heap_Double__

    @Benchmark
    public void testHeapLoopPutDouble() {
        for (int i = 0; i < heapByteBuffer.capacity(); i+=8) {
            heapByteBuffer.putDouble(i, doubleValue);
        }
    }

    @Benchmark
    public int testHeapLoopGetDouble() {
        int r = 0;
        for (int i = 0; i < heapByteBuffer.capacity(); i+=8) {
            r += heapByteBuffer.getDouble(i);
        }
        return r;
    }

    // -- Direct_Double_Swap_RO

    @Benchmark
    public int testDirectLoopGetDoubleSwapRO() {
        int r = 0;
        for (int i = 0; i < directByteBufferSwapRO.capacity(); i+=8) {
            r += directByteBufferSwapRO.getDouble(i);
        }
        return r;
    }

    // -- Direct_Double_Swap_

    @Benchmark
    public void testDirectLoopPutDoubleSwap() {
        for (int i = 0; i < directByteBufferSwap.capacity(); i+=8) {
            directByteBufferSwap.putDouble(i, doubleValue);
        }
    }

    @Benchmark
    public int testDirectLoopGetDoubleSwap() {
        int r = 0;
        for (int i = 0; i < directByteBufferSwap.capacity(); i+=8) {
            r += directByteBufferSwap.getDouble(i);
        }
        return r;
    }

    // -- Direct_Double__RO

    @Benchmark
    public int testDirectLoopGetDoubleRO() {
        int r = 0;
        for (int i = 0; i < directByteBufferRO.capacity(); i+=8) {
            r += directByteBufferRO.getDouble(i);
        }
        return r;
    }

    // -- Direct_Double__

    @Benchmark
    public void testDirectLoopPutDouble() {
        for (int i = 0; i < directByteBuffer.capacity(); i+=8) {
            directByteBuffer.putDouble(i, doubleValue);
        }
    }

    @Benchmark
    public int testDirectLoopGetDouble() {
        int r = 0;
        for (int i = 0; i < directByteBuffer.capacity(); i+=8) {
            r += directByteBuffer.getDouble(i);
        }
        return r;
    }
}
