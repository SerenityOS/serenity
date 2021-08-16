/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

import java.io.Serializable;
import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.runtime.SwitchBootstraps;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

/**
 * @test
 * @compile --enable-preview -source ${jdk.version} SwitchBootstrapsTest.java
 * @run testng/othervm --enable-preview SwitchBootstrapsTest
 */
@Test
public class SwitchBootstrapsTest {

    public static final MethodHandle BSM_TYPE_SWITCH;
    public static final MethodHandle BSM_ENUM_SWITCH;

    static {
        try {
            BSM_TYPE_SWITCH = MethodHandles.lookup().findStatic(SwitchBootstraps.class, "typeSwitch",
                                                                MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, Object[].class));
            BSM_ENUM_SWITCH = MethodHandles.lookup().findStatic(SwitchBootstraps.class, "enumSwitch",
                                                                MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, Object[].class));
        }
        catch (ReflectiveOperationException e) {
            throw new AssertionError("Should not happen", e);
        }
    }

    private void testType(Object target, int start, int result, Object... labels) throws Throwable {
        MethodType switchType = MethodType.methodType(int.class, Object.class, int.class);
        MethodHandle indy = ((CallSite) BSM_TYPE_SWITCH.invoke(MethodHandles.lookup(), "", switchType, labels)).dynamicInvoker();
        assertEquals((int) indy.invoke(target, start), result);
        assertEquals(-1, (int) indy.invoke(null, start));
    }

    private void testEnum(Enum<?> target, int start, int result, Object... labels) throws Throwable {
        MethodType switchType = MethodType.methodType(int.class, target.getClass(), int.class);
        MethodHandle indy = ((CallSite) BSM_ENUM_SWITCH.invoke(MethodHandles.lookup(), "", switchType, labels)).dynamicInvoker();
        assertEquals((int) indy.invoke(target, start), result);
        assertEquals(-1, (int) indy.invoke(null, start));
    }

    public enum E1 {
        A,
        B;
    }

    public enum E2 {
        C;
    }

    public void testTypes() throws Throwable {
        testType("", 0, 0, String.class, Object.class);
        testType("", 0, 0, Object.class);
        testType("", 0, 1, Integer.class);
        testType("", 0, 1, Integer.class, Serializable.class);
        testType(E1.A, 0, 0, E1.class, Object.class);
        testType(E2.C, 0, 1, E1.class, Object.class);
        testType(new Serializable() { }, 0, 1, Comparable.class, Serializable.class);
        testType("", 0, 0, "", String.class);
        testType("", 1, 1, "", String.class);
        testType("a", 0, 1, "", String.class);
        testType(1, 0, 0, 1, Integer.class);
        testType(2, 0, 1, 1, Integer.class);
        testType(Byte.valueOf((byte) 1), 0, 0, 1, Integer.class);
        testType(Short.valueOf((short) 1), 0, 0, 1, Integer.class);
        testType(Character.valueOf((char) 1), 0, 0, 1, Integer.class);
        testType(Integer.valueOf((int) 1), 0, 0, 1, Integer.class);
        try {
            testType(1, 0, 1, 1.0, Integer.class);
            fail("Didn't get the expected exception.");
        } catch (IllegalArgumentException ex) {
            //OK
        }
        testType("", 0, 0, String.class, String.class, String.class);
        testType("", 1, 1, String.class, String.class, String.class);
        testType("", 2, 2, String.class, String.class, String.class);
    }

    public void testEnums() throws Throwable {
        testEnum(E1.A, 0, 2, "B", "C", "A", E1.class);
        testEnum(E1.B, 0, 0, "B", "C", "A", E1.class);
        testEnum(E1.B, 1, 3, "B", "C", "A", E1.class);
        try {
            testEnum(E1.B, 1, 3, "B", "C", "A", E2.class);
            fail("Didn't get the expected exception.");
        } catch (IllegalArgumentException ex) {
            //OK
        }
        try {
            testEnum(E1.B, 1, 3, "B", "C", "A", String.class);
            fail("Didn't get the expected exception.");
        } catch (IllegalArgumentException ex) {
            //OK
        }
    }

    public void testWrongSwitchTypes() throws Throwable {
        MethodType[] switchTypes = new MethodType[] {
            MethodType.methodType(int.class, Object.class),
            MethodType.methodType(int.class, double.class, int.class),
            MethodType.methodType(int.class, Object.class, Integer.class)
        };
        for (MethodType switchType : switchTypes) {
            try {
                BSM_TYPE_SWITCH.invoke(MethodHandles.lookup(), "", switchType);
                fail("Didn't get the expected exception.");
            } catch (IllegalArgumentException ex) {
                //OK, expected
            }
        }
        MethodType[] enumSwitchTypes = new MethodType[] {
            MethodType.methodType(int.class, Enum.class),
            MethodType.methodType(int.class, Object.class, int.class),
            MethodType.methodType(int.class, double.class, int.class),
            MethodType.methodType(int.class, Enum.class, Integer.class)
        };
        for (MethodType enumSwitchType : enumSwitchTypes) {
            try {
                BSM_ENUM_SWITCH.invoke(MethodHandles.lookup(), "", enumSwitchType);
                fail("Didn't get the expected exception.");
            } catch (IllegalArgumentException ex) {
                //OK, expected
            }
        }
    }

    public void testNullLabels() throws Throwable {
        MethodType switchType = MethodType.methodType(int.class, Object.class, int.class);
        try {
            BSM_TYPE_SWITCH.invoke(MethodHandles.lookup(), "", switchType, (Object[]) null);
            fail("Didn't get the expected exception.");
        } catch (NullPointerException ex) {
            //OK
        }
        try {
            BSM_TYPE_SWITCH.invoke(MethodHandles.lookup(), "", switchType,
                                   new Object[] {1, null, String.class});
            fail("Didn't get the expected exception.");
        } catch (IllegalArgumentException ex) {
            //OK
        }
        MethodType enumSwitchType = MethodType.methodType(int.class, E1.class, int.class);
        try {
            BSM_TYPE_SWITCH.invoke(MethodHandles.lookup(), "", enumSwitchType, (Object[]) null);
            fail("Didn't get the expected exception.");
        } catch (NullPointerException ex) {
            //OK
        }
        try {
            BSM_TYPE_SWITCH.invoke(MethodHandles.lookup(), "", enumSwitchType,
                                   new Object[] {1, null, String.class});
            fail("Didn't get the expected exception.");
        } catch (IllegalArgumentException ex) {
            //OK
        }
    }
}
