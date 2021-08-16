/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8157246
 * @summary Tests invocation of MethodHandle with invalid leading argument
 * @run testng/othervm test.java.lang.invoke.InvokeMethodHandleWithBadArgument
 */

package test.java.lang.invoke;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;

import static java.lang.invoke.MethodType.methodType;

import static org.testng.AssertJUnit.*;

import org.testng.annotations.*;

/**
 * Tests invocation of MethodHandle with invalid leading argument such as
 * MethodHandle, VarHandle, and array object
 */
public class InvokeMethodHandleWithBadArgument {
    // ---- null array reference ----

    @Test(expectedExceptions = {NullPointerException.class})
    public static void testAsSpreaderPosInvokeWithNull() throws Throwable {
        MethodHandle spreader = MH_spread.asSpreader(1, int[].class, 3);
        spreader.invoke("A", null, "B");
    }

    @Test(expectedExceptions = {NullPointerException.class})
    public static void testAsSpreaderInvokeWithNull() throws Throwable {
        MethodHandle spreader = MH_String_equals.asSpreader(String[].class, 2);
        assert ((boolean) spreader.invokeExact(new String[]{"me", "me"}));
        boolean eq = (boolean) spreader.invokeExact((String[]) null);
    }

    // ---- incorrect array element count ----
    @Test(expectedExceptions = {IllegalArgumentException.class})
    public static void testAsSpreaderPosInvokeWithBadElementCount() throws Throwable {
        MethodHandle spreader = MH_spread.asSpreader(1, int[].class, 3);
        spreader.invoke("A", new int[]{1, 2}, "B");
    }

    @Test(expectedExceptions = {IllegalArgumentException.class})
    public static void testAsSpreaderInvokeWithBadElementCount() throws Throwable {
        MethodHandle spreader = MH_String_equals.asSpreader(String[].class, 2);
        assert (!(boolean) spreader.invokeExact(new String[]{"me", "thee"}));
        boolean eq = (boolean) spreader.invokeExact(new String[0]);
    }

    // ---- spread no argument ----
    @Test
    public static void testAsSpreaderPosInvokeWithZeroLength() throws Throwable {
        MethodHandle spreader = MH_spread.asSpreader(1, int[].class, 0);
        assert("A123B".equals(spreader.invoke("A", (int[])null, 1, 2, 3, "B")));
    }

    @Test
    public static void testAsSpreaderInvokeWithZeroLength() throws Throwable {
        MethodHandle spreader = MH_String_equals.asSpreader(String[].class, 0);
        assert ((boolean) spreader.invokeExact("me", (Object)"me", new String[0]));
        boolean eq = (boolean) spreader.invokeExact("me", (Object)"me", (String[]) null);
    }

    // ---- invokers with null method/var handle argument ----
    @Test(expectedExceptions = {NullPointerException.class})
    public static void testInvokerWithNull() throws Throwable {
        MethodType type = methodType(int.class, int.class, int.class);
        MethodHandle invoker = MethodHandles.invoker(type);
        assert((int) invoker.invoke(MH_add, 1, 2) == 3);
        int sum = (int)invoker.invoke((MethodHandle)null, 1, 2);
    }

    @Test(expectedExceptions = {NullPointerException.class})
    public static void testExactInvokerWithNull() throws Throwable {
        MethodType type = methodType(int.class, int.class, int.class);
        MethodHandle invoker = MethodHandles.exactInvoker(type);
        assert((int) invoker.invoke(MH_add, 1, 2) == 3);
        int sum = (int)invoker.invokeExact((MethodHandle)null, 1, 2);
    }

    @Test(expectedExceptions = {NullPointerException.class})
    public static void testSpreadInvokerWithNull() throws Throwable {
        MethodType type = methodType(boolean.class, String.class, String.class);
        MethodHandle invoker = MethodHandles.spreadInvoker(type, 0);
        assert ((boolean) invoker.invoke(MH_String_equals, new String[]{"me", "me"}));
        boolean eq = (boolean) invoker.invoke((MethodHandle)null, new String[]{"me", "me"});
    }

    @Test(expectedExceptions = {NullPointerException.class})
    public static void testVarHandleInvokerWithNull() throws Throwable {
        VarHandle.AccessMode am = VarHandle.AccessMode.GET;
        MethodHandle invoker = MethodHandles.varHandleInvoker(am, VH_array.accessModeType(am));
        assert ((int) invoker.invoke(VH_array, array, 3) == 3);
        int value = (int)invoker.invoke((VarHandle)null, array, 3);
    }

    @Test(expectedExceptions = {NullPointerException.class})
    public static void testVarHandleExactInvokerWithNull() throws Throwable {
        VarHandle.AccessMode am = VarHandle.AccessMode.GET;
        MethodHandle invoker = MethodHandles.varHandleExactInvoker(am, VH_array.accessModeType(am));
        assert ((int) invoker.invoke(VH_array, array, 3) == 3);
        int value = (int)invoker.invokeExact((VarHandle)null, array, 3);
    }

    static final Lookup LOOKUP = MethodHandles.lookup();
    static final MethodHandle MH_add;
    static final MethodHandle MH_spread;
    static final MethodHandle MH_String_equals;
    static final VarHandle VH_array;

    static final int[] array = new int[] { 0, 1, 2, 3, 4, 5};
    static {
        try {
            Class<?> arrayClass = Class.forName("[I");
            VH_array = MethodHandles.arrayElementVarHandle(arrayClass);
            MH_add = LOOKUP.findStatic(InvokeMethodHandleWithBadArgument.class, "add",
                methodType(int.class, int.class, int.class));
            MH_spread = LOOKUP.findStatic(InvokeMethodHandleWithBadArgument.class, "spread",
                methodType(String.class, String.class, int.class, int.class, int.class, String.class));
            MH_String_equals = LOOKUP.findVirtual(String.class, "equals", methodType(boolean.class, Object.class));
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    static String spread(String s1, int i1, int i2, int i3, String s2) {
        return s1 + i1 + i2 + i3 + s2;
    }

    static int add(int x, int y) {
        return x+y;
    }
}
