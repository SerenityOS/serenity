/*
 * Copyright (c) 2021, Alibaba Group Holding Limited. All Rights Reserved.
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
 *
 */

/*
 * @test
 * @bug 8267239
 * @author Yi Yang
 * @summary apply RCE for % operations
 * @requires vm.compiler1.enabled
 * @library /test/lib
 * @run main/othervm -XX:TieredStopAtLevel=1 -XX:+TieredCompilation
 *                   -XX:CompileCommand=compileonly,*ArithmeticRemRCE.test*
 *                   compiler.c1.ArithmeticRemRCE
 */

package compiler.c1;

import jdk.test.lib.Asserts;

public class ArithmeticRemRCE {
    static int field = 1000;

    static void test1() {
        // seq should be loop invariant, so we can not put it into static fields
        int[] seq = new int[1000];
        for (int i = 0; i < seq.length; i++) {
            seq[i] = i;
        }

        for (int i = 0; i < 1024; i++) {
            int constVal = 10;
            Asserts.assertTrue(0 <= seq[i % 5] && seq[i % 5] <= 4);
            Asserts.assertTrue(0 <= seq[i % -5] && seq[i % -5] <= 4);

            Asserts.assertTrue(0 <= seq[i % constVal] && seq[i % constVal] <= 9);
            Asserts.assertTrue(0 <= seq[i % -constVal] && seq[i % -constVal] <= 9);

            Asserts.assertTrue(seq[i % 1] == 0);

            // will not apply RCE
            Asserts.assertTrue(0 <= seq[i % field] && seq[i % field] <= 999);
            Asserts.assertTrue(0 <= seq[i % -field] && seq[i % -field] <= 999);
        }
    }

    public static void main(String... args) throws Exception {
        for (int i = 0; i < 10_000; i++) {
            test1();
        }
    }
}
