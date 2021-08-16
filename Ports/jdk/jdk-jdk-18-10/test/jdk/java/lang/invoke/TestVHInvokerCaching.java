/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 8265079
 * @run testng/othervm -Xverify:all TestVHInvokerCaching
 */

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static java.lang.invoke.MethodHandles.lookup;
import static org.testng.Assert.assertSame;

public class TestVHInvokerCaching {

    @Test(dataProvider = "testHandles")
    public static void testVHIInvokerCaching(VarHandle testHandle) throws Throwable {
        for (VarHandle.AccessMode mode : VarHandle.AccessMode.values()) {
            MethodHandle handle1 = MethodHandles.varHandleInvoker(mode, testHandle.accessModeType(mode));
            MethodHandle handle2 = MethodHandles.varHandleInvoker(mode, testHandle.accessModeType(mode));
            assertSame(handle1, handle2);
        }

        for (VarHandle.AccessMode mode : VarHandle.AccessMode.values()) {
            MethodHandle handle1 = MethodHandles.varHandleExactInvoker(mode, testHandle.accessModeType(mode));
            MethodHandle handle2 = MethodHandles.varHandleExactInvoker(mode, testHandle.accessModeType(mode));
            assertSame(handle1, handle2);
        }
    }

    @DataProvider
    public static Object[][] testHandles() throws NoSuchFieldException, IllegalAccessException {
        List<VarHandle> testHandles = new ArrayList<>();

        class Holder {
            byte f_byte;
            short f_short;
            char f_char;
            int f_int;
            long f_long;
            float f_float;
            double f_double;
            Object f_Object;
            String f_String;
        }

        MethodHandles.Lookup lookup = lookup();

        for (Field field : Holder.class.getFields()) {
            String fieldName = field.getName();
            Class<?> fieldType = field.getType();

            testHandles.add(MethodHandles.arrayElementVarHandle(fieldType.arrayType()));
            testHandles.add(lookup.findVarHandle(Holder.class, fieldName, fieldType));
        }

        return testHandles.stream().map(vh -> new Object[]{ vh }).toArray(Object[][]::new);
    }
}

