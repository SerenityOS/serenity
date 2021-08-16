/*
 * Copyright (c) 2019, Red Hat, Inc. All rights reserved.
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
 * @bug 8134739 8010500
 * @summary SEGV in SuperWord::get_pre_loop_end
 * @run main/othervm compiler.loopopts.superword.TestFuzzPreLoop
 */

package compiler.loopopts.superword;

public class TestFuzzPreLoop {
    static Object sink;
    short sFld = -19206;

    void doTest() {
        int[] arr = new int[400];

        for (int i1 = 0; i1 < 200; i1++) {
            for (int i2 = 0; i2 < 100; i2++) {
                sink = new int[400];
            }
            arr[i1] = 0;
        }

        float f1 = 0;
        for (int i3 = 0; i3 < 200; i3++) {
            f1 += i3 * i3;
        }
        for (int i4 = 0; i4 < 200; i4++) {
            f1 += i4 - sFld;
        }

        System.out.println(arr);
        System.out.println(f1);
    }

    public static void main(String... args) throws Exception {
        TestFuzzPreLoop test = new TestFuzzPreLoop();
        for (int i = 0; i < 100; i++) {
            test.doTest();
        }
    }
}
