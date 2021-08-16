/*
 * Copyright (c) 2021, Red Hat, Inc. All rights reserved.
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
 * @bug 8270366
 * @summary Test corner cases of integer associative rules
 *
 * @run main/othervm -XX:-TieredCompilation -XX:-BackgroundCompilation -XX:-UseOnStackReplacement TestAssociative
 *
 */

public class TestAssociative {
    private static class IntParams {
        int a;
        int b;
        int c;
        public IntParams(int a, int b, int c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }
    }

    private static class LongParams {
        long a;
        long b;
        long c;
        public LongParams(long a, long b, long c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }
    }

    private static final IntParams[]  intParamsArray = {
        new IntParams(17, 34, 10),
        new IntParams(Integer.MAX_VALUE - 4, 34, 10),
        new IntParams(7, Integer.MAX_VALUE, 10),
        new IntParams(7, Integer.MAX_VALUE, Integer.MAX_VALUE),
        new IntParams(10, Integer.MIN_VALUE, Integer.MIN_VALUE),
        new IntParams(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE)
    };

    private static final LongParams[] longParamsArray = {
        new LongParams(17, 34, 10),
        new LongParams(Long.MAX_VALUE - 4, 34, 10),
        new LongParams(7, Long.MAX_VALUE, 10),
        new LongParams(7, Long.MAX_VALUE, Long.MAX_VALUE),
        new LongParams(10, Long.MIN_VALUE, Long.MIN_VALUE),
        new LongParams(Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE)
    };

    // Integer
    private static interface IntAssociativeTest {
        public int test(int a, int b, int c);
    }

    // Integer Add
    private static class IntAssociativeTest0 implements IntAssociativeTest {
        public int test(int a, int b, int c) {
            return a * b + a * c;
        }
    }

    private static class IntAssociativeTest1 implements IntAssociativeTest {
        public int test(int a, int b, int c) {
            return a * b + b * c;
        }
    }

    private static class IntAssociativeTest2 implements IntAssociativeTest {
        public int test(int a, int b, int c) {
            return a * c +  b * c;
        }
    }

    private static class IntAssociativeTest3 implements IntAssociativeTest {
        public int test(int a, int b, int c) {
            return a * b + c * a;
        }
    }

    // Integer Substract
    private static class IntAssociativeTest4 implements IntAssociativeTest {
        public int test(int a, int b, int c) {
            return a * b - a * c;
        }
    }

    private static class IntAssociativeTest5 implements IntAssociativeTest {
        public int test(int a, int b, int c) {
            return a * b - b * c;
        }
    }

    private static class IntAssociativeTest6 implements IntAssociativeTest {
        public int test(int a, int b, int c) {
            return a * c -  b * c;
        }
    }

    private static class IntAssociativeTest7 implements IntAssociativeTest {
        public int test(int a, int b, int c) {
            return a * b - c * a;
        }
    }


    // Long
    private static interface LongAssociativeTest {
        public long test(long a, long b, long c);
    }

    // Long Add
    private static class LongAssociativeTest0 implements LongAssociativeTest {
        public long test(long a, long b, long c) {
            return a * b + a * c;
        }
    }

    private static class LongAssociativeTest1 implements LongAssociativeTest {
        public long test(long a, long b, long c) {
            return a * b + b * c;
        }
    }

    private static class LongAssociativeTest2 implements LongAssociativeTest {
        public long test(long a, long b, long c) {
            return a * c +  b * c;
        }
    }

    private static class LongAssociativeTest3 implements LongAssociativeTest {
        public long test(long a, long b, long c) {
            return a * b + c * a;
        }
    }

    // Long Substract
    private static class LongAssociativeTest4 implements LongAssociativeTest {
        public long test(long a, long b, long c) {
            return a * b - a * c;
        }
    }

    private static class LongAssociativeTest5 implements LongAssociativeTest {
        public long test(long a, long b, long c) {
            return a * b - b * c;
        }
    }

    private static class LongAssociativeTest6 implements LongAssociativeTest {
        public long test(long a, long b, long c) {
            return a * c -  b * c;
        }
    }

    private static class LongAssociativeTest7 implements LongAssociativeTest {
        public long test(long a, long b, long c) {
            return a * b - c * a;
        }
    }

    private static void runIntTest(IntAssociativeTest t) {
        for (IntParams p : intParamsArray) {
            int result = t.test(p.a, p.b, p.c);
            for (int i = 0; i < 20_000; i++) {
                if (result != t.test(p.a, p.b, p.c)) {
                    throw new RuntimeException("incorrect result");
                }
            }
        }
    }

    private static void runLongTest(LongAssociativeTest t) {
        for (LongParams p : longParamsArray) {
            long result = t.test(p.a, p.b, p.c);
            for (int i = 0; i < 20_000; i++) {
                if (result != t.test(p.a, p.b, p.c)) {
                    throw new RuntimeException("incorrect result");
                }
            }
        }
    }

    private static final IntAssociativeTest[] intTests = {
        new IntAssociativeTest0(),
        new IntAssociativeTest1(),
        new IntAssociativeTest2(),
        new IntAssociativeTest3(),
        new IntAssociativeTest4(),
        new IntAssociativeTest5(),
        new IntAssociativeTest6(),
        new IntAssociativeTest7()
    };

    private static final LongAssociativeTest[] longTests = {
        new LongAssociativeTest0(),
        new LongAssociativeTest1(),
        new LongAssociativeTest2(),
        new LongAssociativeTest3(),
        new LongAssociativeTest4(),
        new LongAssociativeTest5(),
        new LongAssociativeTest6(),
        new LongAssociativeTest7()
    };

    public static void main(String[] args) {
        for (IntAssociativeTest t: intTests) {
            runIntTest(t);
        }

        for (LongAssociativeTest t: longTests) {
            runLongTest(t);
        }
    }
}
