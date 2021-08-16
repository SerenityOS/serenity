/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8190797
 * @summary Test OSR compilation with bad operand stack.
 * @library /test/lib /
 * @requires vm.flagless
 * @compile OSRWithBadOperandStack.jasm
 * @run driver compiler.linkage.TestLinkageErrorInGenerateOopMap
 */

package compiler.linkage;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class TestLinkageErrorInGenerateOopMap {

    public static void main(String args[]) throws Exception {
        if (args.length == 0) {
            // Spawn new VM instance to execute test
            ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
                    "-XX:+UnlockDiagnosticVMOptions",
                    "-XX:-BytecodeVerificationRemote",
                    "-XX:-BytecodeVerificationLocal",
                    "-XX:-TieredCompilation",
                    "-XX:CompileCommand=dontinline,compiler/linkage/OSRWithBadOperandStack.m*",
                    "-XX:-CreateCoredumpOnCrash",
                    "-Xmx64m",
                    TestLinkageErrorInGenerateOopMap.class.getName(),
                    "run");
            OutputAnalyzer out = new OutputAnalyzer(pb.start());
            if (out.getExitValue() != 0) {
                // OSR compilation should exit with an error during OopMap verification
                // because a LinkageError cannot be thrown from a compiler thread.
                out.shouldContain("fatal error: Illegal class file encountered");
            }
        } else {
            // Execute test
            OSRWithBadOperandStack.test();
        }
    }
}
