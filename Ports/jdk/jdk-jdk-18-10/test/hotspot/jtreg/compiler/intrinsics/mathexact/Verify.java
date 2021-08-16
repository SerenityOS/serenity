/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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

package compiler.intrinsics.mathexact;

import jdk.test.lib.Utils;

import java.util.Random;

/**
 * The class depends on Utils class from testlibrary package.
 * It uses factory method that obtains random generator.
 */
public class Verify {
    public static String throwWord(boolean threw) {
        return (threw ? "threw" : "didn't throw");
    }

    public static void verifyResult(UnaryMethod method, int result1, int result2, boolean exception1, boolean exception2, int value) {
        if (exception1 != exception2) {
            throw new RuntimeException("Intrinsic version [" + method.name() + "]" + throwWord(exception1) + " exception, NonIntrinsic version" + throwWord(exception2) + " for: " + value);
        }
        if (result1 != result2) {
            throw new RuntimeException("Intrinsic version [" + method.name() + "] returned: " + result1 + " while NonIntrinsic version returned: " + result2);
        }
    }

    public static void verifyResult(UnaryLongMethod method, long result1, long result2, boolean exception1, boolean exception2, long value) {
        if (exception1 != exception2) {
            throw new RuntimeException("Intrinsic version [" + method.name() + "]" + throwWord(exception1) + " exception, NonIntrinsic version" + throwWord(exception2) + " for: " + value);
        }
        if (result1 != result2) {
            throw new RuntimeException("Intrinsic version [" + method.name() + "] returned: " + result1 + " while NonIntrinsic version returned: " + result2);
        }
    }

    private static void verifyResult(BinaryMethod method, int result1, int result2, boolean exception1, boolean exception2, int a, int b) {
        if (exception1 != exception2) {
            throw new RuntimeException("Intrinsic version [" + method.name() + "]" + throwWord(exception1) + " exception, NonIntrinsic version " + throwWord(exception2) + " for: " + a + " + " + b);
        }
        if (result1 != result2) {
            throw new RuntimeException("Intrinsic version [" + method.name() + "] returned: " + result1 + " while NonIntrinsic version returned: " + result2);
        }
    }

    private static void verifyResult(BinaryLongMethod method, long result1, long result2, boolean exception1, boolean exception2, long a, long b) {
        if (exception1 != exception2) {
            throw new RuntimeException("Intrinsic version [" + method.name() + "]" + throwWord(exception1) + " exception, NonIntrinsic version " + throwWord(exception2) + " for: " + a + " + " + b);
        }
        if (result1 != result2) {
            throw new RuntimeException("Intrinsic version [" + method.name() + "] returned: " + result1 + " while NonIntrinsic version returned: " + result2);
        }
    }


    public static void verifyUnary(int a, UnaryMethod method) {
        boolean exception1 = false, exception2 = false;
        int result1 = 0, result2 = 0;
        try {
            result1 = method.checkMethod(a);
        } catch (ArithmeticException e) {
            exception1 = true;
        }
        try {
            result2 = method.safeMethod(a);
        } catch (ArithmeticException e) {
            exception2 = true;
        }

        verifyResult(method, result1, result2, exception1, exception2, a);
    }

    public static void verifyUnary(long a, UnaryLongMethod method) {
        boolean exception1 = false, exception2 = false;
        long result1 = 0, result2 = 0;
        try {
            result1 = method.checkMethod(a);
        } catch (ArithmeticException e) {
            exception1 = true;
        }
        try {
            result2 = method.safeMethod(a);
        } catch (ArithmeticException e) {
            exception2 = true;
        }

        verifyResult(method, result1, result2, exception1, exception2, a);
    }


    public static void verifyBinary(int a, int b, BinaryMethod method) {
        boolean exception1 = false, exception2 = false;
        int result1 = 0, result2 = 0;
        try {
            result1 = method.checkMethod(a, b);
        } catch (ArithmeticException e) {
            exception1 = true;
        }
        try {
            result2 = method.safeMethod(a, b);
        } catch (ArithmeticException e) {
            exception2 = true;
        }

        verifyResult(method, result1, result2, exception1, exception2, a, b);
    }

    public static void verifyBinary(long a, long b, BinaryLongMethod method) {
        boolean exception1 = false, exception2 = false;
        long result1 = 0, result2 = 0;
        try {
            result1 = method.checkMethod(a, b);
        } catch (ArithmeticException e) {
            exception1 = true;
        }
        try {
            result2 = method.safeMethod(a, b);
        } catch (ArithmeticException e) {
            exception2 = true;
        }

        verifyResult(method, result1, result2, exception1, exception2, a, b);
    }


