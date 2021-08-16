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
 * @bug 8080699
 * @summary eliminated arraycopy node still reachable through exception edges
 *
 * @run main/othervm -XX:-UseOnStackReplacement -XX:-BackgroundCompilation
 *                   compiler.arraycopy.TestDeadArrayCopyOnMemChain
 */

package compiler.arraycopy;

public class TestDeadArrayCopyOnMemChain {
    static class A {
        int f;
    }

    static void test_helper(Object o) {
    }

    static void test(int src_off, boolean flag) {
        // dst is eliminated first. Eliminating dst causes src to be
        // eliminated. When working on the safepoint at the uncommon
        // trap in the exception handler, the eliminated ArrayCopyNode
        // is reached through the exception edges.
        Object[] dst = new Object[10];
        Object[] src = new Object[10];

        // src_off causes the exception handler to be run sometimes
        try {
            System.arraycopy(src, src_off, dst, 0, 10);
        } catch (IndexOutOfBoundsException ioobe) {
            // flag always false so test becomes uncommon trap. Make
            // sure src is live at the unc.
            if (flag) {
                test_helper(src);
            }
        }
    }

    static public void main(String[] args) {
        for (int i = 0; i < 20000; i++) {
            test((i%2) == 0 ? 0 : -1, false);
        }
    }
}
