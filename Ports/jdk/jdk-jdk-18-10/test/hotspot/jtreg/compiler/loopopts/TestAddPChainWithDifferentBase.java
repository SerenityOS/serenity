/*
 * Copyright (c) 2021, Red Hat, Inc. All rights reserved.
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
 * @bug 8267988
 * @summary C2: assert(!addp->is_AddP() || addp->in(AddPNode::Base)->is_top() || addp->in(AddPNode::Base) == n->in(AddPNode::Base)) failed: Base pointers must match (addp 1301)
 *
 * @run main/othervm -Xcomp -XX:CompileOnly=TestAddPChainWithDifferentBase TestAddPChainWithDifferentBase
 *
 */

public class TestAddPChainWithDifferentBase {
    static int x;
    static int iArrFld[] = new int[400];

    public static void main(String[] strArr) {
        test();
    }

    static void test() {
        int i6, i7 = 9, i8, i9 = 138;

        for (i6 = 7; i6 > 1; i6 -= 3) {
            for (i8 = i6; i8 < 4; i8++) {
                try {
                    iArrFld[i8] = (52691 / i8);
                    i7 = (iArrFld[i8 + 1] % i9);
                    i7 = (412419036 / iArrFld[i8]);
                } catch (ArithmeticException a_e) {
                }
                i9 += 13;
            }
        }
        x = i7;
    }
}
