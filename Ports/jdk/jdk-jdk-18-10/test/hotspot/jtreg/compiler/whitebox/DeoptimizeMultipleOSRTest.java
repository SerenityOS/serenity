/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @test DeoptimizeMultipleOSRTest
 * @bug 8061817
 * @summary testing of WB::deoptimizeMethod()
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 *          java.management
 *
 * @requires vm.opt.DeoptimizeALot != true
 *
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   -XX:CompileCommand=compileonly,compiler.whitebox.DeoptimizeMultipleOSRTest::triggerOSR
 *                   compiler.whitebox.DeoptimizeMultipleOSRTest
 */

package compiler.whitebox;

import sun.hotspot.WhiteBox;

import java.lang.reflect.Method;

public class DeoptimizeMultipleOSRTest {
    private static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();
    private static final long BACKEDGE_THRESHOLD = 150000;
    private Method method;
    private int counter = 0;

    public static void main(String[] args) throws Exception {
        DeoptimizeMultipleOSRTest test = new DeoptimizeMultipleOSRTest();
        test.test();
    }

    /**
     * Triggers two different OSR compilations for the same method and
     * checks if WhiteBox.deoptimizeMethod() deoptimizes both.
     *
     * @throws Exception
     */
    public void test() throws Exception {
        method = DeoptimizeMultipleOSRTest.class.getDeclaredMethod("triggerOSR", boolean.class, long.class);
        // Trigger two OSR compiled versions
        triggerOSR(true, BACKEDGE_THRESHOLD);
        triggerOSR(false, BACKEDGE_THRESHOLD);
        // Wait for compilation
        CompilerWhiteBoxTest.waitBackgroundCompilation(method);
        // Deoptimize
        WHITE_BOX.deoptimizeMethod(method, true);
        if (WHITE_BOX.isMethodCompiled(method, true)) {
            throw new AssertionError("Not all OSR compiled versions were deoptimized");
        }
    }

    /**
     * Triggers OSR compilations by executing loops.
     *
     * @param first Determines which loop to execute
     * @param limit The number of loop iterations
     */
    public void triggerOSR(boolean first, long limit) {
        if (limit != 1) {
            // Warmup method to avoid uncommon traps
            for (int i = 0; i < limit; ++i) {
                triggerOSR(first, 1);
            }
            CompilerWhiteBoxTest.waitBackgroundCompilation(method);
        }
        if (first) {
            // Trigger OSR compilation 1
            for (int i = 0; i < limit; ++i) {
                counter++;
            }
        } else {
            // Trigger OSR compilation 2
            for (int i = 0; i < limit; ++i) {
                counter++;
            }
        }
    }
}
