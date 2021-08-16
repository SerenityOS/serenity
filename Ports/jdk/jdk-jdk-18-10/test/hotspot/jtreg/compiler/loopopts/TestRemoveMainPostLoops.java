/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8233529
 * @summary Verify that correct loops are selected when trying to remove main/post.
 * @run main/othervm -XX:-TieredCompilation -Xbatch
 *                   -XX:CompileCommand=compileonly,compiler.loopopts.TestRemoveMainPostLoops::test
 *                   compiler.loopopts.TestRemoveMainPostLoops
 */

package compiler.loopopts;

public class TestRemoveMainPostLoops {
    static int cnt1 = 0;
    int cnt2 = 0;

    void testCallee() {
        // (5) Only main and post loops are created (no pre loop -> "PeelMainPost") and main is unrolled.
        for (int i = 0; i < 100; ++i) {
            // (4) Inner loop is fully unrolled and removed.
            for (int j = 0; j < 10; ++j) {
                cnt1 += j;
            }
        }
    }

    void test() {
        for (int i = 0; i < 10_000; ++i) {
            // (0) testCallee method is inlined
            testCallee();
            cnt2 = 0;
            // (1) OSR compilation is triggered in this loop.
            // (2) Pre-/main-/post loops are created.
            // (3) Main and post loops found empty and removed.
            // (6) Pre loop is found empty, attempt to remove main and post loop then incorrectly selects main from (5).
            for (int j = 0; j < 10; ++j) {
                cnt2 = cnt1 + j;
            }
        }
    }

    public static void main(String[] strArr) {
        TestRemoveMainPostLoops test = new TestRemoveMainPostLoops();
        for (int i = 0; i < 100; i++) {
            cnt1 = 0;
            test.cnt2 = 0;
            test.test();
            if (cnt1 != 45000000 || test.cnt2 != 45000009) {
                throw new RuntimeException("Incorrect result: " + cnt1 + " " + test.cnt2);
            }
        }
    }
}
