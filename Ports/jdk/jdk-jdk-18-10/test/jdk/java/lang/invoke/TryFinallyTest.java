/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8139885 8150824 8150825 8194238 8233920
 * @run testng/othervm -ea -esa -Xverify:all test.java.lang.invoke.TryFinallyTest
 */

package test.java.lang.invoke;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;

import static java.lang.invoke.MethodType.methodType;

import static org.testng.AssertJUnit.*;

import org.testng.annotations.*;

/**
 * Tests for the tryFinally method handle combinator introduced in JEP 274.
 */
public class TryFinallyTest {

    static final Lookup LOOKUP = MethodHandles.lookup();

    @Test
    public static void testTryFinally() throws Throwable {
        MethodHandle hello = MethodHandles.tryFinally(TryFinally.MH_greet, TryFinally.MH_exclaim);
        assertEquals(TryFinally.MT_hello, hello.type());
        assertEquals("Hello, world!", hello.invoke("world"));
    }

    @DataProvider
    static Object[][] tryFinallyArgs() {
        return new Object[][] {
                { boolean.class, true },
                { byte.class, (byte) 2 },
                { short.class, (short) 2 },
                { char.class, (char) 2 },
                { int.class, 2 },
                { long.class, 2L },
                { float.class, 2f },
                { double.class, 2D },
                { Object.class, new Object() }
        };
    }

    @Test(dataProvider = "tryFinallyArgs")
    public static void testTryFinally(Class<?> argType, Object arg) throws Throwable {
        MethodHandle identity = MethodHandles.identity(argType);
        MethodHandle tryFinally = MethodHandles.tryFinally(
                identity,
                MethodHandles.dropArguments(identity, 0, Throwable.class));
        assertEquals(methodType(argType, argType), tryFinally.type());
        assertEquals(arg, tryFinally.invoke(arg));
    }

    @Test(dataProvider = "tryFinallyArgs", expectedExceptions = TryFinally.T1.class)
    public static void testTryFinallyException(Class<?> argType, Object arg) throws Throwable {
        MethodHandle identity = TryFinally.MH_throwingTargetIdentity.asType(methodType(argType, argType));
        MethodHandle tryFinally = MethodHandles.tryFinally(
                identity,
                MethodHandles.dropArguments(identity, 0, TryFinally.T1.class));
        assertEquals(methodType(argType, argType), tryFinally.type());
        tryFinally.invoke(arg); // should throw
    }

    @Test
    public static void testTryFinallyVoid() throws Throwable {
        MethodHandle tfVoid = MethodHandles.tryFinally(TryFinally.MH_print, TryFinally.MH_printMore);
        assertEquals(TryFinally.MT_printHello, tfVoid.type());
        tfVoid.invoke("world");
    }

    @Test
    public static void testTryFinallySublist() throws Throwable {
        MethodHandle helloMore = MethodHandles.tryFinally(TryFinally.MH_greetMore, TryFinally.MH_exclaimMore);
        assertEquals(TryFinally.MT_moreHello, helloMore.type());
        assertEquals("Hello, world and universe (but world first)!", helloMore.invoke("world", "universe"));
    }

    @DataProvider
    static Object[][] omitTrailingArguments() {
        MethodHandle c = TryFinally.MH_voidCleanup;
        return new Object[][]{
                {c},
                {MethodHandles.dropArguments(c, 1, int.class)},
                {MethodHandles.dropArguments(c, 1, int.class, long.class)},
                {MethodHandles.dropArguments(c, 1, int.class, long.class, Object.class, int.class)},
                {MethodHandles.dropArguments(c, 1, int.class, long.class, Object.class, int.class, long.class)}
        };
    }

    @Test(dataProvider = "omitTrailingArguments")
    public static void testTryFinallyOmitTrailingArguments(MethodHandle cleanup) throws Throwable {
        MethodHandle tf = MethodHandles.tryFinally(TryFinally.MH_dummyTarget, cleanup);
        tf.invoke(1, 2L, "a", 23, 42L, "b");
    }

    @DataProvider
    static Object[][] negativeTestData() {
        MethodHandle intid = MethodHandles.identity(int.class);
        MethodHandle intco = MethodHandles.constant(int.class, 0);
        MethodHandle errTarget = MethodHandles.dropArguments(intco, 0, int.class, double.class, String.class, int.class);
        MethodHandle errCleanup = MethodHandles.dropArguments(MethodHandles.constant(int.class, 0), 0, Throwable.class,
                int.class, double.class, Object.class);
        MethodHandle voidTarget = TryFinally.MH_voidTarget;
        MethodHandle voidICleanup = MethodHandles.dropArguments(TryFinally.MH_voidCleanup, 1, int.class);
        return new Object[][]{
                {intid, MethodHandles.identity(double.class),
                        "target and return types must match: double != int"},
                {intid, MethodHandles.dropArguments(intid, 0, String.class),
                        "cleanup first argument and Throwable must match: (String,int)int != class java.lang.Throwable"},
                {intid, MethodHandles.dropArguments(intid, 0, Throwable.class, double.class),
                        "cleanup second argument and target return type must match: (Throwable,double,int)int != int"},
                {errTarget, errCleanup,
                        "cleanup parameters after (Throwable,result) and target parameter list prefix must match: " +
                                errCleanup.type() + " != " + errTarget.type()},
                {voidTarget, voidICleanup,
                        "cleanup parameters after (Throwable,result) and target parameter list prefix must match: " +
                                voidICleanup.type() + " != " + voidTarget.type()}
        };
    }

