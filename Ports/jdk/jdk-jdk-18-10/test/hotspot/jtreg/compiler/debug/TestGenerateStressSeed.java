/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

package compiler.debug;

import java.nio.file.Paths;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

/*
 * @test
 * @bug 8252219 8256535
 * @requires vm.compiler2.enabled
 * @summary Tests that using a stress option without -XX:StressSeed=N generates
 *          and logs a random seed.
 * @library /test/lib /
 * @run driver compiler.debug.TestGenerateStressSeed StressLCM
 * @run driver compiler.debug.TestGenerateStressSeed StressGCM
 * @run driver compiler.debug.TestGenerateStressSeed StressIGVN
 * @run driver compiler.debug.TestGenerateStressSeed StressCCP
 */

public class TestGenerateStressSeed {

    static void sum(int n) {
        int acc = 0;
        for (int i = 0; i < n; i++) acc += i;
        System.out.println(acc);
    }

    public static void main(String[] args) throws Exception {
        if (args[0].startsWith("Stress")) {
            String className = TestGenerateStressSeed.class.getName();
            String stressOpt = args[0];
            String log = "test.log";
            String[] procArgs = {
                "-Xcomp", "-XX:-TieredCompilation", "-XX:+UnlockDiagnosticVMOptions",
                "-XX:CompileOnly=" + className + "::sum", "-XX:+" + stressOpt,
                "-XX:+LogCompilation", "-XX:LogFile=" + log, className, "10"};
            new OutputAnalyzer(ProcessTools.createJavaProcessBuilder(procArgs).start())
                .shouldHaveExitValue(0);
            new OutputAnalyzer(Paths.get(log))
                .shouldContain("stress_test seed");
        } else {
            sum(Integer.parseInt(args[0]));
        }
    }
}
