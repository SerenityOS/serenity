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
 * @summary CONSTANT_Dynamic_info structure's tries to use a BSM index whose signature is for an invokedynamic and vice versa.
 * @requires vm.flagless
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @compile CondyUsesIndyBSM.jcod
 * @compile IndyUsesCondyBSM.jcod
 * @run driver BadBSMUseTest
 */

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.compiler.InMemoryJavaCompiler;

// BootstrapMethodError expected in each test case below.
public class BadBSMUseTest {
    public static void main(String args[]) throws Throwable {
        // 1. Test a CONSTANT_Dynamic_info's bootstrap_method_attr_index points
        //    at a BSM meant for a CONSTANT_InvokeDynamic_info
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("CondyUsesIndyBSM");
        OutputAnalyzer oa = new OutputAnalyzer(pb.start());
        oa.shouldContain("In Indybsm target CallSite method foo");
        oa.shouldContain("BootstrapMethodError: bootstrap method initialization exception");
        oa.shouldHaveExitValue(1);

        // 2. Test a CONSTANT_InvokeDynamic_info's bootstrap_method_attr_index points
        //    at a BSM meant for a CONSTANT_Dynamic_info
        pb = ProcessTools.createJavaProcessBuilder("IndyUsesCondyBSM");
        oa = new OutputAnalyzer(pb.start());
        oa.shouldContain("In Condybsm");
        oa.shouldContain("BootstrapMethodError: bootstrap method initialization exception");
        oa.shouldHaveExitValue(1);
    }
}
