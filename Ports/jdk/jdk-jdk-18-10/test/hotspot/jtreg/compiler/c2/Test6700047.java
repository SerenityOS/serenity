/*
 * Copyright (c) 2008, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6700047
 * @summary C2 failed in idom_no_update
 *
 * @run main compiler.c2.Test6700047
 */

package compiler.c2;

public class Test6700047 {
    static byte[] dummy = new byte[256];

    public static void main(String[] args) {
        for (int i = 0; i < 100000; i++) {
            intToLeftPaddedAsciiBytes();
        }
    }

    public static int intToLeftPaddedAsciiBytes() {
        int offset = 40;
        int q;
        int r;
        int         i   = 100;
        int result = 1;
        while (offset > 0) {
            q = (i * 52429);
            r = i;
            offset--;
            i = q;
            if (i == 0) {
                break;
            }
        }
        if (offset > 0) {
            for(int j = 0; j < offset; j++) {
                result++;
                dummy[i] = 0;
            }
        }
        return result;
    }
}
