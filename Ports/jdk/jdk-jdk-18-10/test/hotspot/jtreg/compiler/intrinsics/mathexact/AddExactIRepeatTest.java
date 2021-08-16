/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @key randomness
 * @bug 8025657
 * @summary Test repeating addExact
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 *          java.management
 *
 * @run main compiler.intrinsics.mathexact.AddExactIRepeatTest
 */

package compiler.intrinsics.mathexact;

import jdk.test.lib.Utils;

import java.util.Random;

public class AddExactIRepeatTest {
    public static void main(String[] args) {
        runTest(new Verify.AddExactI());
    }

    public static int nonExact(int x, int y, Verify.BinaryMethod method) {
        int result = method.unchecked(x, y);
        result += method.unchecked(x, y);
        result += method.unchecked(x, y);
        result += method.unchecked(x, y);
        return result;
    }

    public static void runTest(Verify.BinaryMethod method) {
        Random rnd = Utils.getRandomInstance();
        for (int i = 0; i < 50000; ++i) {
            int x = Integer.MAX_VALUE - 10;
            int y = Integer.MAX_VALUE - 10 + rnd.nextInt(5);

            int c = rnd.nextInt() / 2;
            int d = rnd.nextInt() / 2;

            int a = catchingExact(x, y, method);

            if (a != 36) {
                throw new RuntimeException("a != 36 : " + a);
            }

            int b = nonExact(c, d, method);
            int n = exact(c, d, method);


            if (n != b) {
                throw new RuntimeException("n != b : " + n + " != " + b);
            }
        }
    }

    public static int exact(int x, int y, Verify.BinaryMethod method) {
        int result = 0;
        result += method.checkMethod(x, y);
        result += method.checkMethod(x, y);
        result += method.checkMethod(x, y);
        result += method.checkMethod(x, y);
        return result;
    }

    public static int catchingExact(int x, int y, Verify.BinaryMethod method) {
        int result = 0;
        try {
            result += 5;
            result = method.checkMethod(x, y);
        } catch (ArithmeticException e) {
            result += 1;
        }
        try {
            result += 6;

            result += method.checkMethod(x, y);
        } catch (ArithmeticException e) {
            result += 2;
        }
        try {
            result += 7;
            result += method.checkMethod(x, y);
        } catch (ArithmeticException e) {
            result += 3;
        }
        try {
            result += 8;
            result += method.checkMethod(x, y);
        } catch (ArithmeticException e) {
            result += 4;
        }
        return result;
    }
}
