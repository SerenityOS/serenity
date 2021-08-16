/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8186046 8199875
 * @summary Test basic invocation of bootstrap methods
 * @library /lib/testlibrary/bytecode /java/lang/invoke/common
 * @build jdk.experimental.bytecode.BasicClassBuilder test.java.lang.invoke.lib.InstructionHelper
 * @run testng CondyBSMInvocation
 * @run testng/othervm -XX:+UnlockDiagnosticVMOptions -XX:UseBootstrapCallInfo=3 CondyBSMInvocation
 */


import org.testng.Assert;
import org.testng.annotations.Test;
import test.java.lang.invoke.lib.InstructionHelper;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.WrongMethodTypeException;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.invoke.MethodType.methodType;

public class CondyBSMInvocation {
    static final MethodHandles.Lookup L = MethodHandles.lookup();


    @Test
    public void testNonexistent() throws Throwable {
        MethodHandle mh = InstructionHelper.ldcDynamicConstant(
                L, "name", Object.class,
                "bsm", methodType(Object.class),
                S -> {});

        try {
            mh.invoke();
            Assert.fail("NoSuchMethodError expected to be thrown");
        } catch (NoSuchMethodError e) {
        }
    }

    static MethodHandle[] bsms(String bsmName) {
        return Stream.of(CondyBSMInvocation.class.getDeclaredMethods()).
                filter(m -> m.getName().equals(bsmName)).
                map(m -> {
                    try {
                        return MethodHandles.lookup().unreflect(m);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException();
                    }
                }).toArray(MethodHandle[]::new);
    }

    public static Object shape_bsm() {
        return "0";
    }

    public static Object shape_bsm(Object a1) {
        return "0";
    }

    public static Object shape_bsm(Object... args) {
        return "0";
    }

    public static Object shape_bsm(Object a1, Object a2) {
        return "0";
    }

    public static Object shape_bsm(Object a1, Object... args) {
        return "0";
    }

    public static Object shape_bsm(Object a1, Object a2, Object a3) {
        return "0";
    }

    public static Object shape_bsm(MethodHandles.Lookup a1) {
        return "0";
    }

    @Test
    public void testWrongShape() throws Throwable {
        for (MethodHandle bsm : bsms("shape_bsm")) {
            MethodHandle mh = InstructionHelper.ldcDynamicConstant(
                    L, "name", Object.class,
                    "shape_bsm", bsm.type(),
                    S -> {}
            );

            try {
                Object r = mh.invoke();
                Assert.fail("BootstrapMethodError expected to be thrown for " + bsm);
            } catch (BootstrapMethodError e) {
            }
        }
    }


    public static Object sig_bsm(MethodHandles.Lookup a1, String[] a2) {
        return "0";
    }

    public static Object sig_bsm(MethodHandles.Lookup a1, String a2, String a3) {
        return "0";
    }

    @Test
    public void testWrongSignature() throws Throwable {
        for (MethodHandle bsm : bsms("sig_bsm")) {
            MethodHandle mh = InstructionHelper.ldcDynamicConstant(
                    L, "name", Object.class,
                    "sig_bsm", bsm.type(),
                    S -> {}
            );

            try {
                Object r = mh.invoke();
                Assert.fail("BootstrapMethodError expected to be thrown for " + bsm);
            } catch (BootstrapMethodError e) {
            }
        }
    }


    public static Object bsm(MethodHandles.Lookup l, String name, Class<?> type) {
        return "0";
    }

    public static Object bsm(MethodHandles.Lookup l, String name, Class<?> type,
                             Object a1) {
        assertAll(a1);
        return "1";
    }

    public static Object bsm(MethodHandles.Lookup l, String name, Class<?> type,
                             Object a1, Object a2) {
        assertAll(a1, a2);
        return "2";
    }

    public static Object bsm(MethodHandles.Lookup l, String name, Class<?> type,
                             Object a1, Object a2, Object a3) {
        assertAll(a1, a2, a3);
        return "3";
    }

    public static Object bsm(MethodHandles.Lookup l, String name, Class<?> type,
                             Object a1, Object a2, Object a3, Object a4) {
        assertAll(a1, a2, a3, a4);
        return "4";
    }

    public static Object bsm(MethodHandles.Lookup l, String name, Class<?> type,
                             Object a1, Object a2, Object a3, Object a4, Object a5) {
        assertAll(a1, a2, a3, a4, a5);
        return "5";
    }

    public static Object bsm(MethodHandles.Lookup l, String name, Class<?> type,
                             Object a1, Object a2, Object a3, Object a4, Object a5, Object a6) {
        assertAll(a1, a2, a3, a4, a5, a6);
        return "6";
    }

    public static Object bsm(MethodHandles.Lookup l, String name, Class<?> type,
                             Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7) {
        assertAll(a1, a2, a3, a4, a5, a6, a7);
        return "7";
    }

    public static Object bsm(MethodHandles.Lookup l, Object... args) {
        Object[] staticArgs = Arrays.copyOfRange(args, 2, args.length);
        assertAll(staticArgs);
        return Integer.toString(staticArgs.length);
    }

    static void assertAll(Object... as) {
        for (int i = 0; i < as.length; i++) {
            Assert.assertEquals(as[i], i);
        }
    }

    @Test
    public void testArity() throws Throwable {
        for (int i = 0; i < 8; i++) {
            final int n = i;
            MethodType mt = methodType(Object.class, MethodHandles.Lookup.class, String.class, Class.class)
                    .appendParameterTypes(Collections.nCopies(n, Object.class));
            MethodHandle mh = InstructionHelper.ldcDynamicConstant(
                    L, "name", Object.class,
                    "bsm", mt,
                    S -> IntStream.range(0, n).forEach(S::add)
                    );

            Object r = mh.invoke();
            Assert.assertEquals(r, Integer.toString(n));
        }

        {
            MethodType mt = methodType(Object.class, MethodHandles.Lookup.class, Object[].class);
            MethodHandle mh = InstructionHelper.ldcDynamicConstant(
                    L, "name", Object.class,
                    "bsm", mt,
                    S -> IntStream.range(0, 9).forEach(S::add)
            );

            Object r = mh.invoke();
            Assert.assertEquals(r, Integer.toString(9));

        }
    }

    @Test
    public void testWrongNumberOfStaticArguments() throws Throwable {
        for (int i = 1; i < 8; i++) {
            final int n = i;
            MethodType mt = methodType(Object.class, MethodHandles.Lookup.class, String.class, Class.class)
                    .appendParameterTypes(Collections.nCopies(n, Object.class));
            MethodHandle mh = InstructionHelper.ldcDynamicConstant(
                    L, "name", Object.class,
                    "bsm", mt,
                    S -> IntStream.range(0, n - 1).forEach(S::add)
            );

            try {
                Object r = mh.invoke();
                Assert.fail("BootstrapMethodError expected to be thrown for arrity " + n);
            } catch (BootstrapMethodError e) {
                Throwable t = e.getCause();
                Assert.assertTrue(WrongMethodTypeException.class.isAssignableFrom(t.getClass()));
            }
        }
    }
}
