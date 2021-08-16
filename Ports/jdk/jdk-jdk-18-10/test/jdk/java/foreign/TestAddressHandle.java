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
 * @run testng/othervm -Djava.lang.invoke.VarHandle.VAR_HANDLE_GUARDS=true -Djava.lang.invoke.VarHandle.VAR_HANDLE_IDENTITY_ADAPT=false -Xverify:all TestAddressHandle
 * @run testng/othervm -Djava.lang.invoke.VarHandle.VAR_HANDLE_GUARDS=true -Djava.lang.invoke.VarHandle.VAR_HANDLE_IDENTITY_ADAPT=true -Xverify:all TestAddressHandle
 * @run testng/othervm -Djava.lang.invoke.VarHandle.VAR_HANDLE_GUARDS=false -Djava.lang.invoke.VarHandle.VAR_HANDLE_IDENTITY_ADAPT=false -Xverify:all TestAddressHandle
 * @run testng/othervm -Djava.lang.invoke.VarHandle.VAR_HANDLE_GUARDS=false -Djava.lang.invoke.VarHandle.VAR_HANDLE_IDENTITY_ADAPT=true -Xverify:all TestAddressHandle
 */

import java.lang.invoke.*;
import java.nio.ByteOrder;
import jdk.incubator.foreign.*;

import org.testng.annotations.*;
import static org.testng.Assert.*;

public class TestAddressHandle {

    static final MethodHandle INT_TO_BOOL;
    static final MethodHandle BOOL_TO_INT;
    static final MethodHandle INT_TO_STRING;
    static final MethodHandle STRING_TO_INT;

    static {
        try {
            INT_TO_BOOL = MethodHandles.lookup().findStatic(TestAddressHandle.class, "intToBool",
                    MethodType.methodType(boolean.class, int.class));
            BOOL_TO_INT = MethodHandles.lookup().findStatic(TestAddressHandle.class, "boolToInt",
                    MethodType.methodType(int.class, boolean.class));
            INT_TO_STRING = MethodHandles.lookup().findStatic(TestAddressHandle.class, "intToString",
                    MethodType.methodType(String.class, int.class));
            STRING_TO_INT = MethodHandles.lookup().findStatic(TestAddressHandle.class, "stringToInt",
                    MethodType.methodType(int.class, String.class));
        } catch (Throwable ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    @Test(dataProvider = "addressHandles")
    public void testAddressHandle(VarHandle addrHandle, int byteSize) {
        VarHandle longHandle = MemoryLayouts.JAVA_LONG.varHandle(long.class);
        try (ResourceScope scope = ResourceScope.newConfinedScope()) {
            MemorySegment segment = MemorySegment.allocateNative(8, scope);
            MemorySegment target = ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN ?
                    segment.asSlice(8 - byteSize) :
                    segment;
            longHandle.set(segment, 42L);
            MemoryAddress address = (MemoryAddress)addrHandle.get(target);
            assertEquals(address.toRawLongValue(), 42L);
            addrHandle.set(target, address.addOffset(1));
            long result = (long)longHandle.get(segment);
            assertEquals(43L, result);
        }
    }

    @Test(dataProvider = "addressHandles")
    public void testNull(VarHandle addrHandle, int byteSize) {
        VarHandle longHandle = MemoryLayouts.JAVA_LONG.varHandle(long.class);
        try (ResourceScope scope = ResourceScope.newConfinedScope()) {
            MemorySegment segment = MemorySegment.allocateNative(8, scope);
            longHandle.set(segment, 0L);
            MemoryAddress address = (MemoryAddress)addrHandle.get(segment);
            assertTrue(address == MemoryAddress.NULL);
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testBadAdaptFloat() {
        VarHandle floatHandle = MemoryLayouts.JAVA_FLOAT.varHandle(float.class);
        MemoryHandles.asAddressVarHandle(floatHandle);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testBadAdaptDouble() {
        VarHandle doubleHandle = MemoryLayouts.JAVA_DOUBLE.varHandle(double.class);
        MemoryHandles.asAddressVarHandle(doubleHandle);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testBadAdaptBoolean() {
        VarHandle intHandle = MemoryLayouts.JAVA_INT.varHandle(int.class);
        VarHandle boolHandle = MemoryHandles.filterValue(intHandle, BOOL_TO_INT, INT_TO_BOOL);
        MemoryHandles.asAddressVarHandle(boolHandle);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testBadAdaptString() {
        VarHandle intHandle = MemoryLayouts.JAVA_INT.varHandle(int.class);
        VarHandle stringHandle = MemoryHandles.filterValue(intHandle, STRING_TO_INT, INT_TO_STRING);
        MemoryHandles.asAddressVarHandle(stringHandle);
    }

    @DataProvider(name = "addressHandles")
    static Object[][] addressHandles() {
        return new Object[][] {
                // long
                { MemoryHandles.asAddressVarHandle(at(MemoryHandles.varHandle(long.class, ByteOrder.nativeOrder()), 0)), 8 },
                { MemoryHandles.asAddressVarHandle(MemoryLayouts.JAVA_LONG.varHandle(long.class)), 8 },

                // int
                { MemoryHandles.asAddressVarHandle(at(MemoryHandles.varHandle(int.class, ByteOrder.nativeOrder()), 0)), 4 },
                { MemoryHandles.asAddressVarHandle(MemoryLayouts.JAVA_INT.varHandle(int.class)), 4 },

                // short
                { MemoryHandles.asAddressVarHandle(at(MemoryHandles.varHandle(short.class, ByteOrder.nativeOrder()), 0)), 2 },
                { MemoryHandles.asAddressVarHandle(MemoryLayouts.JAVA_SHORT.varHandle(short.class)), 2 },

                // char
                { MemoryHandles.asAddressVarHandle(at(MemoryHandles.varHandle(char.class, ByteOrder.nativeOrder()), 0)), 2 },
                { MemoryHandles.asAddressVarHandle(MemoryLayouts.JAVA_CHAR.varHandle(char.class)), 2 },

                // byte
                { MemoryHandles.asAddressVarHandle(at(MemoryHandles.varHandle(byte.class, ByteOrder.nativeOrder()), 0)), 1 },
                { MemoryHandles.asAddressVarHandle(MemoryLayouts.JAVA_BYTE.varHandle(byte.class)), 1 }
        };
    }

    static VarHandle at(VarHandle handle, long offset) {
        return MemoryHandles.insertCoordinates(handle, 1, offset);
    }

    static int boolToInt(boolean value) {
        return value ? 1 : 0;
    }

    static boolean intToBool(int value) {
        return value != 0;
    }

    static int stringToInt(String value) {
        return value.length();
    }

    static String intToString(int value) {
        return String.valueOf(value);
    }
}
