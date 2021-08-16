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
import jdk.incubator.foreign.MemoryLayouts;
import jdk.incubator.foreign.ResourceScope;
import jdk.incubator.foreign.SequenceLayout;
import sun.misc.Unsafe;
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

import jdk.incubator.foreign.MemorySegment;
import java.lang.invoke.VarHandle;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Spliterator;
import java.util.concurrent.CountedCompleter;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

import static jdk.incubator.foreign.MemoryLayout.PathElement.sequenceElement;
import static jdk.incubator.foreign.MemoryLayouts.JAVA_INT;

@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@State(org.openjdk.jmh.annotations.Scope.Thread)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(value = 3, jvmArgsAppend = { "--add-modules=jdk.incubator.foreign" })
public class ParallelSum {

    final static int CARRIER_SIZE = 4;
    final static int ALLOC_SIZE = CARRIER_SIZE * 1024 * 1024 * 256;
    final static int ELEM_SIZE = ALLOC_SIZE / CARRIER_SIZE;
    static final VarHandle VH_int = MemoryLayout.sequenceLayout(JAVA_INT).varHandle(int.class, sequenceElement());

    final static MemoryLayout ELEM_LAYOUT = MemoryLayouts.JAVA_INT;
    final static int BULK_FACTOR = 512;
    final static SequenceLayout ELEM_LAYOUT_BULK = MemoryLayout.sequenceLayout(BULK_FACTOR, ELEM_LAYOUT);

    static final Unsafe unsafe = Utils.unsafe;

    MemorySegment segment;
    long address;

    @Setup
    public void setup() {
        address = unsafe.allocateMemory(ALLOC_SIZE);
        for (int i = 0; i < ELEM_SIZE; i++) {
            unsafe.putInt(address + (i * CARRIER_SIZE), i);
        }
        segment = MemorySegment.allocateNative(ALLOC_SIZE, CARRIER_SIZE, ResourceScope.newSharedScope());
        for (int i = 0; i < ELEM_SIZE; i++) {
            VH_int.set(segment, (long) i, i);
        }
    }

    @TearDown
    public void tearDown() throws Throwable {
        unsafe.freeMemory(address);
        segment.scope().close();
    }

    @Benchmark
    public int segment_serial() {
        int res = 0;
        for (int i = 0; i < ELEM_SIZE; i++) {
            res += (int)VH_int.get(segment, (long) i);
        }
        return res;
    }

    @Benchmark
    public int unsafe_serial() {
        int res = 0;
        for (int i = 0; i < ELEM_SIZE; i++) {
            res += unsafe.getInt(address + (i * CARRIER_SIZE));
        }
        return res;
    }

    @Benchmark
    public int segment_parallel() {
        return new SumSegment(segment.spliterator(ELEM_LAYOUT), SEGMENT_TO_INT).invoke();
    }

    @Benchmark
    public int segment_parallel_bulk() {
        return new SumSegment(segment.spliterator(ELEM_LAYOUT_BULK), SEGMENT_TO_INT_BULK).invoke();
    }

    @Benchmark
    public int segment_stream_parallel() {
        return segment.elements(ELEM_LAYOUT).parallel().mapToInt(SEGMENT_TO_INT).sum();
    }

    @Benchmark
    public int segment_stream_parallel_bulk() {
        return segment.elements(ELEM_LAYOUT_BULK).parallel().mapToInt(SEGMENT_TO_INT_BULK).sum();
    }

    final static ToIntFunction<MemorySegment> SEGMENT_TO_INT = slice ->
            (int) VH_int.get(slice, 0L);

    final static ToIntFunction<MemorySegment> SEGMENT_TO_INT_BULK = slice -> {
        int res = 0;
        for (int i = 0; i < BULK_FACTOR ; i++) {
            res += (int)VH_int.get(slice, (long) i);
        }
        return res;
    };

