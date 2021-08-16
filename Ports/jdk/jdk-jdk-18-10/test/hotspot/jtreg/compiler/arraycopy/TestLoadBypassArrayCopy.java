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
 * @bug 8086046
 * @summary load bypasses arraycopy that sets the value after the ArrayCopyNode is expanded
 *
 * @run main/othervm -XX:-BackgroundCompilation  -XX:-UseOnStackReplacement
 *                   -XX:CompileCommand=dontinline,compiler.arraycopy.TestLoadBypassArrayCopy::test_helper
 *                   -XX:-TieredCompilation
 *                   compiler.arraycopy.TestLoadBypassArrayCopy
 */

package compiler.arraycopy;

public class TestLoadBypassArrayCopy {

    static long i;
    static boolean test_helper() {
        i++;
        if ((i%10) == 0) {
            return false;
        }
        return true;
    }

    static int test(int[] src, int len, boolean flag) {
        int[] dest = new int[10];
        int res = 0;
        while (test_helper()) {
            System.arraycopy(src, 0, dest, 0, len);
            // predicate moved out of loop so control of following
            // load is not the ArrayCopyNode. Otherwise, if the memory
            // of the load is changed and the control is set to the
            // ArrayCopyNode the graph is unschedulable and the test
            // doesn't fail.
            if (flag) {
            }
            // The memory of this load shouldn't bypass the arraycopy
            res = dest[0];
        }
        return res;
    }

    static public void main(String[] args) {
        int[] src = new int[10];
        src[0] = 0x42;
        for (int i = 0; i < 20000; i++) {
            int res = test(src, 10, false);
            if (res != src[0]) {
                throw new RuntimeException("test failed");
            }
        }
    }
}
