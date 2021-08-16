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
 * @bug 8202952
 * @summary C2: Unexpected dead nodes after matching
 *
 * @run main/othervm -XX:-TieredCompilation -Xcomp -XX:CompileOnly=::test
 *      compiler.c2.TestMatcherLargeOffset
 */
package compiler.c2;

public class TestMatcherLargeOffset {
    public static final int N = 400;
    public static int iArrFld[] = new int[N];

    public static void m(int i4) {
        i4 |= -104;
        iArrFld[(i4 >>> 1) % N] >>= i4;
    }

    public static void test() {
        int i2 = 1, i24 = 65;
        for (int i1 = 7; i1 < 384; ++i1) {
            for (long l = 2; l < 67; l++) {
                m(i2);
                for (i24 = 1; 2 > i24; ++i24) {
                    iArrFld = iArrFld;
                }
            }
            i2 = (-229 / i24);
        }
    }
    public static void main(String[] strArr) {
        for (int i = 0; i < 5; i++ ) {
            test();
        }
    }
}
