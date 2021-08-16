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
 * @bug 8249603
 * @summary The C1 register allocator uses a register hint interval i as spill location for an interval j which
 *          overlaps with one of i's split children which has the same spill location which lets verification fail.
 *
 * @run main/othervm -Xcomp -XX:CompileCommand=compileonly,compiler.regalloc.TestC1OverlappingRegisterHint::*
 *                   compiler.regalloc.TestC1OverlappingRegisterHint
 */
package compiler.regalloc;

public class TestC1OverlappingRegisterHint {

    public static int iFldStatic = 10;
    public int iFld = 11;

    public int test() {
        int a = 1;
        int b = 2;
        int c = 3;
        int v = 4;
        int w = 5;
        int x = 6;
        int y = 7;
        int z = 8;
        int iArr[] = new int[400];

        double d = 1.5;

        int k = 0;
        for (a = 9; a < 283; a += 2) {
            for (int i = 8; i < 183; i++) {
            }
        }

        for (int i = 12; i < 283; i++) {
            iFldStatic += i;
            for (int j = 1; 93 > j; j += 2) {
                x += (j - z);
                c -= iFld;
                k = 3;
                while ((k -= 2) > 0) {
                }
                switch ((i % 8) + 52) {
                case 52:
                    iArr[8] = 5;
                    for (int i20 = 1; i20 < 3; ++i20) {
                        x *= (int)d;
                        w += 5;
                    }
                    break;
                case 53:
                case 55:
                    v *= iFldStatic;
                    break;
                case 56:
                case 57:
                    try {
                        iArr[5] = a;
                        v = (a / b);
                    } catch (ArithmeticException a_e) {}
                    break;
                default:
                    iFldStatic += iFldStatic;
                }
            }
        }
        return y + k;
    }

    public static void main(String[] strArr) {
        TestC1OverlappingRegisterHint _instance = new TestC1OverlappingRegisterHint();
        for (int i = 0; i < 10; i++) {
            _instance.test();
        }
    }
}

