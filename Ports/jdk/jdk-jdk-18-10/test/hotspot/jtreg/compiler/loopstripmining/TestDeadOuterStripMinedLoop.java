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

/*
 * @test
 * @bug 8235452
 * @library /test/lib /
 * @summary Test loop strip mining verification with dying outer loop.
 * @run main/othervm -Xbatch -XX:-TieredCompilation
 *                   compiler.loopstripmining.TestDeadOuterStripMinedLoop
 */

package compiler.loopstripmining;

import jdk.test.lib.Asserts;

public class TestDeadOuterStripMinedLoop {

    public static int test(boolean b) {
        int res = 5000;
        for (int i = 0; i < 42; i++) {
            if (!b) {
                break;
            }
            // Strip mined loop
            while (--res > 0) {
                if (res < 1) {
                    throw new RuntimeException("Should not reach here!");
                }
            }

            /*
            After parsing:
                Loop:
                    res--;
                    if (res <= 0) {
                        goto End;
                    }
                    if (res < 1) {  <- Treated as loop exit check (res <= 0)
                        UncommonTrap();
                    }
                goto Loop;
                End:

            Loop opts convert this into:
                // Outer strip mined loop
                do {
                    // Strip mined loop
                    do {
                        res--;
                        if (res <= 0) {
                            goto End;
                        }
                    } while (res > 0);  <- Always false due to above res <= 0 check
                    Safepoint()
                } while (false);
                UncommonTrap();
                End:

            The safepoint path dies, because the exit condition of the strip mined loop is always false:
                // Strip mined loop
                do {
                    res--;
                    if (res <= 0) {
                        goto End;
                    }
                } while (true);
                End:
            */
        }
        return res;
    }

    public static void main(String[] args) {
        for (int i = 0; i < 100; i++) {
            Asserts.assertEQ(test(true), -41);
        }
        Asserts.assertEQ(test(false), 5000);
    }
}
