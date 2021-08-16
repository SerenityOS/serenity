/*
 * Copyright (c) 2020, Red Hat, Inc. All rights reserved.
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
 * @key stress randomness
 * @bug 8241900
 * @summary Loop unswitching may cause dependence on null check to be lost
 *
 * @requires vm.compiler2.enabled
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:-TieredCompilation -XX:-BackgroundCompilation -XX:+StressGCM -XX:+StressLCM TestLoopUnswitchingLostCastDependency
 */

import java.util.Arrays;

public class TestLoopUnswitchingLostCastDependency {
    private static Object objectField;

    public static void main(String[] args) {
        Object[] array = new Object[100];
        Arrays.fill(array, new Object());
        for (int i = 0; i < 20_000; i++) {
            array[1] = null;
            test(array);
            array[1] = new Object();
            objectField = null;
            test(array);
            array[1] = new Object();
            objectField = new Object();
            test(array);
        }
    }

    private static void test(Object[] array) {
        Object o = objectField;
        Object o3 = array[1];
        int j = 0;
        for (int i = 1; i < 100; i *= 2) {
            Object o2 = array[i];
            // Both branches taken: loop is unswitched.
            if (o3 != null) {
                if (o2 == null) {
                }
                // Both branches taken: loop is unswitched next.
                if (o != null) {
                    // CastPP here becomes control dependent on o2 ==
                    // null check above.
                    if (o.getClass() == Object.class) {
                    }
                    // This causes partial peeling. When that happens,
                    // the o2 == null check becomes redundant with the
                    // o3 != null check in the peeled iteration. The
                    // CastPP with o as input that was control
                    // dependent on the o2 == null check becomes
                    // control dependent on the o3 != null check,
                    // above the o != null check.
                    if (j > 7) {

                    }
                    j++;
                }
            }
        }
    }
}
