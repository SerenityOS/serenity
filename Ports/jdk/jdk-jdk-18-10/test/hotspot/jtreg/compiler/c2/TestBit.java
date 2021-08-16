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

package compiler.c2;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

/*
 * @test
 * @bug 8247408
 * @summary C2 should convert ((var&16) == 16) to ((var&16) != 0) for power-of-two constants
 * @library /test/lib /
 *
 * @requires vm.flagless
 * @requires os.arch=="aarch64" | os.arch=="amd64" | os.arch == "ppc64le"
 * @requires vm.debug == true & vm.compiler2.enabled
 *
 * @run driver compiler.c2.TestBit
 */
public class TestBit {

    static void runTest(String testName) throws Exception {
        String className = TestBit.class.getName();
        String[] procArgs = {
            "-Xbatch",
            "-XX:-TieredCompilation",
            "-XX:+PrintOptoAssembly",
            "-XX:CompileCommand=compileonly," + className + "::tst*",
            className, testName};

        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(procArgs);
        OutputAnalyzer output = new OutputAnalyzer(pb.start());

        String expectedTestBitInstruction =
            "ppc64le".equals(System.getProperty("os.arch")) ? "ANDI" :
            "aarch64".equals(System.getProperty("os.arch")) ? "tb"   :
            "amd64".equals(System.getProperty("os.arch"))   ? "test" : null;

        if (expectedTestBitInstruction != null) {
            output.shouldContain(expectedTestBitInstruction);
        } else {
            System.err.println("unexpected os.arch");
        }
    }

    static final int ITER = 100000; // ~ Tier4CompileThreshold + compilation time

    // dummy volatile variable
    public static volatile long c = 0;

    // C2 is expected to generate test bit instruction on the test
    static void tstBitLong(long value) {
        if (1L == (1L & value)) {
            c++;
        } else {
            c--;
        }
    }

    // C2 is expected to generate test bit instruction on the test
    static void tstBitInt(int value) {
        if (1 == (1 & value)) {
            c++;
        } else {
            c--;
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            // Fork VMs to check their debug compiler output
            runTest("tstBitLong");
            runTest("tstBitInt");
        }
        if (args.length > 0) {
            // We are in a forked VM to execute the named test
            String testName = args[0];
            switch (testName) {
            case "tstBitLong":
                for (int i = 0; i < ITER; i++) {
                    tstBitLong(i % 2);
                }
                break;
            case "tstBitInt":
                for (int i = 0; i < ITER; i++) {
                    tstBitInt(i % 2);
                }
                break;
            default:
                throw new RuntimeException("unexpected test name " + testName);
            }
        }
    }
}
