/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

import jdk.incubator.foreign.ResourceScope;
import jdk.incubator.foreign.SegmentAllocator;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import sun.misc.Unsafe;

import jdk.incubator.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static jdk.incubator.foreign.MemoryLayouts.JAVA_INT;

@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@State(org.openjdk.jmh.annotations.Scope.Thread)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(value = 3, jvmArgsAppend = { "--add-modules=jdk.incubator.foreign" })
public class BulkMismatchAcquire {

    public enum ScopeKind {
        CONFINED(ResourceScope::newConfinedScope),
        SHARED(ResourceScope::newSharedScope),
        IMPLICIT(ResourceScope::newImplicitScope);

        final Supplier<ResourceScope> scopeFactory;

        ScopeKind(Supplier<ResourceScope> scopeFactory) {
            this.scopeFactory = scopeFactory;
        }

        ResourceScope makeScope() {
            return scopeFactory.get();
        }
    }

    @Param({"CONFINED", "SHARED", "IMPLICIT"})
    public ScopeKind scopeKind;

    // large(ish) segments/buffers with same content, 0, for mismatch, non-multiple-of-8 sized
    static final int SIZE_WITH_TAIL = (1024 * 1024) + 7;

    ResourceScope scope;
    MemorySegment mismatchSegmentLarge1;
    MemorySegment mismatchSegmentLarge2;
    ByteBuffer mismatchBufferLarge1;
    ByteBuffer mismatchBufferLarge2;
    MemorySegment mismatchSegmentSmall1;
    MemorySegment mismatchSegmentSmall2;
    ByteBuffer mismatchBufferSmall1;
    ByteBuffer mismatchBufferSmall2;

    @Setup
    public void setup() {
        scope = scopeKind.makeScope();
        mismatchSegmentLarge1 = MemorySegment.allocateNative(SIZE_WITH_TAIL, scope);
        mismatchSegmentLarge2 = MemorySegment.allocateNative(SIZE_WITH_TAIL, scope);
        mismatchBufferLarge1 = ByteBuffer.allocateDirect(SIZE_WITH_TAIL);
        mismatchBufferLarge2 = ByteBuffer.allocateDirect(SIZE_WITH_TAIL);

        // mismatch at first byte
        mismatchSegmentSmall1 = MemorySegment.allocateNative(7, scope);
        mismatchSegmentSmall2 = MemorySegment.allocateNative(7, scope);
        mismatchBufferSmall1 = ByteBuffer.allocateDirect(7);
        mismatchBufferSmall2 = ByteBuffer.allocateDirect(7);
        {
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
    }

    @TearDown
    public void tearDown() {
        if (!scope.isImplicit())
            scope.close();
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public long mismatch_large_segment() {
        return mismatchSegmentLarge1.mismatch(mismatchSegmentLarge2);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public long mismatch_large_segment_acquire() {
        var handle = mismatchSegmentLarge1.scope().acquire();
        try {
            return mismatchSegmentLarge1.mismatch(mismatchSegmentLarge2);
        } finally {
            mismatchSegmentLarge1.scope().release(handle);
        }
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
    public long mismatch_small_segment_acquire() {
        var handle = mismatchSegmentLarge1.scope().acquire();
        try {
            return mismatchSegmentSmall1.mismatch(mismatchSegmentSmall2);
        } finally {
            mismatchSegmentLarge1.scope().release(handle);
        }
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public int mismatch_small_bytebuffer() {
        return mismatchBufferSmall1.mismatch(mismatchBufferSmall2);
    }
}
