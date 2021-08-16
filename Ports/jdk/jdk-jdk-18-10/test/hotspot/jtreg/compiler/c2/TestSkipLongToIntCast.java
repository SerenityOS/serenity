/*
 * Copyright (c) BELLSOFT. All rights reserved.
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

package compiler.c2;

/*
 * @test
 * @bug 8248043
 * @summary Functional test to enshure CmpL(int value, long constant) is not broken
 *
 * @run main/othervm -XX:-TieredCompilation -XX:CompileOnly=compiler.c2.TestSkipLongToIntCast::cmplTest
 *      compiler.c2.TestSkipLongToIntCast
 */
public class TestSkipLongToIntCast {

    public static int[] pos = {0, 1, 2, 3};
    public static int[] neg = {0, -1, -2, -3};
    public static int[] max = {2147483647, 2147483646, 2147483645, 2147483644};
    public static int[] min = {-2147483648, -2147483647, -2147483646, -2147483645};
    public static int[] out = {(int)2147483648L, (int)-2147483649L, (int)Long.MAX_VALUE, (int)Long.MIN_VALUE};

    // Testing cmp(int value, long constant) expressions as they are updated
    // on Ideal graph level: i2l conversion is skipped when possible.
    public static void cmplTest() throws Exception {
        // values around zero
        if (pos[0] != 0L) { throw new Exception("pos[0] is " + pos[0]); }
        if (pos[1] != 1L) { throw new Exception("pos[1] is " + pos[1]); }
        if (pos[2] != 2L) { throw new Exception("pos[2] is " + pos[2]); }
        if (pos[3] != 3L) { throw new Exception("pos[3] is " + pos[3]); }

        if (neg[0] != -0L) { throw new Exception("neg[0] is " + neg[0]); }
        if (neg[1] != -1L) { throw new Exception("neg[1] is " + neg[1]); }
        if (neg[2] != -2L) { throw new Exception("neg[2] is " + neg[2]); }
        if (neg[3] != -3L) { throw new Exception("neg[3] is " + neg[3]); }

        // values near the ends of Integer range
        if (max[0] != 2147483647L) { throw new Exception("max[0] is " + max[0]); }
        if (max[1] != 2147483646L) { throw new Exception("max[1] is " + max[1]); }
        if (max[2] != 2147483645L) { throw new Exception("max[2] is " + max[2]); }
        if (max[3] != 2147483644L) { throw new Exception("max[3] is " + max[3]); }

        if (min[0] != -2147483648L) { throw new Exception("min[0] is " + min[0]); }
        if (min[1] != -2147483647L) { throw new Exception("min[1] is " + min[1]); }
        if (min[2] != -2147483646L) { throw new Exception("min[2] is " + min[2]); }
        if (min[3] != -2147483645L) { throw new Exception("min[3] is " + min[3]); }

        // constants outside of the Integer range
        if (out[0] == 2147483648L)  { throw new Exception("out[0] is " + out[0]); }
        if (out[1] == -2147483649L) { throw new Exception("out[1] is " + out[1]); }
        if (out[2] == Long.MAX_VALUE) { throw new Exception("out[2] is " + out[2]); }
        if (out[3] == Long.MIN_VALUE) { throw new Exception("out[3] is " + out[3]); }
    }

    // similar test with long constant on LHS
    public static void cmplTest_LHS() throws Exception {
        // values around zero
        if (0L != pos[0]) { throw new Exception("LHS: pos[0] is " + pos[0]); }
        if (1L != pos[1]) { throw new Exception("LHS: pos[1] is " + pos[1]); }
        if (2L != pos[2]) { throw new Exception("LHS: pos[2] is " + pos[2]); }
        if (3L != pos[3]) { throw new Exception("LHS: pos[3] is " + pos[3]); }

        if (-0L != neg[0]) { throw new Exception("LHS: neg[0] is " + neg[0]); }
        if (-1L != neg[1]) { throw new Exception("LHS: neg[1] is " + neg[1]); }
        if (-2L != neg[2]) { throw new Exception("LHS: neg[2] is " + neg[2]); }
        if (-3L != neg[3]) { throw new Exception("LHS: neg[3] is " + neg[3]); }

        // values near the ends of Integer range
        if (2147483647L != max[0]) { throw new Exception("LHS: max[0] is " + max[0]); }
        if (2147483646L != max[1]) { throw new Exception("LHS: max[1] is " + max[1]); }
        if (2147483645L != max[2]) { throw new Exception("LHS: max[2] is " + max[2]); }
        if (2147483644L != max[3]) { throw new Exception("LHS: max[3] is " + max[3]); }

        if (-2147483648L != min[0]) { throw new Exception("LHS: min[0] is " + min[0]); }
        if (-2147483647L != min[1]) { throw new Exception("LHS: min[1] is " + min[1]); }
        if (-2147483646L != min[2]) { throw new Exception("LHS: min[2] is " + min[2]); }
        if (-2147483645L != min[3]) { throw new Exception("LHS: min[3] is " + min[3]); }

        // constants outside of the Integer range
        if (2147483648L == out[0])  { throw new Exception("LHS: out[0] is " + out[0]); }
        if (-2147483649L == out[1]) { throw new Exception("LHS: out[1] is " + out[1]); }
        if (Long.MAX_VALUE == out[2]) { throw new Exception("LHS: out[2] is " + out[2]); }
        if (Long.MIN_VALUE == out[3]) { throw new Exception("LHS: out[3] is " + out[3]); }
    }

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 100_000; i++) {
            cmplTest();
            cmplTest_LHS();
        }
    }
}
