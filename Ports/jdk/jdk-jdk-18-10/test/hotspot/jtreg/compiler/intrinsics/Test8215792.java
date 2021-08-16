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
 * @bug 8215792
 * @summary Fix a bug in AArch64 string intrinsics
 *
 * @run main/othervm compiler.intrinsics.Test8215792
 * @run main/othervm -XX:-CompactStrings compiler.intrinsics.Test8215792
 */

package compiler.intrinsics;

public class Test8215792 {

    private static final int ITERATIONS = 10000;
    private static final String pattern = "01234567890123456789";

    public static void main(String[] args) {

        // Repeat many times to trigger compilation
        for (int iter = 0; iter < ITERATIONS; iter++) {
            StringBuilder str1 = new StringBuilder("ABCDEFGHIJKLMNOPQRSTUVWXYZ01234567890123456789");
            StringBuilder str2 = new StringBuilder("\u4f60\u598dCDEFGHIJKLMNOPQRSTUVWXYZ01234567890123456789");

            for (int i = 0; i < 20; i++) {
                // Remove one character from the tail
                str1.setLength(str1.length() - 1);
                str2.setLength(str2.length() - 1);
                // Pattern string should not be found after characters removed from the tail
                if (str1.indexOf(pattern) != -1 || str2.indexOf(pattern) != -1) {
                    System.out.println("FAILED");
                    System.exit(1);
                }
            }
        }
        System.out.println("PASSED");
    }
}

