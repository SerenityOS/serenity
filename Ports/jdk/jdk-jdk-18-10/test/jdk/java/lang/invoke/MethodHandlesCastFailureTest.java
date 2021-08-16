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

/* @test
 * @summary unit tests for java.lang.invoke.MethodHandles
 * @library /test/lib /java/lang/invoke/common
 * @compile MethodHandlesTest.java MethodHandlesCastFailureTest.java remote/RemoteExample.java
 * @run junit/othervm/timeout=2500 -XX:+IgnoreUnrecognizedVMOptions
 *                                 -XX:-VerifyDependencies
 *                                 -esa
 *                                 test.java.lang.invoke.MethodHandlesCastFailureTest
 */

package test.java.lang.invoke;

import org.junit.*;
import test.java.lang.invoke.lib.CodeCacheOverflowProcessor;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import static org.junit.Assert.*;

public class MethodHandlesCastFailureTest extends MethodHandlesTest {

    @Test  // SLOW
    public void testCastFailure() throws Throwable {
        CodeCacheOverflowProcessor.runMHTest(this::testCastFailure0);
    }

    public void testCastFailure0() throws Throwable {
        if (CAN_SKIP_WORKING)  return;
        startTest("testCastFailure");
        testCastFailure("cast/argument", 11000);
        if (CAN_TEST_LIGHTLY)  return;
        testCastFailure("unbox/argument", 11000);
        testCastFailure("cast/return", 11000);
        testCastFailure("unbox/return", 11000);
    }

    static class Surprise {
        public MethodHandle asMethodHandle() {
            return VALUE.bindTo(this);
        }
        Object value(Object x) {
            trace("value", x);
            if (boo != null)  return boo;
            return x;
        }
        Object boo;
        void boo(Object x) { boo = x; }

        static void trace(String x, Object y) {
            if (verbosity > 8) System.out.println(x+"="+y);
        }
        static Object  refIdentity(Object x)  { trace("ref.x", x); return x; }
        static Integer boxIdentity(Integer x) { trace("box.x", x); return x; }
        static int     intIdentity(int x)     { trace("int.x", x); return x; }
        static MethodHandle VALUE, REF_IDENTITY, BOX_IDENTITY, INT_IDENTITY;
        static {
            try {
                VALUE = PRIVATE.findVirtual(
                    Surprise.class, "value",
                        MethodType.methodType(Object.class, Object.class));
                REF_IDENTITY = PRIVATE.findStatic(
                    Surprise.class, "refIdentity",
                        MethodType.methodType(Object.class, Object.class));
                BOX_IDENTITY = PRIVATE.findStatic(
                    Surprise.class, "boxIdentity",
                        MethodType.methodType(Integer.class, Integer.class));
                INT_IDENTITY = PRIVATE.findStatic(
                    Surprise.class, "intIdentity",
                        MethodType.methodType(int.class, int.class));
            } catch (NoSuchMethodException | IllegalAccessException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    @SuppressWarnings("ConvertToStringSwitch")
    void testCastFailure(String mode, int okCount) throws Throwable {
        countTest(false);
        if (verbosity > 2)  System.out.println("mode="+mode);
        Surprise boo = new Surprise();
        MethodHandle identity = Surprise.REF_IDENTITY, surprise0 = boo.asMethodHandle(), surprise = surprise0;
        if (mode.endsWith("/return")) {
            if (mode.equals("unbox/return")) {
                // fail on return to ((Integer)surprise).intValue
                surprise = surprise.asType(MethodType.methodType(int.class, Object.class));
                identity = identity.asType(MethodType.methodType(int.class, Object.class));
            } else if (mode.equals("cast/return")) {
                // fail on return to (Integer)surprise
                surprise = surprise.asType(MethodType.methodType(Integer.class, Object.class));
                identity = identity.asType(MethodType.methodType(Integer.class, Object.class));
            }
        } else if (mode.endsWith("/argument")) {
            MethodHandle callee = null;
            if (mode.equals("unbox/argument")) {
                // fail on handing surprise to int argument
                callee = Surprise.INT_IDENTITY;
            } else if (mode.equals("cast/argument")) {
                // fail on handing surprise to Integer argument
                callee = Surprise.BOX_IDENTITY;
            }
            if (callee != null) {
                callee = callee.asType(MethodType.genericMethodType(1));
                surprise = MethodHandles.filterArguments(callee, 0, surprise);
                identity = MethodHandles.filterArguments(callee, 0, identity);
            }
        }
        assertNotSame(mode, surprise, surprise0);
        identity = identity.asType(MethodType.genericMethodType(1));
        surprise = surprise.asType(MethodType.genericMethodType(1));
        Object x = 42;
        for (int i = 0; i < okCount; i++) {
            Object y = identity.invokeExact(x);
            assertEquals(x, y);
            Object z = surprise.invokeExact(x);
            assertEquals(x, z);
        }
        boo.boo("Boo!");
        Object y = identity.invokeExact(x);
        assertEquals(x, y);
        try {
            Object z = surprise.invokeExact(x);
            System.out.println("Failed to throw; got z="+z);
            assertTrue(false);
        } catch (ClassCastException ex) {
            if (verbosity > 2)
                System.out.println("caught "+ex);
            if (verbosity > 3)
                ex.printStackTrace(System.out);
            assertTrue(true);  // all is well
        }
    }
}
