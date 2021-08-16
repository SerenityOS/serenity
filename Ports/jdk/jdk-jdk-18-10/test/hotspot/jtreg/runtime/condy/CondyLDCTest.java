/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8186211
 * @summary Tests various ldc, ldc_w, ldc2_w instructions of CONSTANT_Dynamic.
 * @requires vm.flagless
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @compile CondyUseLDC_W.jasm
 * @compile CondyBadLDC2_W.jasm
 * @compile CondyBadLDC.jasm
 * @run driver CondyLDCTest
 */

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.compiler.InMemoryJavaCompiler;

public class CondyLDCTest {
    public static void main(String args[]) throws Throwable {
        // 1. Test a ldc_w instruction can be used with condy's which generate
        //    loadable constants of the following types: byte, char, short, float, integer, boolean.
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("-Xverify:all",
                                                                  "CondyUseLDC_W");
        OutputAnalyzer oa = new OutputAnalyzer(pb.start());
        oa.shouldNotContain("VerifyError");
        oa.shouldHaveExitValue(0);

        // 2. Test ldc2_w of a condy which returns a dynamically generated
        //    float constant, generates a VerifyError.
        pb = ProcessTools.createJavaProcessBuilder("-Xverify:all",
                                                   "CondyBadLDC2_W");
        oa = new OutputAnalyzer(pb.start());
        oa.shouldContain("java.lang.VerifyError: Illegal type at constant pool entry");
        oa.shouldContain("CondyBadLDC2_W.F()F @0: ldc2_w");
        oa.shouldHaveExitValue(1);

        // 3. Test a ldc of a condy which returns a dynamically generated
        //    double constant, generates a VerifyError.
        pb = ProcessTools.createJavaProcessBuilder("-Xverify:all",
                                                   "CondyBadLDC");
        oa = new OutputAnalyzer(pb.start());
        oa.shouldContain("java.lang.VerifyError: Illegal type at constant pool entry");
        oa.shouldContain("CondyBadLDC.D()D @0: ldc");
        oa.shouldHaveExitValue(1);
    }
}
