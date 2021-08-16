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
 * @bug 7048332
 * @summary Cadd_cmpLTMask doesn't handle 64-bit tmp register properly
 *
 * @run main/othervm -Xbatch compiler.c2.Test7048332
 */

package compiler.c2;

public class Test7048332 {

    static int capacity = 2;
    static int first = 1;
    static int last = 2;

    static int test(int i1, int i2, int i3, int i4, int i5, int i6) {
        final int result;
        if (last >= first) {
            result = last - first;
        } else {
            result = last - first + capacity;
        }
        return result;
    }

    public static void main(String[] args) {
        for (int i = 0; i < 11000; i++) {
            last = (i & 1) << 1; // 0 or 2
            int k = test(1, 2, 3, 4, 5, 6);
            if (k != 1) {
                System.out.println("FAILED: " + k + " != 1");
                System.exit(97);
            }
        }
    }
}
