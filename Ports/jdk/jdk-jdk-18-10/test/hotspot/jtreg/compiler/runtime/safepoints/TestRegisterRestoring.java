/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8148490
 * @summary Test correct saving and restoring of vector registers at safepoints.
 *
 * @run main/othervm -Xbatch -XX:-TieredCompilation
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+SafepointALot
 *                   -XX:CompileCommand=exclude,compiler.runtime.safepoints.TestRegisterRestoring::main
 *                   compiler.runtime.safepoints.TestRegisterRestoring
 */

package compiler.runtime.safepoints;

public class TestRegisterRestoring {
    public static void main(String args[]) throws Exception {
        // Initialize
        float[] array = new float[100];
        for (int i = 0; i < array.length; ++i) {
            array[i] = 0;
        }
        // Test
        for (int j = 0; j < 20_000; ++j) {
            increment(array);
            // Check result
            for (int i = 0; i < array.length; i++) {
                if (array[i] != 10_000) {
                    throw new RuntimeException("Test failed: array[" + i + "] = " + array[i] + " but should be 10,000");
                }
                array[i] = 0;
            }
        }
    }

    static void increment(float[] array) {
        // Loop with safepoint
        for (long l = 0; l < 10_000; l++) {
            // Vectorized loop
            for (int i = 0; i < array.length; ++i) {
                array[i] += 1;
            }
        }
    }
}

