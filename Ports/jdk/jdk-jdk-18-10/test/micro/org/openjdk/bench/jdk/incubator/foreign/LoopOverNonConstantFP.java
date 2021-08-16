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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.TimeUnit;

import static jdk.incubator.foreign.MemoryLayout.PathElement.sequenceElement;
import static jdk.incubator.foreign.MemoryLayouts.JAVA_DOUBLE;

@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@State(org.openjdk.jmh.annotations.Scope.Thread)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(3)
public class LoopOverNonConstantFP {

    static final Unsafe unsafe = Utils.unsafe;

    static final int ELEM_SIZE = 1_000_000;
    static final int CARRIER_SIZE = (int)JAVA_DOUBLE.byteSize();
    static final int ALLOC_SIZE = ELEM_SIZE * CARRIER_SIZE;

    MemorySegment segmentIn, segmentOut;
    long unsafe_addrIn, unsafe_addrOut;
    ByteBuffer byteBufferIn, byteBufferOut;

    @Setup
    public void setup() {
        unsafe_addrIn = unsafe.allocateMemory(ALLOC_SIZE);
        unsafe_addrOut = unsafe.allocateMemory(ALLOC_SIZE);
        for (int i = 0; i < ELEM_SIZE; i++) {
            unsafe.putDouble(unsafe_addrIn + (i * CARRIER_SIZE), i);
        }
        for (int i = 0; i < ELEM_SIZE; i++) {
            unsafe.putDouble(unsafe_addrOut + (i * CARRIER_SIZE), i);
        }
        segmentIn = MemorySegment.allocateNative(ALLOC_SIZE, ResourceScope.newConfinedScope());
        segmentOut = MemorySegment.allocateNative(ALLOC_SIZE, ResourceScope.newConfinedScope());
        for (int i = 0; i < ELEM_SIZE; i++) {
            MemoryAccess.setDoubleAtIndex(segmentIn, i, i);
        }
        for (int i = 0; i < ELEM_SIZE; i++) {
            MemoryAccess.setDoubleAtIndex(segmentOut, i, i);
        }
        byteBufferIn = ByteBuffer.allocateDirect(ALLOC_SIZE).order(ByteOrder.nativeOrder());
        byteBufferOut = ByteBuffer.allocateDirect(ALLOC_SIZE).order(ByteOrder.nativeOrder());
        for (int i = 0; i < ELEM_SIZE; i++) {
            byteBufferIn.putDouble(i * CARRIER_SIZE , i);
        }
        for (int i = 0; i < ELEM_SIZE; i++) {
            byteBufferOut.putDouble(i * CARRIER_SIZE , i);
        }
    }

    @TearDown
    public void tearDown() {
        segmentIn.scope().close();
        segmentOut.scope().close();
        unsafe.invokeCleaner(byteBufferIn);
        unsafe.invokeCleaner(byteBufferOut);
        unsafe.freeMemory(unsafe_addrIn);
        unsafe.freeMemory(unsafe_addrOut);
    }

    @Benchmark
    public void unsafe_loop() {
        for (int i = 0; i < ELEM_SIZE; i ++) {
            unsafe.putDouble(unsafe_addrOut + (i * CARRIER_SIZE),
                    unsafe.getDouble(unsafe_addrIn + (i * CARRIER_SIZE)) +
                    unsafe.getDouble(unsafe_addrOut + (i * CARRIER_SIZE)));
        }
    }

    @Benchmark
    public void segment_loop() {
        for (int i = 0; i < ELEM_SIZE; i ++) {
            MemoryAccess.setDoubleAtIndex(segmentOut, i,
                    MemoryAccess.getDoubleAtIndex(segmentIn, i) +
                    MemoryAccess.getDoubleAtIndex(segmentOut, i));
        }
    }

    @Benchmark
    public void BB_loop() {
        for (int i = 0; i < ELEM_SIZE; i++) {
            byteBufferOut.putDouble(i * CARRIER_SIZE,
                    byteBufferIn.getDouble(i * CARRIER_SIZE) +
                    byteBufferOut.getDouble(i * CARRIER_SIZE));
        }
    }
}
