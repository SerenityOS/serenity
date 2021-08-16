/*
 * Copyright (c) 2016, 2018, Red Hat, Inc. All rights reserved.
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
 * @key stress randomness
 * @summary Logic that moves a null check in the expanded barrier may cause a memory access that doesn't depend on the barrier to bypass the null check
 * @requires vm.gc.Shenandoah
 * @requires vm.flavor == "server"
 * @run main/othervm -XX:-BackgroundCompilation -XX:-UseOnStackReplacement -XX:-TieredCompilation
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC
 *                   -XX:+StressGCM -XX:+StressLCM TestExpandedWBLostNullCheckDep
 */

public class TestExpandedWBLostNullCheckDep {

    static void test(int i, int[] arr) {
        // arr.length depends on a null check for arr
        if (i < 0 || i >= arr.length) {
        }
        // The write barrier here also depends on the null check. The
        // null check is moved in the barrier to enable implicit null
        // checks. The null check must not be moved arr.length
        arr[i] = 0x42;
    }

    static public void main(String[] args) {
        int[] int_arr = new int[10];
        for (int i = 0; i < 20000; i++) {
            test(0, int_arr);
        }
        try {
            test(0, null);
        } catch (NullPointerException npe) {}
    }
}
