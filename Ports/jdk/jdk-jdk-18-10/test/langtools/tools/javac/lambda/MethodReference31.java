/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8003280
 * @summary Add lambda tests
 *  check that boxing of return-type works as expected
 */

public class MethodReference31 {

    static class Success extends RuntimeException { }

    static int assertionCount = 0;

    static void assertTrue(boolean cond) {
        assertionCount++;
        if (!cond)
            throw new AssertionError();
    }

    interface SAM<X> {
        X m();
    }

    interface SAM_byte {
        byte m();
    }

    interface SAM_short {
        short m();
    }

    interface SAM_int {
        int m();
    }

    interface SAM_long {
        long m();
    }

    interface SAM_float {
        float m();
    }

    interface SAM_double {
        double m();
    }

    static <Z> Z test() {
        assertTrue(true);
        throw new Success();
    }

    static byte test_byte() {
        assertTrue(true);
        return 0;
    }

    static short test_short() {
        assertTrue(true);
        return 0;
    }

    static int test_int() {
        assertTrue(true);
        return 0;
    }

    static long test_long() {
        assertTrue(true);
        return 0;
    }

    static float test_float() {
        assertTrue(true);
        return 0;
    }

    static double test_double() {
        assertTrue(true);
        return 0;
    }

    static void testByte() {
        SAM<Byte> s1 = MethodReference31::test_byte;
        s1.m();
        SAM_byte s2 = MethodReference31::test_byte;
        s2.m();
        SAM<Byte> s3 = MethodReference31::<Byte>test;
        try {
            s3.m();
        }
        catch (RuntimeException ex) { }
        SAM_byte s4 = MethodReference31::<Byte>test;
        try {
            s4.m();
        }
        catch (RuntimeException ex) { }
    }

    static void testShort() {
        SAM<Short> s1 = MethodReference31::test_short;
        s1.m();
        SAM_short s2 = MethodReference31::test_short;
        s2.m();
        SAM<Short> s3 = MethodReference31::<Short>test;
        try {
            s3.m();
        }
        catch (RuntimeException ex) { }
        SAM_short s4 = MethodReference31::<Short>test;
        try {
            s4.m();
        }
        catch (RuntimeException ex) { }
    }

    static void testInteger() {
        SAM<Integer> s1 = MethodReference31::test_int;
        s1.m();
        SAM_int s2 = MethodReference31::test_int;
        s2.m();
        SAM<Integer> s3 = MethodReference31::<Integer>test;
        try {
            s3.m();
        }
        catch (RuntimeException ex) { }
        SAM_int s4 = MethodReference31::<Integer>test;
        try {
            s4.m();
        }
        catch (RuntimeException ex) { }
    }

    static void testLong() {
        SAM<Long> s1 = MethodReference31::test_long;
        s1.m();
        SAM_long s2 = MethodReference31::test_long;
        s2.m();
        SAM<Long> s3 = MethodReference31::<Long>test;
        try {
            s3.m();
        }
        catch (RuntimeException ex) { }
        SAM_long s4 = MethodReference31::<Long>test;
        try {
            s4.m();
        }
        catch (RuntimeException ex) { }
    }

    static void testFloat() {
        SAM<Float> s1 = MethodReference31::test_float;
        s1.m();
        SAM_float s2 = MethodReference31::test_float;
        s2.m();
        SAM<Float> s3 = MethodReference31::<Float>test;
        try {
            s3.m();
        }
        catch (RuntimeException ex) { }
        SAM_float s4 = MethodReference31::<Float>test;
        try {
            s4.m();
        }
        catch (RuntimeException ex) { }
    }

    static void testDouble() {
        SAM<Double> s1 = MethodReference31::test_double;
        s1.m();
        SAM_double s2 = MethodReference31::test_double;
        s2.m();
        SAM<Double> s3 = MethodReference31::<Double>test;
        try {
            s3.m();
        }
        catch (RuntimeException ex) { }
        SAM_double s4 = MethodReference31::<Double>test;
        try {
            s4.m();
        }
        catch (RuntimeException ex) { }
    }

    public static void main(String[] args) {
        testByte();
        testShort();
        testInteger();
        testLong();
        testFloat();
        testDouble();
        assertTrue(assertionCount == 24);
    }
}
