/*
 * Copyright 2009 Google Inc.  All Rights Reserved.
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

/**
 * @test
 * @bug 6860469
 * @summary remix_address_expressions reshapes address expression with bad control
 *
 * @run main/othervm -Xcomp
 *      -XX:CompileCommand=compileonly,compiler.loopopts.Test6860469::C
 *      compiler.loopopts.Test6860469
 */

package compiler.loopopts;

public class Test6860469 {

    private static final int H = 16;
    private static final int F = 9;

    static int[] fl = new int[1 << F];

    static int C(int ll, int f) {
        int max = -1;
        int min = H + 1;

        if (ll != 0) {
            if (ll < min) {
                min = ll;
            }
            if (ll > max) {
                max = ll;
            }
        }

        if (f > max) {
            f = max;
        }
        if (min > f) {
            min = f;
        }

        for (int mc = 1 >> max - f; mc <= 0; mc++) {
            int i = mc << (32 - f);
            fl[i] = max;
        }

        return min;
    }

    public static void main(String argv[]) {
        C(0, 10);
    }
}