    @Test(dataProvider = "negativeTestData")
    public static void testTryFinallyNegative(MethodHandle target, MethodHandle cleanup, String expectedMessage) {
        boolean caught = false;
        try {
            MethodHandles.tryFinally(target, cleanup);
        } catch (IllegalArgumentException iae) {
            assertEquals(expectedMessage, iae.getMessage());
            caught = true;
        }
        assertTrue(caught);
    }

    @Test
    public static void testTryFinallyThrowableCheck() {
        MethodHandle mh = MethodHandles.tryFinally(TryFinally.MH_throwingTarget,
                                                   TryFinally.MH_catchingCleanup);
        try {
            mh.invoke();
            fail("ClassCastException expected");
        } catch (Throwable t) {
            assertTrue("Throwable not assignable to ClassCastException: " + t,
                       ClassCastException.class.isAssignableFrom(t.getClass()));
        }
    }

    static class TryFinally {

        static String greet(String whom) {
            return "Hello, " + whom;
        }

        static String exclaim(Throwable t, String r, String whom) {
            return r + "!";
        }

        static void print(String what) {
            System.out.print("Hello, " + what);
        }

        static void printMore(Throwable t, String what) {
            System.out.println("!");
        }

        static String greetMore(String first, String second) {
            return "Hello, " + first + " and " + second;
        }

        static String exclaimMore(Throwable t, String r, String first) {
            return r + " (but " + first + " first)!";
        }

        static void voidTarget() {}

        static void voidCleanup(Throwable t) {}

        static class T1 extends Throwable {}

        static class T2 extends Throwable {}

        static void throwingTarget() throws Throwable {
            throw new T1();
        }

        static Object throwingTargetIdentity(Object o) throws Throwable {
            throw new T1();
        }

        static void catchingCleanup(T2 t) throws Throwable {
        }

        static final Class<TryFinally> TRY_FINALLY = TryFinally.class;

        static final MethodType MT_greet = methodType(String.class, String.class);
        static final MethodType MT_exclaim = methodType(String.class, Throwable.class, String.class, String.class);
        static final MethodType MT_print = methodType(void.class, String.class);
        static final MethodType MT_printMore = methodType(void.class, Throwable.class, String.class);
        static final MethodType MT_greetMore = methodType(String.class, String.class, String.class);
        static final MethodType MT_exclaimMore = methodType(String.class, Throwable.class, String.class, String.class);
        static final MethodType MT_voidTarget = methodType(void.class);
        static final MethodType MT_voidCleanup = methodType(void.class, Throwable.class);
        static final MethodType MT_throwingTarget = methodType(void.class);
        static final MethodType MT_throwingTargetIdentity = methodType(Object.class, Object.class);
        static final MethodType MT_catchingCleanup = methodType(void.class, T2.class);

        static final MethodHandle MH_greet;
        static final MethodHandle MH_exclaim;
        static final MethodHandle MH_print;
        static final MethodHandle MH_printMore;
        static final MethodHandle MH_greetMore;
        static final MethodHandle MH_exclaimMore;
        static final MethodHandle MH_voidTarget;
        static final MethodHandle MH_voidCleanup;
        static final MethodHandle MH_throwingTarget;
        static final MethodHandle MH_throwingTargetIdentity;
        static final MethodHandle MH_catchingCleanup;

        static final MethodHandle MH_dummyTarget;

        static final MethodType MT_hello = methodType(String.class, String.class);
        static final MethodType MT_printHello = methodType(void.class, String.class);
        static final MethodType MT_moreHello = methodType(String.class, String.class, String.class);

        static {
            try {
                MH_greet = LOOKUP.findStatic(TRY_FINALLY, "greet", MT_greet);
                MH_exclaim = LOOKUP.findStatic(TRY_FINALLY, "exclaim", MT_exclaim);
                MH_print = LOOKUP.findStatic(TRY_FINALLY, "print", MT_print);
                MH_printMore = LOOKUP.findStatic(TRY_FINALLY, "printMore", MT_printMore);
                MH_greetMore = LOOKUP.findStatic(TRY_FINALLY, "greetMore", MT_greetMore);
                MH_exclaimMore = LOOKUP.findStatic(TRY_FINALLY, "exclaimMore", MT_exclaimMore);
                MH_voidTarget = LOOKUP.findStatic(TRY_FINALLY, "voidTarget", MT_voidTarget);
                MH_voidCleanup = LOOKUP.findStatic(TRY_FINALLY, "voidCleanup", MT_voidCleanup);
                MH_throwingTarget = LOOKUP.findStatic(TRY_FINALLY, "throwingTarget", MT_throwingTarget);
                MH_throwingTargetIdentity = LOOKUP.findStatic(TRY_FINALLY, "throwingTargetIdentity", MT_throwingTargetIdentity);
                MH_catchingCleanup = LOOKUP.findStatic(TRY_FINALLY, "catchingCleanup", MT_catchingCleanup);
                MH_dummyTarget = MethodHandles.dropArguments(MH_voidTarget, 0, int.class, long.class, Object.class,
                        int.class, long.class, Object.class);
            } catch (Exception e) {
                throw new ExceptionInInitializerError(e);
            }
        }

    }

}
