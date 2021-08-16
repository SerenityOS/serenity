/*
 * Copyright (c) 2003, 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4889342
 * @summary This test verifies that when a primitive boolean value is
 * passed by a dynamic proxy class to an invocation handler, it is
 * boxed as one of the canonical Boolean instances (Boolean.TRUE or
 * Boolean.FALSE).  This test also exercises a dynamic proxy class's
 * boxing of all primitive types.
 * @author Peter Jones
 *
 * @run main Boxing
 */

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Random;

public class Boxing {

    private interface Test {
        void m(byte b,
               char c,
               double d,
               float f,
               int i,
               long j,
               short s,
               boolean z);
    }

    private static final int REPS = 100000;

    private byte b;
    private char c;
    private double d;
    private float f;
    private int i;
    private long j;
    private short s;
    private boolean z;

    public static void main(String[] args) {
        (new Boxing()).run();
        System.err.println("TEST PASSED");
    }

    private void run() {
        Random random = new Random(42); // ensure consistent test domain

        Test proxy = (Test) Proxy.newProxyInstance(
            Test.class.getClassLoader(),
            new Class<?>[] { Test.class },
            new TestHandler());

        for (int rep = 0; rep < REPS; rep++) {
            b = (byte) random.nextInt();
            c = (char) random.nextInt();
            d = random.nextDouble();
            f = random.nextFloat();
            i = random.nextInt();
            j = random.nextLong();
            s = (short) random.nextInt();
            z = random.nextBoolean();
            proxy.m(b,c,d,f,i,j,s,z);
        }
    }

    private class TestHandler implements InvocationHandler {
        public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable
        {
            if (!method.getName().equals("m")) {
                throw new AssertionError();
            }

            byte b2 = ((Byte) args[0]).byteValue();
            if (b2 != b) {
                throw new RuntimeException("TEST FAILED: " +
                    "wrong byte, expected " + b + " but got " + b2);
            }

            char c2 = ((Character) args[1]).charValue();
            if (c2 != c) {
                throw new RuntimeException("TEST FAILED: " +
                    "wrong char, expected " + c + " but got " + c2);
            }

            double d2 = ((Double) args[2]).doubleValue();
            if (d2 != d) {
                throw new RuntimeException("TEST FAILED: " +
                    "wrong double, expected " + d + " but got " + d2);
            }

            float f2 = ((Float) args[3]).floatValue();
            if (f2 != f) {
                throw new RuntimeException("TEST FAILED: " +
                    "wrong float, expected " + f + " but got " + f2);
            }

            int i2 = ((Integer) args[4]).intValue();
            if (i2 != i) {
                throw new RuntimeException("TEST FAILED: " +
                    "wrong int, expected " + i + " but got " + i2);
            }

            long j2 = ((Long) args[5]).longValue();
            if (j2 != j) {
                throw new RuntimeException("TEST FAILED: " +
                    "wrong long, expected " + j + " but got " + j2);
            }

            short s2 = ((Short) args[6]).shortValue();
            if (s2 != s) {
                throw new RuntimeException("TEST FAILED: " +
                    "wrong short, expected " + s + " but got " + s2);
            }

            Boolean Z = Boolean.valueOf(z);
            Boolean Z2 = (Boolean) args[7];
            if (Z2 != Z) {
                throw new RuntimeException("TEST FAILED: " +
                    "wrong Boolean instance, expected " +
                    identityToString(Z) + " but got " + identityToString(Z2));
            }

            return null;
        }
    }

    private static String identityToString(Object obj) {
        return obj.toString() + "@" + System.identityHashCode(obj);
    }
}
