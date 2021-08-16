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
 * @bug 8080976
 * @summary Loop variant use in reduction should prevent vectorization
 * @run main/othervm -XX:-BackgroundCompilation -XX:-UseOnStackReplacement
 *      compiler.loopopts.superword.TestReductionWithLoopVariantUse
 */

package compiler.loopopts.superword;

public class TestReductionWithLoopVariantUse {
    static int m(int[] array) {
        int c = 0;
        for (int i = 0; i < 256; i++) {
            c += array[i];
            array[i] = c;
        }
        return c;
    }

    static public void main(String[] args) {
        int[] array = new int[256];
        int[] array2 = new int[256];
        for (int j = 0; j < 256; j++) {
            array2[j] = j;
        }
        for (int i = 0; i < 20000; i++) {
            System.arraycopy(array2, 0, array, 0, 256);
            int res = m(array);
            boolean success = true;
            int c = 0;
            for (int j = 0; j < 256; j++) {
                c += array2[j];
                if (array[j] != c) {
                    System.out.println("Failed for " + j + " : " + array[j] + " != " + c);
                    success = false;
                }
            }
            if (c != res) {
                System.out.println("Failed for sum: " + c + " != " + res);
            }
            if (!success) {
                throw new RuntimeException("Test failed");
            }
        }
    }
}
