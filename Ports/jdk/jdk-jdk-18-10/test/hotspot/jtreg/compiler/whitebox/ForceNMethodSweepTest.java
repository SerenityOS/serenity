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
 * @test
 * @bug 8059624 8064669 8153265
 * @summary testing of WB::forceNMethodSweep
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 *          java.management
 *
 * @requires vm.opt.DeoptimizeALot != true
 *
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *                   -XX:-TieredCompilation -XX:+WhiteBoxAPI
 *                   -XX:CompileCommand=compileonly,compiler.whitebox.SimpleTestCaseHelper::*
 *                   -XX:-BackgroundCompilation -XX:-UseCounterDecay
 *                   -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockExperimentalVMOptions -XX:+EagerJVMCI
 *                   compiler.whitebox.ForceNMethodSweepTest
 */

package compiler.whitebox;

import jdk.test.lib.Asserts;
import sun.hotspot.code.BlobType;

import java.util.EnumSet;

public class ForceNMethodSweepTest extends CompilerWhiteBoxTest {
    public static void main(String[] args) throws Exception {
        CompilerWhiteBoxTest.main(ForceNMethodSweepTest::new, args);
    }
    private final EnumSet<BlobType> blobTypes;
    private ForceNMethodSweepTest(TestCase testCase) {
        super(testCase);
        // to prevent inlining of #method
        WHITE_BOX.testSetDontInlineMethod(method, true);
        blobTypes = BlobType.getAvailable();
    }

    @Override
    protected void test() throws Exception {
        // prime the asserts: get their bytecodes loaded, any lazy computation
        // resolved, and executed once
        Asserts.assertGT(1, 0, "message");
        Asserts.assertLTE(0, 0, "message");
        Asserts.assertLT(-1, 0, "message");

        checkNotCompiled();
        guaranteedSweep();
        int usage = getTotalUsage();

        compile();
        checkCompiled();
        int afterCompilation = getTotalUsage();
        Asserts.assertGT(afterCompilation, usage,
                "compilation should increase usage");

        guaranteedSweep();
        int afterSweep = getTotalUsage();
        Asserts.assertLTE(afterSweep, afterCompilation,
                "sweep shouldn't increase usage");

        deoptimize();
        guaranteedSweep();
        int afterDeoptAndSweep = getTotalUsage();
        Asserts.assertLT(afterDeoptAndSweep, afterSweep,
                "sweep after deoptimization should decrease usage");
     }

    private int getTotalUsage() {
        int usage = 0;
        for (BlobType type : blobTypes) {
           usage += type.getMemoryPool().getUsage().getUsed();
        }
        return usage;
    }
    private void guaranteedSweep() {
        // not entrant -> ++stack_traversal_mark -> zombie -> flushed
        for (int i = 0; i < 5; ++i) {
            WHITE_BOX.fullGC();
            WHITE_BOX.forceNMethodSweep();
        }
    }
}
