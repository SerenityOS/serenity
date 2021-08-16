/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @summary basic tests for MethodHandle.invokeWithArguments
 * @run testng test.java.lang.invoke.InvokeWithArgumentsTest
 */

package test.java.lang.invoke;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.WrongMethodTypeException;

import static java.lang.invoke.MethodType.methodType;

public class InvokeWithArgumentsTest {
    static final MethodHandles.Lookup L = MethodHandles.lookup();

    static Object[] arity(Object o1, Object o2, Object... a) {
        return a;
    }

    @Test
    public void testArity() throws Throwable {
        MethodHandle mh = L.findStatic(L.lookupClass(), "arity",
                                       methodType(Object[].class, Object.class, Object.class, Object[].class));

        try {
            mh.invokeWithArguments("");
            Assert.fail("WrongMethodTypeException expected");
        } catch (WrongMethodTypeException e) {}
    }

    static Object[] passThrough(String... a) {
        return a;
    }

    static Object[] pack(Object o, Object... a) {
        return a;
    }

    @Test
    public void testArrayNoPassThrough() throws Throwable {
        String[] actual = {"A", "B"};

        MethodHandle mh = L.findStatic(L.lookupClass(), "passThrough",
                                       methodType(Object[].class, String[].class));

        // Note: the actual array is not preserved, the elements will be
        // unpacked and then packed into a new array before invoking the method
        String[] expected = (String[]) mh.invokeWithArguments(actual);

        Assert.assertTrue(actual != expected, "Array should not pass through");
        Assert.assertEquals(actual, expected, "Array contents should be equal");
    }

    @Test
    public void testArrayPack() throws Throwable {
        String[] actual = new String[]{"A", "B"};

        MethodHandle mh = L.findStatic(L.lookupClass(), "pack",
                                       methodType(Object[].class, Object.class, Object[].class));

        // Note: since String[] can be cast to Object, the actual String[] array
        // will cast to Object become the single element of a new Object[] array
        Object[] expected = (Object[]) mh.invokeWithArguments("", actual);

        Assert.assertEquals(1, expected.length, "Array should contain just one element");
        Assert.assertTrue(actual == expected[0], "Array should pass through");
    }

    static void intArray(int... a) {
    }

    @Test
    public void testPrimitiveArrayWithNull() throws Throwable {
        MethodHandle mh = L.findStatic(L.lookupClass(), "intArray",
                                       methodType(void.class, int[].class));
        try {
            mh.invokeWithArguments(null, null);
            Assert.fail("NullPointerException expected");
        } catch (NullPointerException e) {}
    }

    @Test
    public void testPrimitiveArrayWithRef() throws Throwable {
        MethodHandle mh = L.findStatic(L.lookupClass(), "intArray",
                                       methodType(void.class, int[].class));
        try {
            mh.invokeWithArguments("A", "B");
            Assert.fail("ClassCastException expected");
        } catch (ClassCastException e) {}
    }


    static void numberArray(Number... a) {
    }

    @Test
    public void testRefArrayWithCast() throws Throwable {
        MethodHandle mh = L.findStatic(L.lookupClass(), "numberArray",
                                       methodType(void.class, Number[].class));
        // All numbers, should not throw
        mh.invokeWithArguments(1, 1.0, 1.0F, 1L);

        try {
            mh.invokeWithArguments("A");
            Assert.fail("ClassCastException expected");
        } catch (ClassCastException e) {}
    }
}
