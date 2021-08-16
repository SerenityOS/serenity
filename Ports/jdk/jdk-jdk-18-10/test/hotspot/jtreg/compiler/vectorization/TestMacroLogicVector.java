/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8241040
 * @library /test/lib
 *
 * @run main/othervm -XX:+IgnoreUnrecognizedVMOptions
 *           -Xbatch -XX:-TieredCompilation -XX:CICompilerCount=1 -XX:UseAVX=3
 *           -XX:CompileCommand=quiet -XX:CompileCommand=compileonly,*::test*
 *           -XX:+TraceNewVectors compiler.vectorization.TestMacroLogicVector
 */
package compiler.vectorization;

import jdk.test.lib.Utils;

import java.util.Random;
import java.util.concurrent.Callable;

public class TestMacroLogicVector {
    static boolean booleanFunc1(boolean a, boolean b) {
        return a & b;
    }

    static void testSubWordBoolean(boolean[] r, boolean[] a, boolean[] b) {
        for (int i = 0; i < r.length; i++) {
            r[i] = booleanFunc1(a[i], b[i]);
        }
    }
    static void verifySubWordBoolean(boolean[] r, boolean[] a, boolean[] b) {
        for (int i = 0; i < r.length; i++) {
            boolean expected = booleanFunc1(a[i], b[i]);
            if (r[i] != expected) {
                throw new AssertionError(
                        String.format("at #%d: r=%b, expected = %b = booleanFunc1(%b,%b)",
                                      i, r[i], expected, a[i], b[i]));
            }
        }
    }


    static short charFunc1(char a, char b) {
        return (short)((a & b) & 1);
    }

    static void testSubWordChar(short[] r, char[] a, char[] b) {
        for (int i = 0; i < r.length; i++) {
            r[i] = charFunc1(a[i], b[i]);
        }
    }
    static void verifySubWordChar(short[] r, char[] a, char[] b) {
        for (int i = 0; i < r.length; i++) {
            short expected = charFunc1(a[i], b[i]);
            if (r[i] != expected) {
                throw new AssertionError(
                        String.format("at #%d: r=%d, expected = %d = booleanFunc1(%d,%d)",
                                      i, r[i], expected, (int)a[i], (int)b[i]));
            }
        }
    }

    static int intFunc1(int a, int b, int c) {
        int v1 = (a & b) ^ (a & c) ^ (b & c);
        int v2 = (~a & b) | (~b & c) | (~c & a);
        return v1 & v2;
    }
    static void testInt1(int[] r, int[] a, int[] b, int[] c) {
        for (int i = 0; i < r.length; i++) {
            r[i] = intFunc1(a[i], b[i], c[i]);
        }
    }
    static void verifyInt1(int[] r, int[] a, int[] b, int[] c) {
        for (int i = 0; i < r.length; i++) {
            int expected = intFunc1(a[i], b[i], c[i]);
            if (r[i] != expected) {
                throw new AssertionError(String.format("at #%d: r=%d, expected = %d = intFunc1(%d,%d,%d)",
                                                       i, r[i], expected, a[i], b[i], c[i]));
            }
        }
    }
    static int intFunc2(int a) {
        int v1 = (a & a) ^ (a & a) ^ (a & a);
        int v2 = (~a & ~a) | (~a & a) | (a & a);
        return v1 & v2;
    }
    static void testInt2(int[] r, int[] a) {
        for (int i = 0; i < r.length; i++) {
            r[i] = intFunc2(a[i]);
        }
    }
    static void verifyInt2(int[] r, int[] a) {
        for (int i = 0; i < r.length; i++) {
            int expected = intFunc2(a[i]);
            if (r[i] != expected) {
                throw new AssertionError(String.format("at #%d: r=%d, expected = %d = intFunc2(%d)",
                                                       i, r[i], expected, a[i]));
            }
        }
    }

    static int intFunc3(int a, int b) {
        return (~a & b) ^ (a | b);
    }
    static void testInt3(int[] r, int[] a, int[] b) {
        for (int i = 0; i < r.length; i++) {
            r[i] = intFunc3(a[i], b[i]);
        }
    }
    static void verifyInt3(int[] r, int[] a, int[] b) {
        for (int i = 0; i < r.length; i++) {
            int expected = intFunc3(a[i], b[i]);
            if (r[i] != expected) {
                throw new AssertionError(String.format("at #%d: r=%d, expected = %d = intFunc3(%d,%d)",
                                                       i, r[i], expected, a[i], b[i]));
            }
        }
    }

    static int intFunc4(int a, int b, int c, int d, int e, int f) {
        int v1 = (~a | ~b) & ~c;
        int v2 = ~d & (~e & f);
        return v1 | v2;
    }