    public static class LoadTest {
        public static Random rnd = Utils.getRandomInstance();
        public static int[] values = new int[256];

        public static void init() {
            for (int i = 0; i < values.length; ++i) {
                values[i] = rnd.nextInt();
            }
        }

        public static void verify(BinaryMethod method) {
            for (int i = 0; i < 50000; ++i) {
                Verify.verifyBinary(values[i & 255], values[i & 255] - i, method);
                Verify.verifyBinary(values[i & 255] + i, values[i & 255] - i, method);
                Verify.verifyBinary(values[i & 255], values[i & 255], method);
                if ((i & 1) == 1 && i > 5) {
                    Verify.verifyBinary(values[i & 255] + i, values[i & 255] - i, method);
                } else {
                    Verify.verifyBinary(values[i & 255] - i, values[i & 255] + i, method);
                }
                Verify.verifyBinary(values[i & 255], values[(i + 1) & 255], method);
            }
        }
    }

    public static class NonConstantTest {
        public static Random rnd = Utils.getRandomInstance();
        public static int[] values = new int[] { Integer.MAX_VALUE, Integer.MIN_VALUE };

        public static void verify(BinaryMethod method) {
            for (int i = 0; i < 50000; ++i) {
                int rnd1 = rnd.nextInt(), rnd2 = rnd.nextInt();
                Verify.verifyBinary(rnd1, rnd2, method);
                Verify.verifyBinary(rnd1, rnd2 + 1, method);
                Verify.verifyBinary(rnd1 + 1, rnd2, method);
                Verify.verifyBinary(rnd1 - 1, rnd2, method);
                Verify.verifyBinary(rnd1, rnd2 - 1, method);
                Verify.verifyBinary(0, values[0], method);
                Verify.verifyBinary(values[0], 0, method);
                Verify.verifyBinary(0, values[1], method);
                Verify.verifyBinary(values[1], 0, method);
            }
        }
    }

    public static class NonConstantLongTest {
        public static long[] values = { Long.MIN_VALUE, Long.MAX_VALUE, 0, Long.MAX_VALUE - 1831 };
        public static Random rnd = Utils.getRandomInstance();

        public static void verify(BinaryLongMethod method) {
            for (int i = 0; i < 50000; ++i) {
                long rnd1 = rnd.nextLong(), rnd2 = rnd.nextLong();
                Verify.verifyBinary(rnd1, rnd2, method);
                Verify.verifyBinary(rnd1, rnd2 + 1, method);
                Verify.verifyBinary(rnd1 + 1, rnd2, method);
                Verify.verifyBinary(rnd1 - 1, rnd2, method);
                Verify.verifyBinary(rnd1, rnd2 - 1, method);
                Verify.verifyBinary(rnd1 + Long.MAX_VALUE - rnd2, rnd2 + 1, method);
                Verify.verifyBinary(values[0], values[2], method);
                Verify.verifyBinary(values[1], values[2], method);
                Verify.verifyBinary(values[3], 74L, method);
            }
        }
    }

    public static class LoopDependentTest {
        public static Random rnd = Utils.getRandomInstance();

        public static void verify(BinaryMethod method) {
            int rnd1 = rnd.nextInt(), rnd2 = rnd.nextInt();
            runTest(rnd1, rnd2, method);
        }

        private static void runTest(int rnd1, int rnd2, BinaryMethod method) {
            for (int i = 0; i < 50000; ++i) {
                Verify.verifyBinary(rnd1 + i, rnd2 + i, method);
                Verify.verifyBinary(rnd1 + i, rnd2 + (i & 0xff), method);
                Verify.verifyBinary(rnd1 - i, rnd2 - (i & 0xff), method);
                Verify.verifyBinary(rnd1 + i + 1, rnd2 + i + 2, method);
                Verify.verifyBinary(rnd1 + i * 2, rnd2 + i, method);
            }
        }
    }

    public static class ConstantTest {
        public static void verify(BinaryMethod method) {
            for (int i = 0; i < 50000; ++i) {
                Verify.verifyBinary(5, 7, method);
                Verify.verifyBinary(Integer.MAX_VALUE, 1, method);
                Verify.verifyBinary(Integer.MIN_VALUE, -1, method);
                Verify.verifyBinary(Integer.MAX_VALUE, -1, method);
                Verify.verifyBinary(Integer.MIN_VALUE, 1, method);
                Verify.verifyBinary(Integer.MAX_VALUE / 2, Integer.MAX_VALUE / 2, method);
                Verify.verifyBinary(Integer.MAX_VALUE / 2, (Integer.MAX_VALUE / 2) + 3, method);
                Verify.verifyBinary(Integer.MAX_VALUE, Integer.MIN_VALUE, method);
            }
        }
    }

