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
 * @bug 8244407
 * @summary JVM crashes after transformation in C2 IdealLoopTree::split_fall_in
 *
 * @run main/othervm -Xcomp -XX:-BackgroundCompilation
 *      -XX:CompileCommand=compileonly,compiler.loopopts.TestBeautifyLoops_2::testMethod
 *      compiler.loopopts.TestBeautifyLoops_2
 */

package compiler.loopopts;

public class TestBeautifyLoops_2 {

    private class X {
        public int x() {
            return -1;
        }
    }

    private int mI = 0;
    private float mF = 0;
    private boolean mZ = false;
    private X mX = new X();
    private long[] mArray = new long[331];

    private void testMethod() {
        double d = 0;

        for (int i = 0; i < 331; i++) {
            if (mZ) {
                continue;
            }

            try {
                for (int j = mArray.length - 1; j >= 0; j--) {
                    for (int k = 0; k < 331; k++) {
                        d += ((double) new Double(d));
                        switch (267) {
                            case 256:
                            default: {
                                mF += (mX.x());
                                break;
                            }
                        }
                    }
                }
            } catch (Exception ignore) {
            }
        }
    }

    public static void main(String[] args) {
        TestBeautifyLoops_2 obj = new TestBeautifyLoops_2();
        obj.testMethod();
    }

}
