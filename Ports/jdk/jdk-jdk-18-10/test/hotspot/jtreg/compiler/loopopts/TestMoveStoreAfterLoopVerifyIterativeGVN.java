/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8238756
 * @requires vm.debug == true & vm.flavor == "server"
 * @summary Test which triggers assertion in PhaseIdealLoop::try_move_store_after_loop with -XX:+VerifyIterativeGVN due to dead hook.
 *
 * @run main/othervm -Xbatch -XX:+VerifyIterativeGVN compiler.loopopts.TestMoveStoreAfterLoopVerifyIterativeGVN
 */

package compiler.loopopts;

public class TestMoveStoreAfterLoopVerifyIterativeGVN {

    private static int[] iArr = new int[10];

    static void test() {
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j <= 10; j++) {
                iArr[i] = j;
            }
        }
    }

    static public void main(String[] args) {
        for (int i = 0; i < 1000; i++) {
            test();
        }
    }
}
