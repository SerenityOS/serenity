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

import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.SegmentAllocator;
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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.TimeUnit;

import static jdk.incubator.foreign.MemoryLayout.PathElement.sequenceElement;
import static jdk.incubator.foreign.MemoryLayouts.JAVA_INT;

@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@State(org.openjdk.jmh.annotations.Scope.Thread)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(value = 3, jvmArgsAppend = { "--add-modules=jdk.incubator.foreign" })
public class LoopOverNew {

    static final Unsafe unsafe = Utils.unsafe;

    static final int ELEM_SIZE = 1_000_000;
    static final int CARRIER_SIZE = (int)JAVA_INT.byteSize();
    static final int ALLOC_SIZE = ELEM_SIZE * CARRIER_SIZE;
    static final MemoryLayout ALLOC_LAYOUT = MemoryLayout.sequenceLayout(ELEM_SIZE, JAVA_INT);

    static final VarHandle VH_int = MemoryLayout.sequenceLayout(JAVA_INT).varHandle(int.class, sequenceElement());

    final ResourceScope scope = ResourceScope.newConfinedScope();
    final SegmentAllocator recyclingAlloc = SegmentAllocator.ofSegment(MemorySegment.allocateNative(ALLOC_LAYOUT, scope));

    @TearDown
    public void tearDown() throws Throwable {
        scope.close();
    }

    @Benchmark
    public void unsafe_loop() {
        long unsafe_addr = unsafe.allocateMemory(ALLOC_SIZE);
        for (int i = 0; i < ELEM_SIZE; i++) {
            unsafe.putInt(unsafe_addr + (i * CARRIER_SIZE) , i);
        }
        unsafe.freeMemory(unsafe_addr);
    }

    @Benchmark
    public void segment_loop_confined() {
        try (ResourceScope scope = ResourceScope.newConfinedScope()) {
            MemorySegment segment = MemorySegment.allocateNative(ALLOC_SIZE, 4, scope);
            for (int i = 0; i < ELEM_SIZE; i++) {
                VH_int.set(segment, (long) i, i);
            }
        }
    }

    @Benchmark
    public void segment_loop_shared() {
        try (ResourceScope scope = ResourceScope.newSharedScope()) {
            MemorySegment segment = MemorySegment.allocateNative(ALLOC_SIZE, 4, scope);
            for (int i = 0; i < ELEM_SIZE; i++) {
                VH_int.set(segment, (long) i, i);
            }
        }
    }

    @Benchmark
    public void segment_loop_recycle() {
        MemorySegment segment = recyclingAlloc.allocate(ALLOC_SIZE, 4);
        for (int i = 0; i < ELEM_SIZE; i++) {
            VH_int.set(segment, (long) i, i);
        }
    }

    @Benchmark
    public void buffer_loop() {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(ALLOC_SIZE).order(ByteOrder.nativeOrder());
        for (int i = 0; i < ELEM_SIZE; i++) {
            byteBuffer.putInt(i * CARRIER_SIZE , i);
        }
        unsafe.invokeCleaner(byteBuffer);
    }

    // hack to even out calls to System::gc, which allows us to compare how the implicit segment deallocation
    // fares compared with ByteBuffer; if there's no call to System.gc() we end up comparing how well the two
    // act under significant native memory pressure, and here the ByteBuffer API has more juice, since it features
    // a complex exponential back off with multiple GC retries (see ByteBuffer::allocateDirect). Of course, we
    // don't care about those cases with segments, as if clients need to allocate/free very frequently
    // they should just use deterministic deallocation (with confined scope) instead, which delivers much
    // better performances anyway.
    static byte gcCount = 0;

    @Benchmark
    public void buffer_loop_implicit() {
        if (gcCount++ == 0) System.gc(); // GC when we overflow
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(ALLOC_SIZE).order(ByteOrder.nativeOrder());
        for (int i = 0; i < ELEM_SIZE; i++) {
            byteBuffer.putInt(i * CARRIER_SIZE , i);
        }
    }

    @Benchmark
    public void segment_loop_implicit() {
        if (gcCount++ == 0) System.gc(); // GC when we overflow
        MemorySegment segment = MemorySegment.allocateNative(ALLOC_SIZE, 4, ResourceScope.newImplicitScope());
        for (int i = 0; i < ELEM_SIZE; i++) {
            VH_int.set(segment, (long) i, i);
        }
    }
}
