/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.invoke.TypeDescriptor;
import java.lang.constant.ClassDesc;

import org.testng.annotations.Test;

import static java.lang.constant.ConstantDescs.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * @test
 * @compile TypeDescriptorTest.java
 * @run testng TypeDescriptorTest
 * @summary unit tests for implementations of java.lang.invoke.TypeDescriptor
 */
@Test
public class TypeDescriptorTest {
    private<F extends TypeDescriptor.OfField<F>> void testArray(F f, boolean isArray, F component, F array) {
        if (isArray) {
            assertTrue(f.isArray());
            assertEquals(f.arrayType(), array);
            assertEquals(f.componentType(), component);
        }
        else {
            assertFalse(f.isArray());
            assertEquals(f.arrayType(), array);
            assertNull(f.componentType());
        }
    }

    public void testClass() {
        testArray(int.class, false, null, int[].class);
        testArray(int[].class, true, int.class, int[][].class);
        testArray(int[][].class, true, int[].class, int[][][].class);
        testArray(String.class, false, null, String[].class);
        testArray(String[].class, true, String.class, String[][].class);
        testArray(String[][].class, true, String[].class, String[][][].class);

        assertTrue(int.class.isPrimitive());
        assertFalse(int[].class.isPrimitive());
        assertFalse(String.class.isPrimitive());
        assertFalse(String[].class.isPrimitive());
    }

    public void testClassDesc() {

        testArray(CD_int, false, null, CD_int.arrayType());
        testArray(CD_int.arrayType(), true, CD_int, CD_int.arrayType(2));
        testArray(CD_int.arrayType(2), true, CD_int.arrayType(), CD_int.arrayType(3));
        testArray(CD_String, false, null, CD_String.arrayType());
        testArray(CD_String.arrayType(), true, CD_String, CD_String.arrayType(2));
        testArray(CD_String.arrayType(2), true, CD_String.arrayType(), CD_String.arrayType(3));

        assertTrue(CD_int.isPrimitive());
        assertFalse(CD_int.arrayType().isPrimitive());
        assertFalse(CD_String.isPrimitive());
        assertFalse(CD_String.arrayType().isPrimitive());
    }

}
