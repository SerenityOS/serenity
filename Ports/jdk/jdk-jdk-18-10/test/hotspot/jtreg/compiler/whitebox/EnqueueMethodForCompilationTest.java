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
 * @test EnqueueMethodForCompilationTest
 * @bug 8006683 8007288 8022832
 * @summary testing of WB::enqueueMethodForCompilation()
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 *          java.management
 *
 * @requires vm.opt.DeoptimizeALot != true
 *
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -Xmixed -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+WhiteBoxAPI -XX:+PrintCompilation -XX:-UseCounterDecay
 *                   compiler.whitebox.EnqueueMethodForCompilationTest
 */

package compiler.whitebox;

public class EnqueueMethodForCompilationTest extends CompilerWhiteBoxTest {

    public static void main(String[] args) throws Exception {
        String directive =
                "[{ match:\"*SimpleTestCaseHelper.*\", BackgroundCompilation: false }, " +
                " { match:\"*.*\", inline:\"-*SimpleTestCaseHelper.*\"}]";
        if (WHITE_BOX.addCompilerDirective(directive) != 2) {
            throw new RuntimeException("Could not add directive");
        }
        try {
            CompilerWhiteBoxTest.main(EnqueueMethodForCompilationTest::new, args);
        } finally {
            WHITE_BOX.removeCompilerDirective(2);
        }
    }

    private EnqueueMethodForCompilationTest(TestCase testCase) {
        super(testCase);
        // to prevent inlining of #method
        WHITE_BOX.testSetDontInlineMethod(method, true);
    }

    @Override
    protected void test() throws Exception {
        checkNotCompiled();

        // method can not be compiled on level 'none'
        WHITE_BOX.enqueueMethodForCompilation(method, COMP_LEVEL_NONE);
        if (isCompilable(COMP_LEVEL_NONE)) {
            throw new RuntimeException(method
                    + " is compilable at level COMP_LEVEL_NONE");
        }
        checkNotCompiled();

        // COMP_LEVEL_ANY is inapplicable as level for compilation
        WHITE_BOX.enqueueMethodForCompilation(method, COMP_LEVEL_ANY);
        checkNotCompiled();

        // not existing comp level
        WHITE_BOX.enqueueMethodForCompilation(method, 42);
        checkNotCompiled();

        compile();
        checkCompiled();

        int compLevel = getCompLevel();
        int bci = WHITE_BOX.getMethodEntryBci(method);
        deoptimize();
        checkNotCompiled();
        WHITE_BOX.clearMethodState(method);

        if (!WHITE_BOX.enqueueMethodForCompilation(method, compLevel, bci)) {
           throw new RuntimeException(method
                    + " could not be enqueued for compilation");
        }
        checkCompiled();
        deoptimize();
        checkNotCompiled();

        compile();
        checkCompiled();
        deoptimize();
        checkNotCompiled();
    }
}
