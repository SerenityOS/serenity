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
 * @bug 8229694
 * @summary Tests the case where there is only 1 pack and no operations left when calling SuperWord::find_align_to_ref() to find the best alignment again.
 *
 * @run main/othervm -Xbatch -XX:CompileCommand=compileonly,compiler.loopopts.superword.AlignmentOnePack::test
 *      compiler.loopopts.superword.AlignmentOnePack
 */

package compiler.loopopts.superword;

public class AlignmentOnePack {
    static int iFld;

    public static void test(int[] intArr, short[] shortArr) {
        for (int j = 8; j < intArr.length;j++) {
            shortArr[10] = 10;
            shortArr[j] = 30;
            intArr[7] = 260;
            intArr[j-1] = 400;
            iFld = intArr[j];
        }
    }

    public static void main(String[] args) throws Exception {
        int[] a = new int[16];
        short[] c = new short[16];

        for (int i = 0; i < 10000; i++) {
            test(a, c);
        }
    }
}
