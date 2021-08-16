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
 *  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 */

/*
 * @test
 * @run testng TestMismatch
 */

import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntFunction;

import jdk.incubator.foreign.MemoryLayouts;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static java.lang.System.out;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;

public class TestMismatch {

    final static VarHandle BYTE_HANDLE = MemoryLayouts.JAVA_BYTE.varHandle(byte.class);

    // stores a increasing sequence of values into the memory of the given segment
    static MemorySegment initializeSegment(MemorySegment segment) {
        for (int i = 0 ; i < segment.byteSize() ; i++) {
            BYTE_HANDLE.set(segment.asSlice(i), (byte)i);
        }
        return segment;
    }

    @Test(dataProvider = "slices")
    public void testSameValues(MemorySegment ss1, MemorySegment ss2) {
        out.format("testSameValues s1:%s, s2:%s\n", ss1, ss2);
        MemorySegment s1 = initializeSegment(ss1);
        MemorySegment s2 = initializeSegment(ss2);

        if (s1.byteSize() == s2.byteSize()) {
            assertEquals(s1.mismatch(s2), -1);  // identical
            assertEquals(s2.mismatch(s1), -1);
        } else if (s1.byteSize() > s2.byteSize()) {
            assertEquals(s1.mismatch(s2), s2.byteSize());  // proper prefix
            assertEquals(s2.mismatch(s1), s2.byteSize());
        } else {
            assert s1.byteSize() < s2.byteSize();
            assertEquals(s1.mismatch(s2), s1.byteSize());  // proper prefix
            assertEquals(s2.mismatch(s1), s1.byteSize());
        }
    }

    @Test(dataProvider = "slices")
    public void testDifferentValues(MemorySegment s1, MemorySegment s2) {
        out.format("testDifferentValues s1:%s, s2:%s\n", s1, s2);
        s1 = initializeSegment(s1);
        s2 = initializeSegment(s2);

        for (long i = s2.byteSize() -1 ; i >= 0; i--) {
            long expectedMismatchOffset = i;
            BYTE_HANDLE.set(s2.asSlice(i), (byte) 0xFF);

            if (s1.byteSize() == s2.byteSize()) {
                assertEquals(s1.mismatch(s2), expectedMismatchOffset);
                assertEquals(s2.mismatch(s1), expectedMismatchOffset);
            } else if (s1.byteSize() > s2.byteSize()) {
                assertEquals(s1.mismatch(s2), expectedMismatchOffset);
                assertEquals(s2.mismatch(s1), expectedMismatchOffset);
            } else {
                assert s1.byteSize() < s2.byteSize();
                var off = Math.min(s1.byteSize(), expectedMismatchOffset);
                assertEquals(s1.mismatch(s2), off);  // proper prefix
                assertEquals(s2.mismatch(s1), off);
            }
        }
    }

    @Test
    public void testEmpty() {
        var s1 = MemorySegment.ofArray(new byte[0]);
        assertEquals(s1.mismatch(s1), -1);
        try (ResourceScope scope = ResourceScope.newConfinedScope()) {
            var nativeSegment = MemorySegment.allocateNative(4, 4, scope);
            var s2 = nativeSegment.asSlice(0, 0);
            assertEquals(s1.mismatch(s2), -1);
            assertEquals(s2.mismatch(s1), -1);
        }
    }

    @Test
    public void testLarge() {
        // skip if not on 64 bits
        if (MemoryLayouts.ADDRESS.byteSize() > 32) {
            try (ResourceScope scope = ResourceScope.newConfinedScope()) {
                var s1 = MemorySegment.allocateNative((long) Integer.MAX_VALUE + 10L, 8, scope);
                var s2 = MemorySegment.allocateNative((long) Integer.MAX_VALUE + 10L, 8, scope);
                assertEquals(s1.mismatch(s1), -1);
                assertEquals(s1.mismatch(s2), -1);
                assertEquals(s2.mismatch(s1), -1);

                testLargeAcrossMaxBoundary(s1, s2);

                testLargeMismatchAcrossMaxBoundary(s1, s2);
            }
        }
    }

