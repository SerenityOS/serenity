/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 8155106
 * @run testng/othervm -ea -esa test.java.lang.invoke.ArrayConstructorTest
 */
package test.java.lang.invoke;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

import static java.lang.invoke.MethodType.methodType;

import static org.testng.AssertJUnit.*;

import org.testng.annotations.*;


public class ArrayConstructorTest {

    static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    @Test
    public static void testFindConstructorArray() {
        boolean caught = false;
        try {
            MethodHandle h = LOOKUP.findConstructor(Object[].class, methodType(void.class));
        } catch (NoSuchMethodException nsme) {
            assertEquals("no constructor for array class: [Ljava.lang.Object;", nsme.getMessage());
            caught = true;
        } catch (Exception e) {
            throw new AssertionError("unexpected exception: " + e);
        }
        assertTrue(caught);
    }

    @DataProvider
    static Object[][] arrayConstructorNegative() {
        return new Object[][]{
                {String.class, IllegalArgumentException.class, "not an array class: java.lang.String"},
                {null, NullPointerException.class, null}
        };
    }

    @Test(dataProvider = "arrayConstructorNegative")
    public static void testArrayConstructorNegative(Class<?> clazz, Class<?> exceptionClass, String message) {
        boolean caught = false;
        try {
            MethodHandle h = MethodHandles.arrayConstructor(clazz);
        } catch (Exception e) {
            assertEquals(exceptionClass, e.getClass());
            if (message != null) {
                assertEquals(message, e.getMessage());
            }
            caught = true;
        }
        assertTrue(caught);
    }

    @Test
    public static void testArrayConstructor() throws Throwable {
        MethodHandle h = MethodHandles.arrayConstructor(String[].class);
        assertEquals(methodType(String[].class, int.class), h.type());
        String[] a = (String[]) h.invoke(17);
        assertEquals(17, a.length);
    }

    @Test(expectedExceptions = {NegativeArraySizeException.class})
    public static void testArrayConstructorNegativeIndex() throws Throwable {
        MethodHandle h = MethodHandles.arrayConstructor(String[].class);
        assertEquals(methodType(String[].class, int.class), h.type());
        h.invoke(-1); // throws exception
    }

}
