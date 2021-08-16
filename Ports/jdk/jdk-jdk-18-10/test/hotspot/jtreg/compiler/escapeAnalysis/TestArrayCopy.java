/*
 * Copyright (c) 2016 SAP SE and/or its affiliates. All rights reserved.
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
 * @bug 8159611
 * @summary The elimination of System.arraycopy by EscapeAnalysis prevents
 *          an IndexOutOfBoundsException from being thrown if the arraycopy
 *          is called with a negative length argument.
 * @modules java.base/jdk.internal.misc
 * @library /testlibrary /test/lib
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 *
 * @run main/othervm/timeout=300
 *        -Xbootclasspath/a:.
 *        -XX:+UnlockDiagnosticVMOptions
 *        -XX:+WhiteBoxAPI
 *        -XX:-UseOnStackReplacement
 *        compiler.escapeAnalysis.TestArrayCopy
 *
 * @author Volker Simonis
 */

package compiler.escapeAnalysis;

import sun.hotspot.WhiteBox;
import java.lang.reflect.Method;

public class TestArrayCopy {

    private static final WhiteBox WB = WhiteBox.getWhiteBox();
    // DST_LEN Must be const, otherwise EliminateAllocations won't work
    static final int DST_LEN = 4;
    static final int SRC_LEN = 8;

    public static boolean do_test1(Object src, int src_pos, int dst_pos, int cpy_len) {
        try {
            System.arraycopy(src, src_pos, new Object[DST_LEN], dst_pos, cpy_len);
            return false;
        } catch (IndexOutOfBoundsException e) {
            return true;
        }
    }

    public static int do_test2(Object src, int src_pos, int dst_pos, int cpy_len) {
        try {
            System.arraycopy(src, src_pos, new Object[DST_LEN], dst_pos, cpy_len);
            return 0;
        } catch (IndexOutOfBoundsException e) {
            return 1;
        } catch (ArrayStoreException e) {
            return 2;
        }
    }

    static final int COUNT = 100_000;
    static final int[] src_pos = { 0, -1, -1,  0,  0,  0,  1,  1,  1,  1, 1 };
    static final int[] dst_pos = { 0, -1,  0, -1,  0,  1,  0,  1,  1,  1, 1 };
    static final int[] cpy_len = { 0,  0,  0,  0, -1, -1, -1, -1,  8,  4, 2 };

    public static void main(String args[]) throws Exception {
        int length = args.length > 0 ? Integer.parseInt(args[0]) : -1;
        int[] int_arr = new int[SRC_LEN];
        Object[] obj_arr = new Object[SRC_LEN];

        Method test1 = TestArrayCopy.class.getMethod("do_test1", Object.class, int.class, int.class, int.class);
        Method test2 = TestArrayCopy.class.getMethod("do_test2", Object.class, int.class, int.class, int.class);

        for (int i = 0; i < src_pos.length; i++) {
            int sp = src_pos[i];
            int dp = dst_pos[i];
            int cl = cpy_len[i];
            String version1 = String.format("System.arraycopy(Object[8], %d, new Object[%d], %d, %d)", sp, DST_LEN, dp, cl);
            String version2 = String.format("System.arraycopy(int[8], %d, new Object[%d], %d, %d)", sp, DST_LEN, dp, cl);
            System.out.format("Testing " + version1 + "\nand " + version2).flush();
            for (int x = 0; x < COUNT; x++) {
                if (!do_test1(obj_arr, sp, dp, cl) &&
                    (sp < 0 || dp < 0 || cl < 0 || (sp + cl >= SRC_LEN) || (dp + cl >= DST_LEN))) {
                    throw new RuntimeException("Expected IndexOutOfBoundsException for " + version1);
                }
                int res = do_test2(int_arr, sp, dp, cl);
                if (res == 0 || res == 1) {
                    throw new RuntimeException("Expected ArrayStoreException for " + version2);
                }
            }
            WB.deoptimizeMethod(test1);
            WB.clearMethodState(test1);
            WB.deoptimizeMethod(test2);
            WB.clearMethodState(test2);
        }

    }
}