    public static class ConstantLongTest {
        public static void verify(BinaryLongMethod method) {
            for (int i = 0; i < 50000; ++i) {
                Verify.verifyBinary(5, 7, method);
                Verify.verifyBinary(Long.MAX_VALUE, 1, method);
                Verify.verifyBinary(Long.MIN_VALUE, -1, method);
                Verify.verifyBinary(Long.MAX_VALUE, -1, method);
                Verify.verifyBinary(Long.MIN_VALUE, 1, method);
                Verify.verifyBinary(Long.MAX_VALUE / 2, Long.MAX_VALUE / 2, method);
                Verify.verifyBinary(Long.MAX_VALUE / 2, (Long.MAX_VALUE / 2) + 3, method);
                Verify.verifyBinary(Long.MAX_VALUE, Long.MIN_VALUE, method);
            }
        }
    }

    public static interface BinaryMethod {
        int safeMethod(int a, int b);
        int checkMethod(int a, int b);
        int unchecked(int a, int b);
        String name();
    }

    public static interface UnaryMethod {
        int safeMethod(int value);
        int checkMethod(int value);
        int unchecked(int value);
        String name();
    }

    public static interface BinaryLongMethod {
        long safeMethod(long a, long b);
        long checkMethod(long a, long b);
        long unchecked(long a, long b);
        String name();
    }

    public static interface UnaryLongMethod {
        long safeMethod(long value);
        long checkMethod(long value);
        long unchecked(long value);
        String name();
    }

    public static class UnaryToBinary implements BinaryMethod {
        private final UnaryMethod method;
        public UnaryToBinary(UnaryMethod method) {
            this.method = method;
        }

        @Override
        public int safeMethod(int a, int b) {
            return method.safeMethod(a);
        }

        @Override
        public int checkMethod(int a, int b) {
            return method.checkMethod(a);
        }

        @Override
        public int unchecked(int a, int b) {
            return method.unchecked(a);

        }

        @Override
        public String name() {
            return method.name();
        }
    }

    public static class UnaryToBinaryLong implements BinaryLongMethod {
        private final UnaryLongMethod method;
        public UnaryToBinaryLong(UnaryLongMethod method) {
            this.method = method;
        }

        @Override
        public long safeMethod(long a, long b) {
            return method.safeMethod(a);
        }

        @Override
        public long checkMethod(long a, long b) {
            return method.checkMethod(a);
        }

        @Override
        public long unchecked(long a, long b) {
            return method.unchecked(a);

        }

        @Override
        public String name() {
            return method.name();
        }
    }


    public static class AddExactI implements BinaryMethod {
        @Override
        public int safeMethod(int x, int y) {
            int r = x + y;
            // HD 2-12 Overflow iff both arguments have the opposite sign of the result
            if (((x ^ r) & (y ^ r)) < 0) {
                throw new ArithmeticException("integer overflow");
            }
            return r;

        }

        @Override
        public int checkMethod(int a, int b) {
            return Math.addExact(a, b);
        }

        @Override
        public String name() {
            return "addExact";
        }

        @Override
        public int unchecked(int a, int b) {
            return a + b;
        }
    }

    public static class AddExactL implements BinaryLongMethod {
        @Override
        public long safeMethod(long x, long y) {
            long r = x + y;
            // HD 2-12 Overflow iff both arguments have the opposite sign of the result
            if (((x ^ r) & (y ^ r)) < 0) {
                throw new ArithmeticException("integer overflow");
            }
            return r;

        }

        @Override
        public long checkMethod(long a, long b) {
            return Math.addExact(a, b);
        }

        @Override
        public String name() {
            return "addExactLong";
        }

        @Override
        public long unchecked(long a, long b) {
            return a + b;
        }
    }

    public static class MulExactI implements BinaryMethod {
        @Override
        public int safeMethod(int x, int y) {
            long r = (long)x * (long)y;
            if ((int)r != r) {
                throw new ArithmeticException("integer overflow");
            }
            return (int)r;

        }

        @Override
        public int checkMethod(int a, int b) {
            return Math.multiplyExact(a, b);
        }

        @Override
        public int unchecked(int a, int b) {
            return a * b;
        }

        @Override
        public String name() {
            return "multiplyExact";
        }
    }

