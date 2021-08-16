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
 * @bug 8240576
 * @summary JVM crashes after transformation in C2 IdealLoopTree::merge_many_backedges
 *
 * @run main/othervm -XX:-TieredCompilation -XX:-BackgroundCompilation
 *      compiler.loopopts.TestBeautifyLoops
 */

package compiler.loopopts;

public class TestBeautifyLoops {
    private int mI = 0;
    private long mJ = 0;
    private float mF = 0f;

    public void testMethod() {
        for (int i0 = 0; i0 < 100; i0++) {
            if (mF != 0) {
                // do nothing
            } else {
                try {
                    mJ = Long.MAX_VALUE;
                    for (int i1 = 0; i1 < 101; i1++) {
                        for (int i2 = 0; i2 < 102; i2++) {
                            mI = new Integer(0x1234);
                        }
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    public static void main(String[] args) {
        TestBeautifyLoops obj = new TestBeautifyLoops();
        obj.testMethod();
    }
}
