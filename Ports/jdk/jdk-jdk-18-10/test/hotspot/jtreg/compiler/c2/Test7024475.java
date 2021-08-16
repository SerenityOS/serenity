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
 * @bug 7024475
 * @summary loop doesn't terminate when compiled
 *
 * @run main compiler.c2.Test7024475
 */

package compiler.c2;

public class Test7024475 {

    static int i;
    static int x1;
    static int[] bucket_B;

    static void test(Test7024475 test, int i, int c0, int j, int c1) {
        for (;;) {
            if (c1 > c0) {
                if (c0 > 253) {
                    throw new InternalError("c0 = " + c0);
                }
                int index = c0 * 256 + c1;
                if (index == -1) return;
                i = bucket_B[index];
                if (1 < j - i && test != null)
                    x1 = 0;
                j = i;
                c1--;
            } else {
                c0--;
                if (j <= 0)
                    break;
                c1 = 255;
            }
        }
    }

    public static void main(String args[]) {
        Test7024475 t = new Test7024475();
        bucket_B = new int[256*256];
        for (int i = 1; i < 256*256; i++) {
            bucket_B[i] = 1;
        }
        for (int n = 0; n < 100000; n++) {
            test(t, 2, 85, 1, 134);
        }
    }
}
