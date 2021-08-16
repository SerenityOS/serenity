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

import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemoryLayouts;
import jdk.incubator.foreign.MemorySegment;

import java.lang.invoke.VarHandle;

import jdk.incubator.foreign.ResourceScope;
import org.testng.annotations.*;
import static org.testng.Assert.*;

/*
 * @test
 * @run testng/othervm -Xverify:all TestSlices
 */
public class TestSlices {

    static MemoryLayout LAYOUT = MemoryLayout.sequenceLayout(2,
            MemoryLayout.sequenceLayout(5, MemoryLayouts.JAVA_INT));

    static VarHandle VH_ALL = LAYOUT.varHandle(int.class,
            MemoryLayout.PathElement.sequenceElement(), MemoryLayout.PathElement.sequenceElement());

    @Test(dataProvider = "slices")
    public void testSlices(VarHandle handle, int lo, int hi, int[] values) {
        try (ResourceScope scope = ResourceScope.newConfinedScope()) {
            MemorySegment segment = MemorySegment.allocateNative(LAYOUT, scope);
            //init
            for (long i = 0 ; i < 2 ; i++) {
                for (long j = 0 ; j < 5 ; j++) {
                    VH_ALL.set(segment, i, j, (int)j + 1 + ((int)i * 5));
                }
            }

            checkSlice(segment, handle, lo, hi, values);
        }
    }

    static void checkSlice(MemorySegment segment, VarHandle handle, long i_max, long j_max, int... values) {
        int index = 0;
        for (long i = 0 ; i < i_max ; i++) {
            for (long j = 0 ; j < j_max ; j++) {
                int x = (int) handle.get(segment, i, j);
                assertEquals(x, values[index++]);
            }
        }
        assertEquals(index, values.length);
    }

    @DataProvider(name = "slices")
    static Object[][] slices() {
        return new Object[][] {
                // x
                { VH_ALL, 2, 5, new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 } },
                // x[0::2]
                { LAYOUT.varHandle(int.class, MemoryLayout.PathElement.sequenceElement(),
                        MemoryLayout.PathElement.sequenceElement(0, 2)), 2, 3, new int[] { 1, 3, 5, 6, 8, 10 } },
                // x[1::2]
                { LAYOUT.varHandle(int.class, MemoryLayout.PathElement.sequenceElement(),
                        MemoryLayout.PathElement.sequenceElement(1, 2)), 2, 2, new int[] { 2, 4, 7, 9 } },
                // x[4::-2]
                { LAYOUT.varHandle(int.class, MemoryLayout.PathElement.sequenceElement(),
                        MemoryLayout.PathElement.sequenceElement(4, -2)), 2, 3, new int[] { 5, 3, 1, 10, 8, 6 } },
                // x[3::-2]
                { LAYOUT.varHandle(int.class, MemoryLayout.PathElement.sequenceElement(),
                        MemoryLayout.PathElement.sequenceElement(3, -2)), 2, 2, new int[] { 4, 2, 9, 7 } },
        };
    }
}
