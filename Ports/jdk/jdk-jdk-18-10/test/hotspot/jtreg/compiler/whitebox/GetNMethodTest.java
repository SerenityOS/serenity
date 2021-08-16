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
 * @test GetNMethodTest
 * @bug 8038240
 * @summary testing of WB::getNMethod()
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 *          java.management
 *
 * @requires vm.opt.DeoptimizeALot != true
 *
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -Xmixed -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+WhiteBoxAPI -XX:-UseCounterDecay
 *                   -XX:CompileCommand=compileonly,compiler.whitebox.SimpleTestCaseHelper::*
 *                   compiler.whitebox.GetNMethodTest
 */

package compiler.whitebox;

import jdk.test.lib.Asserts;
import sun.hotspot.code.BlobType;
import sun.hotspot.code.NMethod;

public class GetNMethodTest extends CompilerWhiteBoxTest {
    public static void main(String[] args) throws Exception {
        CompilerWhiteBoxTest.main(GetNMethodTest::new, args);
    }

    private GetNMethodTest(TestCase testCase) {
        super(testCase);
        // to prevent inlining of #method
        WHITE_BOX.testSetDontInlineMethod(method, true);
    }

    @Override
    protected void test() throws Exception {
        checkNotCompiled();

        compile();
        checkCompiled();

        NMethod nmethod = NMethod.get(method, testCase.isOsr());
        if (IS_VERBOSE) {
            System.out.println("nmethod = " + nmethod);
        }
        Asserts.assertNotNull(nmethod,
                "nmethod of compiled method is null");
        Asserts.assertNotNull(nmethod.insts,
                "nmethod.insts of compiled method is null");
        Asserts.assertGT(nmethod.insts.length, 0,
                "compiled method's instructions is empty");
        Asserts.assertNotNull(nmethod.code_blob_type, "blob type is null");
        if (WHITE_BOX.getBooleanVMFlag("SegmentedCodeCache")) {
            Asserts.assertNE(nmethod.code_blob_type, BlobType.All);
            switch (nmethod.comp_level) {
            case 1:
            case 4:
                checkBlockType(nmethod, BlobType.MethodNonProfiled);
                break;
            case 2:
            case 3:
                checkBlockType(nmethod, BlobType.MethodProfiled);
                break;
            default:
                throw new Error("unexpected comp level " + nmethod);
            }
        } else {
            Asserts.assertEQ(nmethod.code_blob_type, BlobType.All);
        }

        deoptimize();
        checkNotCompiled();
        nmethod = NMethod.get(method, testCase.isOsr());
        Asserts.assertNull(nmethod,
                "nmethod of non-compiled method isn't null");
    }

    private void checkBlockType(NMethod nmethod, BlobType expectedType) {
        Asserts.assertEQ(nmethod.code_blob_type, expectedType,
                String.format("blob_type[%s] for %d level isn't %s",
                        nmethod.code_blob_type, nmethod.comp_level, expectedType));
    }
}
