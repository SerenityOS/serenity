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

/*
 * @test
 * @requires ((os.arch == "amd64" | os.arch == "x86_64") & sun.arch.data.model == "64") | os.arch == "aarch64"
 * @run testng/othervm --enable-native-access=ALL-UNNAMED TestIllegalLink
 */

import jdk.incubator.foreign.CLinker;
import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemoryLayouts;
import jdk.incubator.foreign.MemorySegment;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.invoke.MethodType;

import static jdk.incubator.foreign.CLinker.C_INT;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class TestIllegalLink {

    private static final MemoryAddress DUMMY_TARGET = MemoryAddress.ofLong(1);
    private static final CLinker ABI = CLinker.getInstance();

    @Test(dataProvider = "types")
    public void testTypeMismatch(MethodType mt, FunctionDescriptor desc, String expectedExceptionMessage) {
        try {
            ABI.downcallHandle(DUMMY_TARGET, mt, desc);
            fail("Expected IllegalArgumentException was not thrown");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains(expectedExceptionMessage));
        }
    }

    @DataProvider
    public static Object[][] types() {
        return new Object[][]{
            {
                MethodType.methodType(void.class),
                FunctionDescriptor.of(C_INT),
                "Return type mismatch"
            },
            {
                MethodType.methodType(void.class),
                FunctionDescriptor.ofVoid(C_INT),
                "Arity mismatch"
            },
            {
                MethodType.methodType(void.class, int.class),
                FunctionDescriptor.ofVoid(MemoryLayout.paddingLayout(32)),
                "Expected a ValueLayout"
            },
            {
                MethodType.methodType(void.class, boolean.class),
                FunctionDescriptor.ofVoid(MemoryLayouts.BITS_8_LE),
                "Unsupported carrier"
            },
            {
                MethodType.methodType(void.class, int.class),
                FunctionDescriptor.ofVoid(MemoryLayouts.BITS_64_LE),
                "Carrier size mismatch"
            },
            {
                MethodType.methodType(void.class, MemoryAddress.class),
                FunctionDescriptor.ofVoid(MemoryLayout.paddingLayout(64)),
                "Expected a ValueLayout"
            },
            {
                MethodType.methodType(void.class, MemoryAddress.class),
                FunctionDescriptor.ofVoid(MemoryLayouts.BITS_16_LE),
                "Address size mismatch"
            },
            {
                MethodType.methodType(void.class, MemorySegment.class),
                FunctionDescriptor.ofVoid(MemoryLayouts.BITS_64_LE),
                "Expected a GroupLayout"
            },
            {
                MethodType.methodType(void.class, String.class),
                FunctionDescriptor.ofVoid(MemoryLayouts.BITS_64_LE),
                "Unsupported carrier"
            },
        };
    }

}
