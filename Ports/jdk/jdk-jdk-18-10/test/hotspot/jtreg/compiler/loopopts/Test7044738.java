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
 * @bug 7044738
 * @summary Loop unroll optimization causes incorrect result
 *
 * @run main/othervm -Xbatch compiler.loopopts.Test7044738
 */

package compiler.loopopts;

public class Test7044738 {

    private static final int INITSIZE = 10000;
    public int d[] = {1, 2, 3, 4};
    public int i, size;

    private static int iter = 5;

    boolean done() {
        return (--iter > 0);
    }

    public static void main(String args[]) {
        Test7044738 t = new Test7044738();
        t.test();
    }

    int test() {

        while (done()) {
            size = INITSIZE;

            for (i = 0; i < size; i++) {
                d[0] = d[1]; // 2
                d[1] = d[2]; // 3
                d[2] = d[3]; // 4
                d[3] = d[0]; // 2

                d[0] = d[1]; // 3
                d[1] = d[2]; // 4
                d[2] = d[3]; // 2
                d[3] = d[0]; // 3

                d[0] = d[1]; // 4
                d[1] = d[2]; // 2
                d[2] = d[3]; // 3
                d[3] = d[0]; // 4

                d[0] = d[1]; // 2
                d[1] = d[2]; // 3
                d[2] = d[3]; // 4
                d[3] = d[0]; // 2

                d[0] = d[1]; // 3
                d[1] = d[2]; // 4
                d[2] = d[3]; // 2
                d[3] = d[0]; // 3

                d[0] = d[1]; // 4
                d[1] = d[2]; // 2
                d[2] = d[3]; // 3
                d[3] = d[0]; // 4

                d[0] = d[1]; // 2
                d[1] = d[2]; // 3
                d[2] = d[3]; // 4
                d[3] = d[0]; // 2

                d[0] = d[1]; // 3
                d[1] = d[2]; // 4
                d[2] = d[3]; // 2
                d[3] = d[0]; // 3
            }

            // try to defeat dead code elimination
            if (d[0] == d[1]) {
                System.out.println("test failed: iter=" + iter + "  i=" + i + " d[] = { " + d[0] + ", " + d[1] + ", " + d[2] + ", " + d[3] + " } ");
                System.exit(97);
            }
        }
        return d[3];
    }
}
