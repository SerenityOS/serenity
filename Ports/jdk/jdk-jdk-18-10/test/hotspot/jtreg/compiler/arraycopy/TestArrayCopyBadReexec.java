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
 * @bug 8073866
 * @summary Fix for 8064703 may also cause stores between the allocation and arraycopy to be rexecuted after a deoptimization
 *
 * @run main/othervm -XX:-BackgroundCompilation -XX:-UseOnStackReplacement
 *                   compiler.arraycopy.TestArrayCopyBadReexec
 */

package compiler.arraycopy;

public class TestArrayCopyBadReexec {

    static int val;

    static int[] m1(int[] src, int l) {
        if (src == null) {
            return null;
        }
        int[] dest = new int[10];
        val++;
        try {
            System.arraycopy(src, 0, dest, 0, l);
        } catch (IndexOutOfBoundsException npe) {
        }
        return dest;
    }

    static public void main(String[] args) {
        int[] src = new int[10];
        int[] res = null;
        boolean success = true;

        for (int i = 0; i < 20000; i++) {
            m1(src, 10);
        }

        int val_before = val;

        m1(src, -1);

        if (val - val_before != 1) {
            System.out.println("Bad increment: " + (val - val_before));
            throw new RuntimeException("Test failed");
        }
    }
}
