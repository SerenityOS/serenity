/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @test LockCompilationTest
 * @bug 8059624 8152169
 * @summary testing of WB::lock/unlockCompilation()
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -Xmixed -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+WhiteBoxAPI -XX:-UseCounterDecay
 *                   -XX:CompileCommand=compileonly,*SimpleTestCaseHelper::method
 *                   compiler.whitebox.LockCompilationTest
 */

package compiler.whitebox;

import jdk.test.lib.Asserts;

public class LockCompilationTest extends CompilerWhiteBoxTest {

    public static void main(String[] args) throws Exception {
        // This case waits for 5 seconds and verifies that the method hasn't been
        // compiled during that time. Only do that for one of the test cases.

        CompilerWhiteBoxTest.main(LockCompilationTest::new, new String[] {"METHOD_TEST"});
    }

    private LockCompilationTest(TestCase testCase) {
        super(testCase);
        // to prevent inlining of #method
        WHITE_BOX.testSetDontInlineMethod(method, true);
    }

    protected void test() throws Exception {
        checkNotCompiled();

        System.out.println("locking compilation");
        WHITE_BOX.lockCompilation();

        try {
            System.out.println("trying to compile");
            compile();
            // to check if it works correctly w/ safepoints
            System.out.println("going to safepoint");
            WHITE_BOX.fullGC();
            // Sleep a while and then make sure the compile is still waiting
            Thread.sleep(5000);

            Asserts.assertTrue(
                    WHITE_BOX.isMethodQueuedForCompilation(method),
                    method + " must be in queue");
            Asserts.assertFalse(
                    WHITE_BOX.isMethodCompiled(method, false),
                    method + " must be not compiled");
            Asserts.assertEQ(
                    WHITE_BOX.getMethodCompilationLevel(method, false), 0,
                    method + " comp_level must be == 0");
            Asserts.assertFalse(
                    WHITE_BOX.isMethodCompiled(method, true),
                    method + " must be not osr_compiled");
            Asserts.assertEQ(
                    WHITE_BOX.getMethodCompilationLevel(method, true), 0,
                    method + " osr_comp_level must be == 0");
        } finally {
            System.out.println("unlocking compilation");
            WHITE_BOX.unlockCompilation();
        }
        waitBackgroundCompilation();
        Asserts.assertFalse(
                WHITE_BOX.isMethodQueuedForCompilation(method),
                method + " must not be in queue");
    }
}

