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
    @test
    @bug 8054307
    @summary test chars() and codePoints()
*/

import java.util.Arrays;
import java.util.Random;

public class Chars {

    public static void main(String[] args) {
        Random r = new Random();
        for (int i = 0; i < 10; i++) {
            int n = 100 + r.nextInt(100);
            char[] cc = new char[n];
            int[]  ccExp = new int[n];
            int[]  cpExp = new int[n];
            // latin1
            for (int j = 0; j < n; j++) {
                cc[j] = (char)(ccExp[j] = cpExp[j] = r.nextInt(0x80));
            }
            testChars(cc, ccExp);
            testCPs(cc, cpExp);

            // bmp without surrogates
            for (int j = 0; j < n; j++) {
                cc[j] = (char)(ccExp[j] = cpExp[j] = r.nextInt(0x8000));
            }
            testChars(cc, ccExp);
            testCPs(cc, cpExp);

            // bmp with surrogates
            int k = 0;
            for (int j = 0; j < n; j++) {
                if (j % 9 ==  5 && j + 1 < n) {
                    int cp = 0x10000 + r.nextInt(2000);
                    cpExp[k++] = cp;
                    Character.toChars(cp, cc, j);
                    ccExp[j] = cc[j];
                    ccExp[j + 1] = cc[j + 1];
                    j++;
                } else {
                    cc[j] = (char)(ccExp[j] = cpExp[k++] = r.nextInt(0x8000));
                }
            }
            cpExp = Arrays.copyOf(cpExp, k);
            testChars(cc, ccExp);
            testCPs(cc, cpExp);
        }
    }

    static void testChars(char[] cc, int[] expected) {
        String str = new String(cc);
        if (!Arrays.equals(expected, str.chars().toArray())) {
            throw new RuntimeException("chars/codePoints() failed!");
        }
    }

    static void testCPs(char[] cc, int[] expected) {
        String str = new String(cc);
        if (!Arrays.equals(expected, str.codePoints().toArray())) {
            throw new RuntimeException("chars/codePoints() failed!");
        }
    }
}
