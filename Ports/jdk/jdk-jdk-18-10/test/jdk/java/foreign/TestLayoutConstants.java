/*
 *  Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @run testng TestLayoutConstants
 */

import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.MemoryLayouts;
import jdk.incubator.foreign.MemoryLayout;

import java.lang.invoke.MethodHandles;

import org.testng.annotations.*;
import static org.testng.Assert.*;

public class TestLayoutConstants {

    @Test(dataProvider = "layouts")
    public void testDescribeResolve(MemoryLayout expected) {
        try {
            MemoryLayout actual = expected.describeConstable().get()
                    .resolveConstantDesc(MethodHandles.lookup());
            assertEquals(actual, expected);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }

    @Test(dataProvider = "functions")
    public void testDescribeResolveFunction(MemoryLayout layout, boolean isVoid) {
        FunctionDescriptor expected = isVoid ?
                FunctionDescriptor.ofVoid(layout) :
                FunctionDescriptor.of(layout, layout);
        try {
            FunctionDescriptor actual = expected.describeConstable().get()
                    .resolveConstantDesc(MethodHandles.lookup());
            assertEquals(actual, expected);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }

    @DataProvider(name = "layouts")
    public Object[][] createLayouts() {
        return new Object[][] {
                //padding
                { MemoryLayouts.PAD_32 },
                { MemoryLayout.sequenceLayout(MemoryLayouts.PAD_32) },
                { MemoryLayout.sequenceLayout(5, MemoryLayouts.PAD_32) },
                { MemoryLayout.structLayout(MemoryLayouts.PAD_32, MemoryLayouts.PAD_32) },
                { MemoryLayout.unionLayout(MemoryLayouts.PAD_32, MemoryLayouts.PAD_32) },
                //values, big endian
                { MemoryLayouts.BITS_32_BE },
                { MemoryLayout.structLayout(
                        MemoryLayouts.BITS_32_BE,
                        MemoryLayouts.BITS_32_BE) },
                { MemoryLayout.unionLayout(
                        MemoryLayouts.BITS_32_BE,
                        MemoryLayouts.BITS_32_BE) },
                //values, little endian
                { MemoryLayouts.BITS_32_LE },
                { MemoryLayout.structLayout(
                        MemoryLayouts.BITS_32_LE,
                        MemoryLayouts.BITS_32_LE) },
                { MemoryLayout.unionLayout(
                        MemoryLayouts.BITS_32_LE,
                        MemoryLayouts.BITS_32_LE) },
                //deeply nested
                { MemoryLayout.structLayout(
                        MemoryLayouts.PAD_16,
                        MemoryLayout.structLayout(
                                MemoryLayouts.PAD_8,
                                MemoryLayouts.BITS_32_BE)) },
                { MemoryLayout.unionLayout(
                        MemoryLayouts.PAD_16,
                        MemoryLayout.structLayout(
                                MemoryLayouts.PAD_8,
                                MemoryLayouts.BITS_32_BE)) },
                { MemoryLayout.sequenceLayout(
                        MemoryLayout.structLayout(
                                MemoryLayouts.PAD_8,
                                MemoryLayouts.BITS_32_BE)) },
                { MemoryLayout.sequenceLayout(5,
                        MemoryLayout.structLayout(
                                MemoryLayouts.PAD_8,
                                MemoryLayouts.BITS_32_BE)) },
                { MemoryLayouts.BITS_32_LE.withName("myInt") },
                { MemoryLayouts.BITS_32_LE.withBitAlignment(8) },
                { MemoryLayouts.BITS_32_LE.withAttribute("xyz", "abc") },
        };
    }

    @DataProvider(name = "functions")
    public Object[][] createFunctions() {
        Object[][] layouts = createLayouts();
        Object[][] functions = new Object[layouts.length * 2][];
        boolean[] values = new boolean[] { true, false };
        for (int i = 0 ; i < layouts.length ; i++) {
            for (boolean isVoid : values) {
                int offset = 0;
                if (isVoid) {
                    offset += 1;
                }
                functions[i * 2 + offset] = new Object[] { layouts[i][0], isVoid };
            }
        }
        return functions;
    }
}