    @Benchmark
    public Optional<MemorySegment> segment_stream_findany_serial() {
        return segment.elements(ELEM_LAYOUT)
                .filter(FIND_SINGLE)
                .findAny();
    }

    @Benchmark
    public Optional<MemorySegment> segment_stream_findany_parallel() {
        return segment.elements(ELEM_LAYOUT).parallel()
                .filter(FIND_SINGLE)
                .findAny();
    }

    @Benchmark
    public Optional<MemorySegment> segment_stream_findany_serial_bulk() {
        return segment.elements(ELEM_LAYOUT_BULK)
                .filter(FIND_BULK)
                .findAny();
    }

    @Benchmark
    public Optional<MemorySegment> segment_stream_findany_parallel_bulk() {
        return segment.elements(ELEM_LAYOUT_BULK).parallel()
                .filter(FIND_BULK)
                .findAny();
    }

    final static Predicate<MemorySegment> FIND_SINGLE = slice ->
            (int)VH_int.get(slice, 0L) == (ELEM_SIZE - 1);

    final static Predicate<MemorySegment> FIND_BULK = slice -> {
        for (int i = 0; i < BULK_FACTOR ; i++) {
            if ((int)VH_int.get(slice, (long)i) == (ELEM_SIZE - 1)) {
                return true;
            }
        }
        return false;
    };

    @Benchmark
    public int unsafe_parallel() {
        return new SumUnsafe(address, 0, ALLOC_SIZE / CARRIER_SIZE).invoke();
    }

    static class SumUnsafe extends RecursiveTask<Integer> {

        final static int SPLIT_THRESHOLD = 4 * 1024 * 8;

        private final long address;
        private final int start, length;

        SumUnsafe(long address, int start, int length) {
            this.address = address;
            this.start = start;
            this.length = length;
        }

        @Override
        protected Integer compute() {
            if (length > SPLIT_THRESHOLD) {
                int rem = length % 2;
                int split = length / 2;
                int lobound = split;
                int hibound = lobound + rem;
                SumUnsafe s1 = new SumUnsafe(address, start, lobound);
                SumUnsafe s2 = new SumUnsafe(address, start + lobound, hibound);
                s1.fork();
                s2.fork();
                return s1.join() + s2.join();
            } else {
                int res = 0;
                for (int i = 0; i < length; i ++) {
                    res += unsafe.getInt(address + (start + i) * CARRIER_SIZE);
                }
                return res;
            }
        }
    }

    static class SumSegment extends CountedCompleter<Integer> {

        final static int SPLIT_THRESHOLD = 1024 * 8;

        int localSum = 0;
        private final ToIntFunction<MemorySegment> mapper;
        List<SumSegment> children = new LinkedList<>();

        private Spliterator<MemorySegment> segmentSplitter;

        SumSegment(Spliterator<MemorySegment> segmentSplitter, ToIntFunction<MemorySegment> mapper) {
            this(null, segmentSplitter, mapper);
        }

        SumSegment(SumSegment parent, Spliterator<MemorySegment> segmentSplitter, ToIntFunction<MemorySegment> mapper) {
            super(parent);
            this.segmentSplitter = segmentSplitter;
            this.mapper = mapper;
        }

        @Override
        public void compute() {
            Spliterator<MemorySegment> sub;
            while (segmentSplitter.estimateSize() > SPLIT_THRESHOLD &&
                    (sub = segmentSplitter.trySplit()) != null) {
                addToPendingCount(1);
                SumSegment child = new SumSegment(this, sub, mapper);
                children.add(child);
                child.fork();
            }
            segmentSplitter.forEachRemaining(s -> {
                localSum += mapper.applyAsInt(s);
            });
            propagateCompletion();
        }

        @Override
        public Integer getRawResult() {
            int sum = localSum;
            for (SumSegment c : children) {
                sum += c.getRawResult();
            }
            children = null;
            return sum;
        }
    }
}
