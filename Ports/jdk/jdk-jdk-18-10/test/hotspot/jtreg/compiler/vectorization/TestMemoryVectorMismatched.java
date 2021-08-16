/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
package compiler.vectorization;

/**
 * @test
 * @bug 8263972
 * @requires vm.compiler2.enabled & vm.compMode != "Xint"
 *
 * @run main/othervm -Xcomp -XX:-TieredCompilation -XX:CompileCommand=quiet -XX:CompileCommand=compileonly,*TestMemoryVectorMismatched::test compiler.vectorization.TestMemoryVectorMismatched
 */
public class TestMemoryVectorMismatched {
    public static void main(String[] g) {
        int a = 400;
        long expected = -35984L;
        for (int i = 0; i < 10; i++) {
            long v = test(a);
            if (v != expected) {
                throw new AssertionError("Wrong result: " + v + " != " + expected);
            }
        }
    }

    static long test(int a) {
        int i16, d = 5, e = -56973;
        long f[] = new long[a];
        init(f, 5);
        for (i16 = 2; i16 < 92; i16++) {
            f[i16 - 1] *= d;
            f[i16 + 1] *= d;
        }
        while (++e < 0) {
        }
        return checkSum(f);
    }

    public static void init(long[] a, long seed) {
        for (int j = 0; j < a.length; j++) {
            a[j] = (j % 2 == 0) ? seed + j : seed - j;
        }
    }


    public static long checkSum(long[] a) {
        long sum = 0;
        for (int j = 0; j < a.length; j++) {
            sum += (a[j] / (j + 1) + a[j] % (j + 1));
        }
        return sum;
    }
}
