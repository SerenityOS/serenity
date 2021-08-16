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

/**
 * @test
 * @bug 7020614
 * @summary "-server" mode optimizer makes code hang
 *
 * @run main/othervm/timeout=30 -Xbatch compiler.c2.Test7020614
 */

package compiler.c2;

public class Test7020614 {

    private static final int ITERATIONS = 1000;
    private static int doNotOptimizeOut = 0;

    public static long bitCountShort() {
        long t0 = System.currentTimeMillis();
        int sum = 0;
        for (int it = 0; it < ITERATIONS; ++it) {
            short value = 0;
            do {
                sum += Integer.bitCount(value);
            } while (++value != 0);
        }
        doNotOptimizeOut += sum;
        return System.currentTimeMillis() - t0;
    }

    public static void main(String[] args) {
        for (int i = 0; i < 4; ++i) {
            System.out.println((i + 1) + ": " + bitCountShort());
        }
        System.out.println("doNotOptimizeOut value: " + doNotOptimizeOut);
    }
}

