/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @test TestEnqueueMethodForCompilation
 * @summary If TieredCompilation is disabled, TieredStopAtLevel should have no effect.
 * @requires vm.flavor == "server"
 * @library /test/lib /
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   -XX:TieredStopAtLevel=1 -XX:-TieredCompilation
 *                   compiler.tiered.TestEnqueueMethodForCompilation
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   -XX:TieredStopAtLevel=2 -XX:-TieredCompilation
 *                   compiler.tiered.TestEnqueueMethodForCompilation
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   -XX:TieredStopAtLevel=3 -XX:-TieredCompilation
 *                   compiler.tiered.TestEnqueueMethodForCompilation
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   -XX:TieredStopAtLevel=4 -XX:-TieredCompilation
 *                   compiler.tiered.TestEnqueueMethodForCompilation
 */

package compiler.tiered;

import java.lang.reflect.Method;
import compiler.whitebox.CompilerWhiteBoxTest;

import sun.hotspot.WhiteBox;

public class TestEnqueueMethodForCompilation {
    private static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();

    public void test() { }

    public static void main(String[] args) throws Exception {
        Method method = TestEnqueueMethodForCompilation.class.getMethod("test");
        boolean success = WHITE_BOX.enqueueMethodForCompilation(method, CompilerWhiteBoxTest.COMP_LEVEL_FULL_OPTIMIZATION);
        if (!success) {
            throw new RuntimeException("Method could not be enqueued for compilation at level " + CompilerWhiteBoxTest.COMP_LEVEL_FULL_OPTIMIZATION);
        }
    }
}