    static void testInt4(int[] r, int[] a, int[] b, int[] c, int[] d, int[] e, int[] f) {
        for (int i = 0; i < r.length; i++) {
            r[i] = intFunc4(a[i], b[i], c[i], d[i], e[i], f[i]);
        }
    }

    static void verifyTest4(int[] r, int[] a, int[] b, int[] c, int[] d, int[] e, int[] f) {
        for (int i = 0; i < r.length; i++) {
            long expected = intFunc4(a[i], b[i], c[i], d[i], e[i], f[i]);
            if (r[i] != expected) {
                throw new AssertionError(
                        String.format("at #%d: r=%d, expected = %d = intFunc4(%d,%d,%d,%d,%d,%d)",
                                      i, r[i], expected, a[i], b[i], c[i], d[i], e[i], f[i]));
            }
        }
    }
    // ===================================================== //

    static long longFunc(long a, long b, long c) {
        long v1 = (a & b) ^ (a & c) ^ (b & c);
        long v2 = (~a & b) | (~b & c) | (~c & a);
        return v1 & v2;
    }
    static void testLong(long[] r, long[] a, long[] b, long[] c) {
        for (int i = 0; i < r.length; i++) {
            r[i] = longFunc(a[i], b[i], c[i]);
        }
    }
    static void verifyLong(long[] r, long[] a, long[] b, long[] c) {
        for (int i = 0; i < r.length; i++) {
            long expected = longFunc(a[i], b[i], c[i]);
            if (r[i] != expected) {
                throw new AssertionError(
                        String.format("at #%d: r=%d, expected = %d = longFunc(%d,%d,%d)",
                                      i, r[i], expected, a[i], b[i], c[i]));
            }
        }
    }

    // ===================================================== //

    private static final Random R = Utils.getRandomInstance();

    static boolean[] fillBooleanRandom(Callable<boolean[]> factory) {
        try {
            boolean[] arr = factory.call();
            for (int i = 0; i < arr.length; i++) {
                arr[i] = R.nextBoolean();
            }
            return arr;
        } catch (Exception e) {
            throw new InternalError(e);
        }
    }
    static char[] fillCharRandom(Callable<char[]> factory) {
        try {
            char[] arr = factory.call();
            for (int i = 0; i < arr.length; i++) {
                arr[i] = (char)R.nextInt();
            }
            return arr;
        } catch (Exception e) {
            throw new InternalError(e);
        }
    }
    static int[] fillIntRandom(Callable<int[]> factory) {
        try {
            int[] arr = factory.call();
            for (int i = 0; i < arr.length; i++) {
                arr[i] = R.nextInt();
            }
            return arr;
        } catch (Exception e) {
            throw new InternalError(e);
        }
    }
    static long[] fillLongRandom(Callable<long[]> factory) {
        try {
            long[] arr = factory.call();
            for (int i = 0; i < arr.length; i++) {
                arr[i] = R.nextLong();
            }
            return arr;
        } catch (Exception e) {
            throw new InternalError(e);
        }
    }

    // ===================================================== //

    static final int SIZE = 128;

    public static void main(String[] args) {
        boolean[] br = new boolean[SIZE];
        boolean[] ba = fillBooleanRandom((()-> new boolean[SIZE]));
        boolean[] bb = fillBooleanRandom((()-> new boolean[SIZE]));

        short[] sr = new short[SIZE];
        char[] ca = fillCharRandom((()-> new char[SIZE]));
        char[] cb = fillCharRandom((()-> new char[SIZE]));

        int[] r = new int[SIZE];
        int[] a = fillIntRandom(()-> new int[SIZE]);
        int[] b = fillIntRandom(()-> new int[SIZE]);
        int[] c = fillIntRandom(()-> new int[SIZE]);
        int[] d = fillIntRandom(()-> new int[SIZE]);
        int[] e = fillIntRandom(()-> new int[SIZE]);
        int[] f = fillIntRandom(()-> new int[SIZE]);

        long[] rl = new long[SIZE];
        long[] al = fillLongRandom(() -> new long[SIZE]);
        long[] bl = fillLongRandom(() -> new long[SIZE]);
        long[] cl = fillLongRandom(() -> new long[SIZE]);

        for (int i = 0; i < 20_000; i++) {
            testSubWordBoolean(br, ba, bb);
            verifySubWordBoolean(br, ba, bb);

            testSubWordChar(sr, ca, cb);
            verifySubWordChar(sr, ca, cb);

            testInt1(r, a, b, c);
            verifyInt1(r, a, b, c);

            testInt2(r, a);
            verifyInt2(r, a);

            testInt3(r, a, b);
            verifyInt3(r, a, b);

            testInt4(r, a, b, c, d, e, f);
            verifyTest4(r, a, b, c, d, e, f);

            testLong(rl, al, bl, cl);
            verifyLong(rl, al, bl, cl);

        }
    }
}
