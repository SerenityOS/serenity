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

import jdk.incubator.foreign.MemoryHandles;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemoryLayouts;
import jdk.incubator.foreign.MemorySegment;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@State(org.openjdk.jmh.annotations.Scope.Thread)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(value = 3, jvmArgsAppend = { "--add-modules=jdk.incubator.foreign" })
public class TestAdaptVarHandles {

    static class IntBox {

        private final int value;

        IntBox(int value) {
            this.value = value;
        }

        int intValue() {
            return value;
        }
    }

    static final int ELEM_SIZE = 1_000_000;

    static final MethodHandle INT_TO_INTBOX;
    static final MethodHandle INTBOX_TO_INT;

    static {
        try {
            INT_TO_INTBOX = MethodHandles.lookup()
                    .findConstructor(IntBox.class, MethodType.methodType(void.class, int.class));
            INTBOX_TO_INT = MethodHandles.lookup()
                    .findVirtual(IntBox.class, "intValue", MethodType.methodType(int.class));
        } catch (Throwable ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    static final VarHandle VH_int = MethodHandles.arrayElementVarHandle(int[].class);

    static final VarHandle VH_box_int = MemoryHandles.filterValue(VH_int, INTBOX_TO_INT, INT_TO_INTBOX);

    static final VarHandle VH_addr_int = MemoryLayout.sequenceLayout(MemoryLayouts.JAVA_INT)
            .varHandle(int.class, MemoryLayout.PathElement.sequenceElement());

    static final VarHandle VH_addr_box_int = MemoryHandles.filterValue(VH_addr_int, INTBOX_TO_INT, INT_TO_INTBOX);

    static final MethodHandle MH_int = VH_int.toMethodHandle(VarHandle.AccessMode.GET);

    static final MethodHandle MH_box_int = MethodHandles.filterReturnValue(MH_int, INT_TO_INTBOX);

    int[] base = new int[ELEM_SIZE];
    MemorySegment segment = MemorySegment.ofArray(base);

    @Setup
    public void setup() {
        for (int i = 0; i < ELEM_SIZE; i++) {
            base[i] = i;
        }
    }

    @Benchmark
    public int vh_loop() throws Throwable {
        int sum = 0;
        for (int i = 0; i < ELEM_SIZE; i++) {
            sum += (int)VH_int.get(base, i);
        }
        return sum;
    }

    @Benchmark
    public int vh_box_loop() throws Throwable {
        int sum = 0;
        for (int i = 0; i < ELEM_SIZE; i++) {
            sum += ((IntBox)VH_box_int.get(base, i)).intValue();
        }
        return sum;
    }

    @Benchmark
    public int mh_loop() throws Throwable {
        int sum = 0;
        for (int i = 0; i < ELEM_SIZE; i++) {
            sum += (int)MH_int.invokeExact(base, i);
        }
        return sum;
    }

    @Benchmark
    public int mh_box_loop() throws Throwable {
        int sum = 0;
        for (int i = 0; i < ELEM_SIZE; i++) {
            sum += ((IntBox)MH_box_int.invokeExact(base, i)).intValue();
        }
        return sum;
    }

    @Benchmark
    public int segment_loop() throws Throwable {
        int sum = 0;
        for (int i = 0; i < ELEM_SIZE; i++) {
            sum += (int)VH_addr_int.get(segment, (long)i);
        }
        return sum;
    }

    @Benchmark
    public int segment_box_loop() throws Throwable {
        int sum = 0;
        for (int i = 0; i < ELEM_SIZE; i++) {
            sum += ((IntBox)VH_addr_box_int.get(segment, (long)i)).intValue();
        }
        return sum;
    }
}
