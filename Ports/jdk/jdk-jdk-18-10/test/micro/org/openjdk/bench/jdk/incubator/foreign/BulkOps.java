/*
 *  Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 *
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *   Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 *
 */

package org.openjdk.bench.jdk.incubator.foreign;

import jdk.incubator.foreign.ResourceScope;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import sun.misc.Unsafe;

import jdk.incubator.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import static jdk.incubator.foreign.MemoryLayouts.JAVA_INT;

@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@State(org.openjdk.jmh.annotations.Scope.Thread)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(value = 3, jvmArgsAppend = { "--add-modules=jdk.incubator.foreign" })
public class BulkOps {

    static final Unsafe unsafe = Utils.unsafe;

    static final int ELEM_SIZE = 1_000_000;
    static final int CARRIER_SIZE = (int)JAVA_INT.byteSize();
    static final int ALLOC_SIZE = ELEM_SIZE * CARRIER_SIZE;

    static final long unsafe_addr = unsafe.allocateMemory(ALLOC_SIZE);
    static final MemorySegment segment = MemorySegment.allocateNative(ALLOC_SIZE, ResourceScope.newConfinedScope());

    static final int[] bytes = new int[ELEM_SIZE];
    static final MemorySegment bytesSegment = MemorySegment.ofArray(bytes);
    static final int UNSAFE_INT_OFFSET = unsafe.arrayBaseOffset(int[].class);

    // large(ish) segments/buffers with same content, 0, for mismatch, non-multiple-of-8 sized
    static final int SIZE_WITH_TAIL = (1024 * 1024) + 7;
    static final MemorySegment mismatchSegmentLarge1 = MemorySegment.allocateNative(SIZE_WITH_TAIL, ResourceScope.newConfinedScope());
    static final MemorySegment mismatchSegmentLarge2 = MemorySegment.allocateNative(SIZE_WITH_TAIL, ResourceScope.newConfinedScope());
    static final ByteBuffer mismatchBufferLarge1 = ByteBuffer.allocateDirect(SIZE_WITH_TAIL);
    static final ByteBuffer mismatchBufferLarge2 = ByteBuffer.allocateDirect(SIZE_WITH_TAIL);

    // mismatch at first byte
    static final MemorySegment mismatchSegmentSmall1 = MemorySegment.allocateNative(7, ResourceScope.newConfinedScope());
    static final MemorySegment mismatchSegmentSmall2 = MemorySegment.allocateNative(7, ResourceScope.newConfinedScope());
    static final ByteBuffer mismatchBufferSmall1 = ByteBuffer.allocateDirect(7);
    static final ByteBuffer mismatchBufferSmall2 = ByteBuffer.allocateDirect(7);
    static {
        mismatchSegmentSmall1.fill((byte) 0xFF);
        mismatchBufferSmall1.put((byte) 0xFF).clear();
        // verify expected mismatch indices
        long si = mismatchSegmentLarge1.mismatch(mismatchSegmentLarge2);
        if (si != -1)
            throw new AssertionError("Unexpected mismatch index:" + si);
        int bi = mismatchBufferLarge1.mismatch(mismatchBufferLarge2);
        if (bi != -1)
            throw new AssertionError("Unexpected mismatch index:" + bi);
        si = mismatchSegmentSmall1.mismatch(mismatchSegmentSmall2);
        if (si != 0)
            throw new AssertionError("Unexpected mismatch index:" + si);
        bi = mismatchBufferSmall1.mismatch(mismatchBufferSmall2);
        if (bi != 0)
            throw new AssertionError("Unexpected mismatch index:" + bi);
    }

    static {
        for (int i = 0 ; i < bytes.length ; i++) {
            bytes[i] = i;
        }
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void unsafe_fill() {
        unsafe.setMemory(unsafe_addr, ALLOC_SIZE, (byte)42);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void segment_fill() {
        segment.fill((byte)42);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void unsafe_copy() {
        unsafe.copyMemory(bytes, UNSAFE_INT_OFFSET, null, unsafe_addr, ALLOC_SIZE);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void segment_copy() {
        segment.copyFrom(bytesSegment);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public long mismatch_large_segment() {
        return mismatchSegmentLarge1.mismatch(mismatchSegmentLarge2);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public int mismatch_large_bytebuffer() {
        return mismatchBufferLarge1.mismatch(mismatchBufferLarge2);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public long mismatch_small_segment() {
        return mismatchSegmentSmall1.mismatch(mismatchSegmentSmall2);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public int mismatch_small_bytebuffer() {
        return mismatchBufferSmall1.mismatch(mismatchBufferSmall2);
    }
}
