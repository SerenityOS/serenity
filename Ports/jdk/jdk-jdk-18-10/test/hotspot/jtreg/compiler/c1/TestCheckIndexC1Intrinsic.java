/*
 * Copyright (c) 2021, Alibaba Group Holding Limited. All Rights Reserved.
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
 *
 */

/*
 * @test
 * @author Yi Yang
 * @bug 8265518
 * @summary C1 intrinsic support for jdk.internal.util.Preconditions.checkIndex
 * @requires vm.compiler1.enabled
 * @library /test/lib
 * @modules java.base/jdk.internal.util
 * @run main/othervm -XX:TieredStopAtLevel=1 -Xbatch
 *                   -XX:CompileCommand=dontinline,*TestCheckIndexC1Intrinsic.check*
 *                   -XX:CompileCommand=compileonly,*TestCheckIndexC1Intrinsic.check*
 *                   compiler.c1.TestCheckIndexC1Intrinsic
 */

package compiler.c1;

import jdk.test.lib.Asserts;
import jdk.internal.util.Preconditions;

public class TestCheckIndexC1Intrinsic {
    static int limit = 100;

    private static class MyException extends RuntimeException {
        public MyException(String msg) {
            super(msg);
        }
    }

    static void check0() {
        long res = Preconditions.checkIndex(0, 1, null);
        Asserts.assertEquals((int)res, 0);
        try {
            Preconditions.checkIndex(1, 1, null);
            throw new AssertionError("Expected IndexOutOfBoundsException not thrown");
        } catch (IndexOutOfBoundsException e) {
            // got it!
        }
        try {
            Preconditions.checkIndex(Integer.MIN_VALUE, -1, null);
            throw new AssertionError("Expected IndexOutOfBoundsException not thrown");
        } catch (IndexOutOfBoundsException e) {
            // got it!
        }
        try {
            Preconditions.checkIndex(Integer.MIN_VALUE, Integer.MIN_VALUE, null);
            throw new AssertionError("Expected IndexOutOfBoundsException not thrown");
        } catch (IndexOutOfBoundsException e) {
            // got it!
        }
        try {
            Preconditions.checkIndex(Long.MAX_VALUE, Long.MAX_VALUE, null);
            throw new AssertionError("Expected IndexOutOfBoundsException not thrown");
        } catch (IndexOutOfBoundsException e) {
            // got it!
        }
        res = Preconditions.checkIndex(Long.MAX_VALUE - 1, Long.MAX_VALUE, null);
        Asserts.assertEquals(res, Long.MAX_VALUE - 1);

        try {
            // read fields
            Preconditions.checkIndex(limit + 1, limit, (s, integers) -> new MyException("Reason:" + s + "::" + integers));
            throw new AssertionError("Expected IndexOutOfBoundsException not thrown");
        } catch(MyException e){
            // got it!
        }
    }

    static void check1(int i) {
        boolean trigger = false;
        try {
            Preconditions.checkIndex(i, 9999, (s, integers) -> new RuntimeException("ex"));
        } catch (RuntimeException e) {
            Asserts.assertTrue("ex".equals(e.getMessage()));
            trigger = true;
        } finally {
            if (trigger) {
                Asserts.assertTrue(i == 9999L);
            } else {
                Asserts.assertTrue(i != 9999L);
            }
        }
    }

    static void check2(long i) {
        boolean trigger = false;
        try {
            Preconditions.checkIndex(i, 9999L, (s, integers) -> new RuntimeException("ex"));
        } catch (RuntimeException e) {
            Asserts.assertTrue("ex".equals(e.getMessage()));
            trigger = true;
        } finally {
            if (trigger) {
                Asserts.assertTrue(i == 9999L);
            } else {
                Asserts.assertTrue(i != 9999L);
            }
        }
    }

    static void check3(int i) {
        boolean trigger = false;
        try {
            Preconditions.checkIndex(i, 9999, null);
        } catch (IndexOutOfBoundsException e) {
            Asserts.assertTrue(i == 9999);
            trigger = true;
        } finally {
            if (trigger) {
                Asserts.assertTrue(i == 9999L);
            } else {
                Asserts.assertTrue(i != 9999L);
            }
        }
    }

    static void check4(long i) {
        boolean trigger = false;
        try {
            Preconditions.checkIndex(i, 9999L, null);
        } catch (IndexOutOfBoundsException e) {
            trigger = true;
        } finally {
            if (trigger) {
                Asserts.assertTrue(i == 9999L);
            } else {
                Asserts.assertTrue(i != 9999L);
            }
        }
    }

    static void check5(int i) {
        Preconditions.checkIndex(i, 99999, (s, integers) -> new RuntimeException("ex"));
    }

    static void check6(long i) {
        Preconditions.checkIndex(i, 99999L, (s, integers) -> new RuntimeException("ex"));
    }

    static void check7(int i) {
        Preconditions.checkIndex(i, 99999, null);
    }

    static void check8(long i) {
        Preconditions.checkIndex(i, 99999L, null);
    }

    static void check9(int i) {
        Preconditions.checkIndex(i, i + 1, null);
    }

    static void check10(long i) {
        Preconditions.checkIndex(i, i + 1L, null);
    }

    public static void main(String... args) {
        for (int i = 0; i < 10_000; i++) {
            check0();

            check1(i);
            check2((long) i);
            check3(i);
            check4((long) i);

            check5(i);
            check6((long) i);
            check7(i);
            check8((long) i);

            check9(i);
            check10((long)i);
        }
    }
}
