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
 * @run testng/othervm -ea -esa test.java.lang.invoke.ArrayLengthTest
 */
package test.java.lang.invoke;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

import static org.testng.AssertJUnit.*;

import org.testng.annotations.*;

public class ArrayLengthTest {

    @DataProvider
    Object[][] arrayClasses() {
        return new Object[][] {
                {int[].class},
                {long[].class},
                {float[].class},
                {double[].class},
                {boolean[].class},
                {byte[].class},
                {short[].class},
                {char[].class},
                {Object[].class},
                {StringBuffer[].class}
        };
    }

    @Test(dataProvider = "arrayClasses")
    public void testArrayLength(Class<?> arrayClass) throws Throwable {
        MethodHandle arrayLength = MethodHandles.arrayLength(arrayClass);
        assertEquals(int.class, arrayLength.type().returnType());
        assertEquals(arrayClass, arrayLength.type().parameterType(0));
        Object array = MethodHandles.arrayConstructor(arrayClass).invoke(10);
        assertEquals(10, arrayLength.invoke(array));
    }

    @Test(dataProvider = "arrayClasses", expectedExceptions = NullPointerException.class)
    public void testArrayLengthInvokeNPE(Class<?> arrayClass) throws Throwable {
        MethodHandle arrayLength = MethodHandles.arrayLength(arrayClass);
        arrayLength.invoke(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testArrayLengthNoArray() {
        MethodHandles.arrayLength(String.class);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testArrayLengthNPE() {
        MethodHandles.arrayLength(null);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testNullReference() throws Throwable {
        MethodHandle arrayLength = MethodHandles.arrayLength(String[].class);
        int len = (int)arrayLength.invokeExact((String[])null);
    }
}