    public static class MulExactL implements BinaryLongMethod {
        @Override
        public long safeMethod(long x, long y) {
            long r = x * y;
            long ax = Math.abs(x);
            long ay = Math.abs(y);
            if (((ax | ay) >>> 31 != 0)) {
                // Some bits greater than 2^31 that might cause overflow
                // Check the result using the divide operator
                // and check for the special case of Long.MIN_VALUE * -1
                if (((y != 0) && (r / y != x)) ||
                        (x == Long.MIN_VALUE && y == -1)) {
                    throw new ArithmeticException("long overflow");
                }
            }
            return r;
        }

        @Override
        public long checkMethod(long a, long b) {
            return Math.multiplyExact(a, b);
        }

        @Override
        public long unchecked(long a, long b) {
            return a * b;
        }

        @Override
        public String name() {
            return "multiplyExact";
        }
    }

    public static class NegExactL implements UnaryLongMethod {
        @Override
        public long safeMethod(long a) {
            if (a == Long.MIN_VALUE) {
                throw new ArithmeticException("long overflow");
            }

            return -a;

        }

        @Override
        public long checkMethod(long value) {
            return Math.negateExact(value);
        }

        @Override
        public long unchecked(long value) {
            return -value;
        }

        @Override
        public String name() {
            return "negateExactLong";
        }
    }

    public static class NegExactI implements UnaryMethod {
        @Override
        public int safeMethod(int a) {
            if (a == Integer.MIN_VALUE) {
                throw new ArithmeticException("integer overflow");
            }

            return -a;

        }

        @Override
        public int checkMethod(int value) {
            return Math.negateExact(value);
        }

        @Override
        public int unchecked(int value) {
            return -value;
        }

        @Override
        public String name() {
            return "negateExact";
        }
    }

    public static class SubExactI implements BinaryMethod {
        @Override
        public int safeMethod(int x, int y) {
            int r = x - y;
            // HD 2-12 Overflow iff the arguments have different signs and
            // the sign of the result is different than the sign of x
            if (((x ^ y) & (x ^ r)) < 0) {
                throw new ArithmeticException("integer overflow");
            }
            return r;
        }

        @Override
        public int checkMethod(int a, int b) {
            return Math.subtractExact(a, b);
        }

        @Override
        public int unchecked(int a, int b) {
            return a - b;
        }

        @Override
        public String name() {
            return "subtractExact";
        }
    }

    public static class SubExactL implements BinaryLongMethod {
        @Override
        public long safeMethod(long x, long y) {
            long r = x - y;
            // HD 2-12 Overflow iff the arguments have different signs and
            // the sign of the result is different than the sign of x
            if (((x ^ y) & (x ^ r)) < 0) {
                throw new ArithmeticException("integer overflow");
            }
            return r;
        }

        @Override
        public long checkMethod(long a, long b) {
            return Math.subtractExact(a, b);
        }

        @Override
        public long unchecked(long a, long b) {
            return a - b;
        }

        @Override
        public String name() {
            return "subtractExactLong";
        }
    }

    static class IncExactL implements UnaryLongMethod {
        @Override
        public long safeMethod(long a) {
            if (a == Long.MAX_VALUE) {
                throw new ArithmeticException("long overflow");
            }

            return a + 1L;

        }

        @Override
        public long checkMethod(long value) {
            return Math.incrementExact(value);
        }

        @Override
        public long unchecked(long value) {
            return value + 1;
        }

        @Override
        public String name() {
            return "incrementExactLong";
        }
    }

    static class IncExactI implements UnaryMethod {
        @Override
        public int safeMethod(int a) {
            if (a == Integer.MAX_VALUE) {
                throw new ArithmeticException("integer overflow");
            }

            return a + 1;
        }

        @Override
        public int checkMethod(int value) {
            return Math.incrementExact(value);
        }

        @Override
        public int unchecked(int value) {
            return value + 1;
        }

        @Override
        public String name() {
            return "incrementExact";
        }
    }

    static class DecExactL implements UnaryLongMethod {
        @Override
        public long safeMethod(long a) {
            if (a == Long.MIN_VALUE) {
                throw new ArithmeticException("long overflow");
            }

            return a - 1L;
        }

        @Override
        public long checkMethod(long value) {
            return Math.decrementExact(value);
        }

        @Override
        public long unchecked(long value) {
            return value - 1;
        }

        @Override
        public String name() {
            return "decExactLong";
        }
    }

    static class DecExactI implements UnaryMethod {
        @Override
        public int safeMethod(int a) {
            if (a == Integer.MIN_VALUE) {
                throw new ArithmeticException("integer overflow");
            }

            return a - 1;
        }

        @Override
        public int checkMethod(int value) {
            return Math.decrementExact(value);
        }

        @Override
        public int unchecked(int value) {
            return value - 1;
        }

        @Override
        public String name() {
            return "decrementExact";
        }
    }

}
