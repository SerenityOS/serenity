/*
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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
 * @bug 8268362
 * @requires vm.compiler2.enabled & vm.debug
 * @summary C2 using negative array length as index, using a.length.
 *          AllocateArrayNode::make_ideal_length create CastIINode to not negative range.
 *          Apply transform in GraphKit::load_array_length will covert array load index type to top.
 *          This cause assert in Parse::array_addressing, it expect index type is int.
 * @run main/othervm -XX:-PrintCompilation compiler.arraycopy.TestNegArrayLengthAsIndex1
 */

package compiler.arraycopy;
public class TestNegArrayLengthAsIndex1 {

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 10000; i++) {
            foo();
        }
    }

    static int foo() {
        int minusOne = -1;
        int[] a = null;
        try {
            a = new int[minusOne];
        } catch (NegativeArraySizeException e) {
           return 0;
        }
        return a[a.length - 1];
    }
}
