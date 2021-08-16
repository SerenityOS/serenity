/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8136500
 * @summary Test Long.toString method
 */

public class ToString {

    public static void main(String[] args) throws Exception {
        test("-9223372036854775808", Long.MIN_VALUE);
        test("9223372036854775807",  Long.MAX_VALUE);
        test("0", 0);

        // Wiggle around the exponentially increasing base.
        final int LIMIT = (1 << 15);
        long base = 10000;
        while (base < Long.MAX_VALUE / 10) {
            for (int d = -LIMIT; d < LIMIT; d++) {
                long c = base + d;
                if (c > 0) {
                    buildAndTest(c);
                }
            }
            base *= 10;
        }

        for (int c = 1; c < LIMIT; c++) {
            buildAndTest(Long.MAX_VALUE - LIMIT + c);
        }
    }

    private static void buildAndTest(long c) {
        if (c <= 0) {
            throw new IllegalArgumentException("Test bug: can only handle positives, " + c);
        }

        StringBuilder sbN = new StringBuilder();
        StringBuilder sbP = new StringBuilder();

        long t = c;
        while (t > 0) {
            char digit = (char) ('0' + (t % 10));
            sbN.append(digit);
            sbP.append(digit);
            t = t / 10;
        }

        sbN.append("-");
        sbN.reverse();
        sbP.reverse();

        test(sbN.toString(), -c);
        test(sbP.toString(), c);
    }

    private static void test(String expected, long value) {
        String actual = Long.toString(value);
        if (!expected.equals(actual)) {
            throw new RuntimeException("Expected " + expected + ", but got " + actual);
        }
    }
}
