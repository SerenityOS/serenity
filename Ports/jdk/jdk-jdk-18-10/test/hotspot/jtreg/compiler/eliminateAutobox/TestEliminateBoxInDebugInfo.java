/*
 * Copyright (c) 2021, Huawei Technologies Co., Ltd. All rights reserved.
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
 * @bug 8261137
 * @requires vm.flagless
 * @requires vm.debug == true & vm.compiler2.enabled
 * @summary Verify that box object is scalarized in case it is directly referenced by debug info.
 * @library /test/lib
 *
 * @run driver compiler.eliminateAutobox.TestEliminateBoxInDebugInfo
 */
package compiler.eliminateAutobox;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class TestEliminateBoxInDebugInfo {
    public static void runTest() throws Exception {
        String[] arguments = {
            "-XX:CompileCommand=compileonly,compiler/eliminateAutobox/TestEliminateBoxInDebugInfo$Test.foo",
            "-XX:CompileCommand=dontinline,compiler/eliminateAutobox/TestEliminateBoxInDebugInfo$Test.black",
            "-Xbatch",
            "-XX:+PrintEliminateAllocations",
            Test.class.getName()
        };
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(arguments);
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldHaveExitValue(0)
              .stdoutShouldContain("++++ Eliminated: ");
    }

    public static void main(String[] args) throws Exception {
        runTest();
    }

    static class Test {
        public static void main(String[] args) throws Exception {
            // warmup
            for (int i = 0; i < 100000; i++) {
               foo(1000 + (i % 1000));
            }
        }

        public static int foo(int value) {
            Integer ii = Integer.valueOf(value);
            int sum = 0;
            if (value > 999) {
                sum += ii.intValue();
            }
            black();
            return sum;
        }

        public static void black() {}
    }
}
