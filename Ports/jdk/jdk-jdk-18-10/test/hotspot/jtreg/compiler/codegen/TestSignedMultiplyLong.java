/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2019, Arm Limited. All rights reserved.
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
 * @bug 8232591
 * @summary Test some cases of combined signed multiply long operation
 * @library /test/lib
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions
 *                   -Xcomp -XX:-TieredCompilation -XX:-Inline
 *                   compiler.codegen.TestSignedMultiplyLong
 */

package compiler.codegen;

import jdk.test.lib.Asserts;

public class TestSignedMultiplyLong {

    private static final int   minInt = Integer.MIN_VALUE; // -2147483648
    private static final int   maxInt = Integer.MAX_VALUE; //  2147483647
    private static final long minLong =    Long.MIN_VALUE; // -9223372036854775808
    private static final long maxLong =    Long.MAX_VALUE; //  9223372036854775807

    private static Case[] testCases = {
        // case:     a       b       c         resSmaddl          resSmsubl          resSmnegl
        new Case(  1000,   -200, 500000L,           300000L,           700000L,           200000L),
        new Case(maxInt,      1,      1L, (long)maxInt + 1L, (long)minInt + 2L,  (long)minInt + 1L),
        new Case(minInt,     -1,      1L, (long)maxInt + 2L, (long)minInt + 1L,       (long)minInt),
        new Case(   -10, minInt,      1L,      21474836481L,     -21474836479L,      -21474836480L),
        new Case(     1,      1, maxLong,           minLong,      maxLong - 1L,                -1L),
        new Case(     1,     -1, minLong,           maxLong,      minLong + 1L,                 1L),
        new Case(    -1,     -1, 0xffffffffeL << 32, 0xfffffffe00000001L, 0xfffffffdffffffffL, -1L)
    };

    private static class Case {

        private int a;
        private int b;
        private long c;
        private long resSmaddl;
        private long resSmsubl;
        private long resSmnegl;

        public Case(int a, int b, long c, long resSmaddl, long resSmsubl, long resSmnegl) {
            this.a = a;
            this.b = b;
            this.c = c;
            this.resSmaddl = resSmaddl;
            this.resSmsubl = resSmsubl;
            this.resSmnegl = resSmnegl;
        }

        public void verify() {
            Asserts.assertEQ(smaddl(a, b, c), resSmaddl,
                "Unexpected result from signed multiply-add long.");
            Asserts.assertEQ(smsubl(a, b, c), resSmsubl,
                "Unexpected result from signed multiply-sub long.");
            Asserts.assertEQ(smnegl(a, b), resSmnegl,
                "Unexpected result from signed multiply-neg long.");
        }
    }

    private static long smaddl(int a, int b, long c) {
        return c + a * (long) b;
    }

    private static long smsubl(int a, int b, long c) {
        return c - a * (long) b;
    }

    private static long smnegl(int a, int b) {
        return a * (-(long) b);
    }

    public static void main(String[] args) {
        for (Case c : testCases) {
            c.verify();
        }
    }
}
