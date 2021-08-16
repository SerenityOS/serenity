/*
 * Copyright (c) 2020, 2021, Huawei Technologies Co., Ltd. All rights reserved.
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
 * @bug 8243670
 * @summary Unexpected test result caused by C2 MergeMemNode::Ideal
 * @requires vm.compiler2.enabled
 *
 * @run main/othervm -Xcomp -XX:-SplitIfBlocks
 *      -XX:CompileOnly=compiler.c2.TestReplaceEquivPhis::test
 *      -XX:-BackgroundCompilation compiler.c2.TestReplaceEquivPhis
 */

package compiler.c2;

public class TestReplaceEquivPhis {

    public static final int N = 400;
    public static volatile int instanceCount = 0;
    public int iFld = 0;
    public static int iArrFld[] = new int[N];

    public int test() {
        int v = 0;
        boolean bArr[] = new boolean[N];

        for (int i = 1; i < 344; i++) {
            iFld = i;
            for (int j = 2; j <177 ; j++) {
                v = iFld;
                iFld = TestReplaceEquivPhis.instanceCount;
                TestReplaceEquivPhis.iArrFld[i] = 0;
                iFld += TestReplaceEquivPhis.instanceCount;
                TestReplaceEquivPhis.iArrFld[i] = 0;
                bArr[j] = false;
                TestReplaceEquivPhis.instanceCount = 1;

                for (int k = 1; k < 3; k++) {
                    // do nothing
                }
            }
        }
        return v;
    }

    public static void main(String[] args) {
            TestReplaceEquivPhis obj = new TestReplaceEquivPhis();
            for (int i = 0; i < 5; i++) {
                int result = obj.test();
                if (result != 2) {
                    throw new RuntimeException("Test failed.");
                }
            }
            System.out.println("Test passed.");
    }

}
