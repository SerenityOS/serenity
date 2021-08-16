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

/*
 * @test
 * @run testng TestReshape
 */

import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemoryLayouts;
import jdk.incubator.foreign.SequenceLayout;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.LongStream;

import org.testng.annotations.*;
import static org.testng.Assert.*;

public class TestReshape {

    @Test(dataProvider = "shapes")
    public void testReshape(MemoryLayout layout, long[] expectedShape) {
        long flattenedSize = LongStream.of(expectedShape).reduce(1L, Math::multiplyExact);
        SequenceLayout seq_flattened = MemoryLayout.sequenceLayout(flattenedSize, layout);
        assertDimensions(seq_flattened, flattenedSize);
        for (long[] shape : new Shape(expectedShape)) {
            SequenceLayout seq_shaped = seq_flattened.reshape(shape);
            assertDimensions(seq_shaped, expectedShape);
            assertEquals(seq_shaped.flatten(), seq_flattened);
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testInvalidReshape() {
        SequenceLayout seq = MemoryLayout.sequenceLayout(4, MemoryLayouts.JAVA_INT);
        seq.reshape(3, 2);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testBadReshapeInference() {
        SequenceLayout seq = MemoryLayout.sequenceLayout(4, MemoryLayouts.JAVA_INT);
        seq.reshape(-1, -1);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testBadReshapeParameterZero() {
        SequenceLayout seq = MemoryLayout.sequenceLayout(4, MemoryLayouts.JAVA_INT);
        seq.reshape(0, 4);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testBadReshapeParameterNegative() {
        SequenceLayout seq = MemoryLayout.sequenceLayout(4, MemoryLayouts.JAVA_INT);
        seq.reshape(-2, 2);
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void testReshapeOnUnboundSequence() {
        SequenceLayout seq = MemoryLayout.sequenceLayout(MemoryLayouts.JAVA_INT);
        seq.reshape(3, 2);
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void testFlattenOnUnboundSequence() {
        SequenceLayout seq = MemoryLayout.sequenceLayout(MemoryLayouts.JAVA_INT);
        seq.flatten();
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void testFlattenOnUnboundNestedSequence() {
        SequenceLayout seq = MemoryLayout.sequenceLayout(4, MemoryLayout.sequenceLayout(MemoryLayouts.JAVA_INT));
        seq.flatten();
    }

    static void assertDimensions(SequenceLayout layout, long... dims) {
        SequenceLayout prev = null;
        for (int i = 0 ; i < dims.length ; i++) {
            if (prev != null) {
                layout = (SequenceLayout)prev.elementLayout();
            }
            assertEquals(layout.elementCount().getAsLong(), dims[i]);
            prev = layout;
        }
    }

    static class Shape implements Iterable<long[]> {
        long[] shape;

        Shape(long... shape) {
            this.shape = shape;
        }

        public Iterator<long[]> iterator() {
            List<long[]> shapes = new ArrayList<>();
            shapes.add(shape);
            for (int i = 0 ; i < shape.length ; i++) {
                long[] inferredShape = shape.clone();
                inferredShape[i] = -1;
                shapes.add(inferredShape);
            }
            return shapes.iterator();
        }
    }

    static MemoryLayout POINT = MemoryLayout.structLayout(
            MemoryLayouts.JAVA_INT,
            MemoryLayouts.JAVA_INT
    );

    @DataProvider(name = "shapes")
    Object[][] shapes() {
        return new Object[][] {
                { MemoryLayouts.JAVA_BYTE, new long[] { 256 } },
                { MemoryLayouts.JAVA_BYTE, new long[] { 16, 16 } },
                { MemoryLayouts.JAVA_BYTE, new long[] { 4, 4, 4, 4 } },
                { MemoryLayouts.JAVA_BYTE, new long[] { 2, 8, 16 } },
                { MemoryLayouts.JAVA_BYTE, new long[] { 16, 8, 2 } },
                { MemoryLayouts.JAVA_BYTE, new long[] { 8, 16, 2 } },

                { MemoryLayouts.JAVA_SHORT, new long[] { 256 } },
                { MemoryLayouts.JAVA_SHORT, new long[] { 16, 16 } },
                { MemoryLayouts.JAVA_SHORT, new long[] { 4, 4, 4, 4 } },
                { MemoryLayouts.JAVA_SHORT, new long[] { 2, 8, 16 } },
                { MemoryLayouts.JAVA_SHORT, new long[] { 16, 8, 2 } },
                { MemoryLayouts.JAVA_SHORT, new long[] { 8, 16, 2 } },

                { MemoryLayouts.JAVA_CHAR, new long[] { 256 } },
                { MemoryLayouts.JAVA_CHAR, new long[] { 16, 16 } },
                { MemoryLayouts.JAVA_CHAR, new long[] { 4, 4, 4, 4 } },
                { MemoryLayouts.JAVA_CHAR, new long[] { 2, 8, 16 } },
                { MemoryLayouts.JAVA_CHAR, new long[] { 16, 8, 2 } },
                { MemoryLayouts.JAVA_CHAR, new long[] { 8, 16, 2 } },

                { MemoryLayouts.JAVA_INT, new long[] { 256 } },
                { MemoryLayouts.JAVA_INT, new long[] { 16, 16 } },
                { MemoryLayouts.JAVA_INT, new long[] { 4, 4, 4, 4 } },
                { MemoryLayouts.JAVA_INT, new long[] { 2, 8, 16 } },
                { MemoryLayouts.JAVA_INT, new long[] { 16, 8, 2 } },
                { MemoryLayouts.JAVA_INT, new long[] { 8, 16, 2 } },

                { MemoryLayouts.JAVA_LONG, new long[] { 256 } },
                { MemoryLayouts.JAVA_LONG, new long[] { 16, 16 } },
                { MemoryLayouts.JAVA_LONG, new long[] { 4, 4, 4, 4 } },
                { MemoryLayouts.JAVA_LONG, new long[] { 2, 8, 16 } },
                { MemoryLayouts.JAVA_LONG, new long[] { 16, 8, 2 } },
                { MemoryLayouts.JAVA_LONG, new long[] { 8, 16, 2 } },

                { MemoryLayouts.JAVA_FLOAT, new long[] { 256 } },
                { MemoryLayouts.JAVA_FLOAT, new long[] { 16, 16 } },
                { MemoryLayouts.JAVA_FLOAT, new long[] { 4, 4, 4, 4 } },
                { MemoryLayouts.JAVA_FLOAT, new long[] { 2, 8, 16 } },
                { MemoryLayouts.JAVA_FLOAT, new long[] { 16, 8, 2 } },
                { MemoryLayouts.JAVA_FLOAT, new long[] { 8, 16, 2 } },

                { MemoryLayouts.JAVA_DOUBLE, new long[] { 256 } },
                { MemoryLayouts.JAVA_DOUBLE, new long[] { 16, 16 } },
                { MemoryLayouts.JAVA_DOUBLE, new long[] { 4, 4, 4, 4 } },
                { MemoryLayouts.JAVA_DOUBLE, new long[] { 2, 8, 16 } },
                { MemoryLayouts.JAVA_DOUBLE, new long[] { 16, 8, 2 } },
                { MemoryLayouts.JAVA_DOUBLE, new long[] { 8, 16, 2 } },

                { POINT, new long[] { 256 } },
                { POINT, new long[] { 16, 16 } },
                { POINT, new long[] { 4, 4, 4, 4 } },
                { POINT, new long[] { 2, 8, 16 } },
                { POINT, new long[] { 16, 8, 2 } },
                { POINT, new long[] { 8, 16, 2 } },
        };
    }
}
