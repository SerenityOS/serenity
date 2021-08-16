/*
 * Copyright (c) 2020, Huawei Technologies Co., Ltd. All rights reserved.
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
 * @bug 8250609
 * @summary C2 crash in IfNode::fold_compares
 *
 * @run main/othervm -XX:CompileOnly=compiler.c2.TestFoldCompares::test
 *                   -XX:-BackgroundCompilation compiler.c2.TestFoldCompares
 */

package compiler.c2;

public class TestFoldCompares {

    public int test() {
        byte by = -37;
        int result = 1;
        int iArr[] = new int[6];

        for (int i = 0; i < iArr.length; i++) {
            iArr[i] = 0;
        }

        for (int i = 16; i < 308; i++) {
            result *= i;
            if ((result--) <= (++by)) {
                continue;
            }

            for (int j = 3; j < 86; j++) {
                for (int k = 1; k < 2; k++) {
                    result >>= 25;
                }

                for (int k = 1; k < 2; k += 3) {
                    try {
                        iArr[k] = (16986 / result);
                    } catch (ArithmeticException a_e) {
                    }
                    result = k;
                }
            }
        }

        return result;
    }

    public static void main(String[] args) {
        TestFoldCompares obj = new TestFoldCompares();
        for (int i = 0; i < 10; i++) {
            int result = obj.test();
            if (result != 1) {
                throw new RuntimeException("Test failed.");
            }
        }
        System.out.println("Test passed.");
    }

}
