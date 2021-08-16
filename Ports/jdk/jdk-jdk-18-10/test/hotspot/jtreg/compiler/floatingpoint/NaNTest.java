/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8076373
 * @summary Verify if signaling NaNs are preserved.
 * @library /test/lib /
 *
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   compiler.floatingpoint.NaNTest
 */

package compiler.floatingpoint;

import jdk.test.lib.Platform;
import jtreg.SkippedException;
import sun.hotspot.WhiteBox;

public class NaNTest {
    static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();

    static void testFloat() {
        int originalValue = 0x7f800001;
        int readBackValue = Float.floatToRawIntBits(Float.intBitsToFloat(originalValue));
        if (originalValue != readBackValue) {
            String errorMessage = String.format("Original and read back float values mismatch\n0x%X 0x%X\n",
                                                originalValue,
                                                readBackValue);
            throw new RuntimeException(errorMessage);
        } else {
            System.out.printf("Written and read back float values match\n0x%X 0x%X\n",
                              originalValue,
                              readBackValue);
        }
    }

    static void testDouble() {
        long originalValue = 0xFFF0000000000001L;
        long readBackValue = Double.doubleToRawLongBits(Double.longBitsToDouble(originalValue));
        if (originalValue != readBackValue) {
            String errorMessage = String.format("Original and read back double values mismatch\n0x%X 0x%X\n",
                                                originalValue,
                                                readBackValue);
            throw new RuntimeException(errorMessage);
        } else {
            System.out.printf("Written and read back double values match\n0x%X 0x%X\n",
                              originalValue,
                              readBackValue);
        }

    }

    public static void main(String args[]) {
        // Some platforms are known to strip signaling NaNs.
        // The block below can be used to except them.
        boolean expectStableFloats = true;
        boolean expectStableDoubles = true;

        // On x86_32 without relevant SSE-enabled stubs, we are entering
        // native methods that use FPU instructions, and those strip the
        // signaling NaNs.
        if (Platform.isX86()) {
            int sse = WHITE_BOX.getIntxVMFlag("UseSSE").intValue();
            expectStableFloats = (sse >= 1);
            expectStableDoubles = (sse >= 2);
        }

        if (expectStableFloats) {
           testFloat();
        } else {
           System.out.println("Stable floats cannot be expected, skipping");
        }

        if (expectStableDoubles) {
           testDouble();
        } else {
           System.out.println("Stable doubles cannot be expected, skipping");
        }

        if (!expectStableFloats && !expectStableDoubles) {
           throw new SkippedException("No tests were run.");
        }
    }
}
