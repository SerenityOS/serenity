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

/*
 * @test
 * @bug 8256385
 * @requires vm.debug == true & vm.flavor == "server"
 * @summary Test for dead nodes that are not added to the IGVN worklist for removal.
 * @run main/othervm -Xbatch compiler.c2.TestDeadNodeDuringIGVN
 */
package compiler.c2;

public class TestDeadNodeDuringIGVN {
    static int res;

    static void test(int len) {
        int array[] = new int[len];
        for (long l = 0; l < 10; l++) {
            float e = 1;
            do { } while (++e < 2);
            res += l;
        }
    }

    public static void main(String[] args) {
        for (int i = 0; i < 40_000; ++i) {
            res = 0;
            test(1);
            if (res != 45) {
                throw new RuntimeException("Test failed: res = " + res);
            }
        }
    }
}
