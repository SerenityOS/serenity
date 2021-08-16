/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2018 SAP SE. All rights reserved.
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

/**
 * @test
 * @summary Test that NegativeArraySizeException reports the wrong size.
 * @library /test/lib
 * @compile NegativeArraySizeExceptionTest.java
 * @run main NegativeArraySizeExceptionTest
 */

import java.lang.reflect.Array;

import jdk.test.lib.Asserts;

public class NegativeArraySizeExceptionTest {

    private static void fail () throws Exception {
        throw new RuntimeException("Array allocation with negative size expected to fail!");
    }

    public static void main(String[] args) throws Exception {
        int minusOne = -1;
        Object r = null;

        // Tests for exception thrown in arrayKlass.cp, ArrayKlass::allocate_arrayArray().

        try {
            r = new byte[minusOne][];
            fail();
        } catch (NegativeArraySizeException e) {
            Asserts.assertEQ("-1", e.getMessage());
        }

        try {
            r = new Object[Integer.MIN_VALUE][];
            fail();
        } catch (NegativeArraySizeException e) {
            Asserts.assertEQ("-2147483648", e.getMessage());
        }

        // Tests for exception thrown in typeArrayKlass.cpp, TypeArrayKlass::allocate_common().

        try {
            r = new byte[minusOne];
            fail();
        } catch (NegativeArraySizeException e) {
            Asserts.assertEQ("-1", e.getMessage());
        }

        try {
            r = new byte[Integer.MIN_VALUE];
            fail();
        } catch (NegativeArraySizeException e) {
            Asserts.assertEQ("-2147483648", e.getMessage());
        }

        // Tests for exception thrown in instanceKlass.cpp, InstanceKlass::allocate_objArray().

        try {
            r = new Object[minusOne];
            fail();
        } catch (NegativeArraySizeException e) {
            Asserts.assertEQ("-1", e.getMessage());
        }

        try {
            r = new Object[Integer.MIN_VALUE];
            fail();
        } catch (NegativeArraySizeException e) {
            Asserts.assertEQ("-2147483648", e.getMessage());
        }

        // Tests for exception thrown in typeArrayKlass.cpp, TypeArrayKlass::allocate_common().
        // Innermost primitive array of multidimensional array has wrong size.

        try {
            r = new byte[3][minusOne];
            fail();
        } catch (NegativeArraySizeException e) {
            Asserts.assertEQ("-1", e.getMessage());
        }

        try {
            r = new byte[3][Integer.MIN_VALUE];
            fail();
        } catch (NegativeArraySizeException e) {
            Asserts.assertEQ("-2147483648", e.getMessage());
        }

        // Tests for exception thrown in objArrayKlass.cpp, ObjArrayKlass::allocate().
        // Innermost object array of multidimensional array has wrong size.

        try {
            r = new Object[3][minusOne];
            fail();
        } catch (NegativeArraySizeException e) {
            Asserts.assertEQ("-1", e.getMessage());
        }

        try {
            r = new Object[3][Integer.MIN_VALUE];
            fail();
        } catch (NegativeArraySizeException e) {
            Asserts.assertEQ("-2147483648", e.getMessage());
        }

        // Tests for exception thrown in
        // Innermost primitive array of multidimensional array has wrong size.
        // Outer array has size 0.

        try {
            r = new byte[0][minusOne];
            fail();
        } catch (NegativeArraySizeException e) {
            Asserts.assertEQ("-1", e.getMessage());
        }

        try {
            r = new byte[0][Integer.MIN_VALUE];
            fail();
        } catch (NegativeArraySizeException e) {
            Asserts.assertEQ("-2147483648", e.getMessage());
        }

        // Tests for exception thrown in
        // Innermost object array of multidimensional array has wrong size.
        // Outer array has size 0.

        try {
            r = new Object[0][minusOne];
            fail();
        } catch (NegativeArraySizeException e) {
            Asserts.assertEQ("-1", e.getMessage());
        }

        try {
            r = new Object[0][Integer.MIN_VALUE];
            fail();
        } catch (NegativeArraySizeException e) {
            Asserts.assertEQ("-2147483648", e.getMessage());
        }

        // Tests for exception thrown in objArrayKlass.cpp, ObjArrayKlass::allocate().
        // Outer array of multidimensional array has wrong size, inner array
        // has primitive type.

        try {
            r = new byte[minusOne][3];
            fail();
        } catch (NegativeArraySizeException e) {
            Asserts.assertEQ("-1", e.getMessage());
        }

        try {
            r = new byte[Integer.MIN_VALUE][3];
            fail();
        } catch (NegativeArraySizeException e) {
            Asserts.assertEQ("-2147483648", e.getMessage());
        }

        // Tests for exception thrown in objArrayKlass.cpp, ObjArrayKlass::allocate().
        // Outer array of multidimensional array has wrong size, inner array
        // has object type.

        try {
            r = new Object[minusOne][3];
            fail();
        } catch (NegativeArraySizeException e) {
            Asserts.assertEQ("-1", e.getMessage());
        }

        try {
            r = new Object[Integer.MIN_VALUE][3];
            fail();
        } catch (NegativeArraySizeException e) {
            Asserts.assertEQ("-2147483648", e.getMessage());
        }

        // Tests for exception thrown in reflection.cpp, Reflection::reflect_new_array().

        try {
            Array.newInstance(byte.class, minusOne);
            fail();
        } catch (NegativeArraySizeException e) {
            Asserts.assertEQ("-1", e.getMessage());
        }

        try {
            Array.newInstance(byte.class, Integer.MIN_VALUE);
            fail();
        } catch (NegativeArraySizeException e) {
            Asserts.assertEQ("-2147483648", e.getMessage());
        }

        try {
            Array.newInstance(NegativeArraySizeException.class, minusOne);
            fail();
        } catch (NegativeArraySizeException e) {
            Asserts.assertEQ("-1", e.getMessage());
        }

        try {
            Array.newInstance(NegativeArraySizeException.class, Integer.MIN_VALUE);
            fail();
        } catch (NegativeArraySizeException e) {
            Asserts.assertEQ("-2147483648", e.getMessage());
        }


        // Tests for exception thrown in reflection.cpp, Reflection::reflect_new_multi_array().

        try {
            Array.newInstance(byte.class, new int[] {3, minusOne});
            fail();
        } catch (NegativeArraySizeException e) {
            Asserts.assertEQ("-1", e.getMessage());
        }

        try {
            Array.newInstance(byte.class, new int[] {3, Integer.MIN_VALUE});
            fail();
        } catch (NegativeArraySizeException e) {
            Asserts.assertEQ("-2147483648", e.getMessage());
        }

        try {
            Array.newInstance(byte.class, new int[] {0, minusOne});
            fail();
        } catch (NegativeArraySizeException e) {
            Asserts.assertEQ("-1", e.getMessage());
        }

        try {
            Array.newInstance(byte.class, new int[] {0, Integer.MIN_VALUE});
            fail();
        } catch (NegativeArraySizeException e) {
            Asserts.assertEQ("-2147483648", e.getMessage());
        }

        try {
            Array.newInstance(NegativeArraySizeException.class, new int[] {3, minusOne});
            fail();
        } catch (NegativeArraySizeException e) {
            Asserts.assertEQ("-1", e.getMessage());
        }

        try {
            Array.newInstance(NegativeArraySizeException.class, new int[] {3, Integer.MIN_VALUE});
            fail();
        } catch (NegativeArraySizeException e) {
            Asserts.assertEQ("-2147483648", e.getMessage());
        }

        try {
            Array.newInstance(byte.class, new int[] {minusOne, 3});
            fail();
        } catch (NegativeArraySizeException e) {
            Asserts.assertEQ("-1", e.getMessage());
        }

        try {
            Array.newInstance(byte.class, new int[] {Integer.MIN_VALUE, 3});
            fail();
        } catch (NegativeArraySizeException e) {
            Asserts.assertEQ("-2147483648", e.getMessage());
        }

        try {
            Array.newInstance(NegativeArraySizeException.class, new int[] {minusOne, 3});
            fail();
        } catch (NegativeArraySizeException e) {
            Asserts.assertEQ("-1", e.getMessage());
        }

        try {
            Array.newInstance(NegativeArraySizeException.class, new int[] {Integer.MIN_VALUE, 3});
            fail();
        } catch (NegativeArraySizeException e) {
            Asserts.assertEQ("-2147483648", e.getMessage());
        }

        Asserts.assertEQ(r, null, "Expected all tries to allocate negative array to fail.");
    }
}