    private void testLargeAcrossMaxBoundary(MemorySegment s1, MemorySegment s2) {
        for (long i = s2.byteSize() -1 ; i >= Integer.MAX_VALUE - 10L; i--) {
            var s3 = s1.asSlice(0, i);
            var s4 = s2.asSlice(0, i);
            assertEquals(s3.mismatch(s3), -1);
            assertEquals(s3.mismatch(s4), -1);
            assertEquals(s4.mismatch(s3), -1);
        }
    }

    private void testLargeMismatchAcrossMaxBoundary(MemorySegment s1, MemorySegment s2) {
        for (long i = s2.byteSize() -1 ; i >= Integer.MAX_VALUE - 10L; i--) {
            BYTE_HANDLE.set(s2.asSlice(i), (byte) 0xFF);
            long expectedMismatchOffset = i;
            assertEquals(s1.mismatch(s2), expectedMismatchOffset);
            assertEquals(s2.mismatch(s1), expectedMismatchOffset);
        }
    }

    static final Class<IllegalStateException> ISE = IllegalStateException.class;
    static final Class<UnsupportedOperationException> UOE = UnsupportedOperationException.class;

    @Test
    public void testClosed() {
        MemorySegment s1, s2;
        try (ResourceScope scope = ResourceScope.newConfinedScope()) {
            s1 = MemorySegment.allocateNative(4, 1, scope);
            s2 = MemorySegment.allocateNative(4, 1, scope);
        }
        assertThrows(ISE, () -> s1.mismatch(s1));
        assertThrows(ISE, () -> s1.mismatch(s2));
        assertThrows(ISE, () -> s2.mismatch(s1));
    }

    @Test
    public void testThreadAccess() throws Exception {
        try (ResourceScope scope = ResourceScope.newConfinedScope()) {
            var segment = MemorySegment.allocateNative(4, 1, scope);
            {
                AtomicReference<RuntimeException> exception = new AtomicReference<>();
                Runnable action = () -> {
                    try {
                        MemorySegment.ofArray(new byte[4]).mismatch(segment);
                    } catch (RuntimeException e) {
                        exception.set(e);
                    }
                };
                Thread thread = new Thread(action);
                thread.start();
                thread.join();

                RuntimeException e = exception.get();
                if (!(e instanceof IllegalStateException)) {
                    throw e;
                }
            }
            {
                AtomicReference<RuntimeException> exception = new AtomicReference<>();
                Runnable action = () -> {
                    try {
                        segment.mismatch(MemorySegment.ofArray(new byte[4]));
                    } catch (RuntimeException e) {
                        exception.set(e);
                    }
                };
                Thread thread = new Thread(action);
                thread.start();
                thread.join();

                RuntimeException e = exception.get();
                if (!(e instanceof IllegalStateException)) {
                    throw e;
                }
            }
        }
    }

    enum SegmentKind {
        NATIVE(i -> MemorySegment.allocateNative(i, ResourceScope.newImplicitScope())),
        ARRAY(i -> MemorySegment.ofArray(new byte[i]));

        final IntFunction<MemorySegment> segmentFactory;

        SegmentKind(IntFunction<MemorySegment> segmentFactory) {
            this.segmentFactory = segmentFactory;
        }

        MemorySegment makeSegment(int elems) {
            return segmentFactory.apply(elems);
        }
    }

    @DataProvider(name = "slices")
    static Object[][] slices() {
        int[] sizes = { 16, 8, 1 };
        List<MemorySegment> aSlices = new ArrayList<>();
        List<MemorySegment> bSlices = new ArrayList<>();
        for (List<MemorySegment> slices : List.of(aSlices, bSlices)) {
            for (SegmentKind kind : SegmentKind.values()) {
                MemorySegment segment = kind.makeSegment(16);
                //compute all slices
                for (int size : sizes) {
                    for (int index = 0 ; index < 16 ; index += size) {
                        MemorySegment slice = segment.asSlice(index, size);
                        slices.add(slice);
                    }
                }
            }
        }
        assert aSlices.size() == bSlices.size();
        Object[][] sliceArray = new Object[aSlices.size() * bSlices.size()][];
        for (int i = 0 ; i < aSlices.size() ; i++) {
            for (int j = 0 ; j < bSlices.size() ; j++) {
                sliceArray[i * aSlices.size() + j] = new Object[] { aSlices.get(i), bSlices.get(j) };
            }
        }
        return sliceArray;
    }
}
