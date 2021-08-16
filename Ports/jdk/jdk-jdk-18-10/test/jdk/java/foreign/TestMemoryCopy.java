/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @run testng TestMemoryCopy
 */

import jdk.incubator.foreign.MemoryLayouts;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

import static org.testng.Assert.*;

public class TestMemoryCopy {

    final static VarHandle BYTE_HANDLE = MemoryLayouts.JAVA_BYTE.varHandle(byte.class);

    @Test(dataProvider = "slices")
    public void testCopy(SegmentSlice s1, SegmentSlice s2) {
        int size = Math.min(s1.size(), s2.size());
        //prepare source and target segments
        for (int i = 0 ; i < size ; i++) {
            BYTE_HANDLE.set(s2.segment.asSlice(i), (byte)0);
        }
        for (int i = 0 ; i < size ; i++) {
            BYTE_HANDLE.set(s1.segment.asSlice(i), (byte) i);
        }
        //perform copy
        s2.segment.copyFrom(s1.segment.asSlice(0, size));
        //check that copy actually worked
        for (int i = 0 ; i < size ; i++) {
            assertEquals((byte)i, BYTE_HANDLE.get(s2.segment.asSlice(i)));
        }
    }

    static class SegmentSlice {

        enum Kind {
            NATIVE(i -> MemorySegment.allocateNative(i, ResourceScope.newImplicitScope())),
            ARRAY(i -> MemorySegment.ofArray(new byte[i]));

            final IntFunction<MemorySegment> segmentFactory;

            Kind(IntFunction<MemorySegment> segmentFactory) {
                this.segmentFactory = segmentFactory;
            }

            MemorySegment makeSegment(int elems) {
                return segmentFactory.apply(elems);
            }
        }

        final Kind kind;
        final int first;
        final int last;
        final MemorySegment segment;

        public SegmentSlice(Kind kind, int first, int last, MemorySegment segment) {
            this.kind = kind;
            this.first = first;
            this.last = last;
            this.segment = segment;
        }

        int size() {
            return last - first + 1;
        }
    }

    @DataProvider(name = "slices")
    static Object[][] slices() {
        int[] sizes = { 16, 8, 4, 2, 1 };
        List<SegmentSlice> slices = new ArrayList<>();
        for (SegmentSlice.Kind kind : SegmentSlice.Kind.values()) {
            MemorySegment segment = kind.makeSegment(16);
            //compute all slices
            for (int size : sizes) {
                for (int index = 0 ; index < 16 ; index += size) {
                    MemorySegment slice = segment.asSlice(index, size);
                    slices.add(new SegmentSlice(kind, index, index + size - 1, slice));
                }
            }
        }
        Object[][] sliceArray = new Object[slices.size() * slices.size()][];
        for (int i = 0 ; i < slices.size() ; i++) {
            for (int j = 0 ; j < slices.size() ; j++) {
                sliceArray[i * slices.size() + j] = new Object[] { slices.get(i), slices.get(j) };
            }
        }
        return sliceArray;
    }
}
