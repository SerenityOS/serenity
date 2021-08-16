/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @test CheckCheckCICompilerCount
 * @bug 8130858 8132525 8162881
 * @summary Check that correct range of values for CICompilerCount are allowed depending on whether tiered is enabled or not
 * @library /test/lib /
 * @requires vm.flagless
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run driver compiler.arguments.CheckCICompilerCount
 */

package compiler.arguments;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class CheckCICompilerCount {
    private static final String[][] NON_TIERED_ARGUMENTS = {
        {
            "-server",
            "-XX:-TieredCompilation",
            "-XX:+PrintFlagsFinal",
            "-XX:CICompilerCount=0",
            "-version"
        },
        {
            "-server",
            "-XX:-TieredCompilation",
            "-XX:+PrintFlagsFinal",
            "-XX:CICompilerCount=1",
            "-version"
        },
        {
            "-server",
            "-XX:+PrintFlagsFinal",
            "-XX:CICompilerCount=1",
            "-XX:-TieredCompilation",
            "-version"
        },
        {
            "-client",
            "-XX:-TieredCompilation",
            "-XX:+PrintFlagsFinal",
            "-XX:CICompilerCount=0",
            "-version"
        },
        {
            "-client",
            "-XX:-TieredCompilation",
            "-XX:+PrintFlagsFinal",
            "-XX:CICompilerCount=1",
            "-version"
        },
        {
            "-client",
            "-XX:+PrintFlagsFinal",
            "-XX:CICompilerCount=1",
            "-XX:-TieredCompilation",
            "-version"
        }
    };

    private static final String[] NON_TIERED_EXPECTED_OUTPUTS = {
            "CICompilerCount (0) must be at least 1",
            "intx CICompilerCount                          = 1                                         {product} {command line}",
            "intx CICompilerCount                          = 1                                         {product} {command line}",
            "CICompilerCount (0) must be at least 1",
            "intx CICompilerCount                          = 1                                         {product} {command line}",
            "intx CICompilerCount                          = 1                                         {product} {command line}"
    };

    private static final int[] NON_TIERED_EXIT = {
        1,
        0,
        0,
        1,
        0,
        0
    };

    private static final String[][] TIERED_ARGUMENTS = {
        {
            "-server",
            "-XX:+TieredCompilation",
            "-XX:+PrintFlagsFinal",
            "-XX:CICompilerCount=1",
            "-version"
        },
        {
            "-server",
            "-XX:+TieredCompilation",
            "-XX:TieredStopAtLevel=1",
            "-XX:+PrintFlagsFinal",
            "-XX:CICompilerCount=1",
            "-version"
        },
        {
            "-server",
            "-XX:+TieredCompilation",
            "-XX:+PrintFlagsFinal",
            "-XX:CICompilerCount=1",
            "-XX:TieredStopAtLevel=1",
            "-version"
        },
        {
            "-server",
            "-XX:+TieredCompilation",
            "-XX:+PrintFlagsFinal",
            "-XX:CICompilerCount=2",
            "-version"
        },
        {
            "-client",
            "-XX:+TieredCompilation",
            "-XX:+PrintFlagsFinal",
            "-XX:CICompilerCount=1",
            "-version"
        },
        {
            "-client",
            "-XX:+TieredCompilation",
            "-XX:TieredStopAtLevel=1",
            "-XX:+PrintFlagsFinal",
            "-XX:CICompilerCount=1",
            "-version"
        },
        {
            "-client",
            "-XX:+TieredCompilation",
            "-XX:+PrintFlagsFinal",
            "-XX:CICompilerCount=1",
            "-XX:TieredStopAtLevel=1",
            "-version"
        },
        {
            "-client",
            "-XX:+TieredCompilation",
            "-XX:+PrintFlagsFinal",
            "-XX:CICompilerCount=2",
            "-version"
        }
    };

    private static final String[] TIERED_EXPECTED_OUTPUTS = {
            "CICompilerCount (1) must be at least 2",
            "intx CICompilerCount                          = 1                                         {product} {command line}",
            "intx CICompilerCount                          = 1                                         {product} {command line}",
            "intx CICompilerCount                          = 2                                         {product} {command line}",
            "CICompilerCount (1) must be at least 2",
            "intx CICompilerCount                          = 1                                         {product} {command line}",
            "intx CICompilerCount                          = 1                                         {product} {command line}",
            "intx CICompilerCount                          = 2                                         {product} {command line}",
    };

    private static final int[] TIERED_EXIT = {
        1,
        0,
        0,
        0,
        1,
        0,
        0,
        0
    };

    private static void verifyValidOption(String[] arguments, String expected_output, int exit, boolean tiered) throws Exception {
        ProcessBuilder pb;
        OutputAnalyzer out;

        pb = ProcessTools.createJavaProcessBuilder(arguments);
        out = new OutputAnalyzer(pb.start());

        try {
            out.shouldHaveExitValue(exit);
            out.shouldContain(expected_output);
        } catch (RuntimeException e) {
            // Check if tiered compilation is available in this JVM
            // Version. Throw exception only if it is available.
            if (!(tiered && out.getOutput().contains("-XX:+TieredCompilation not supported in this VM"))) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        if (NON_TIERED_ARGUMENTS.length != NON_TIERED_EXPECTED_OUTPUTS.length || NON_TIERED_ARGUMENTS.length != NON_TIERED_EXIT.length) {
            throw new RuntimeException("Test is set up incorrectly: length of arguments, expected outputs and exit codes in non-tiered mode of operation do not match.");
        }

        if (TIERED_ARGUMENTS.length != TIERED_EXPECTED_OUTPUTS.length || TIERED_ARGUMENTS.length != TIERED_EXIT.length) {
            throw new RuntimeException("Test is set up incorrectly: length of arguments, expected outputs and exit codes in tiered mode of operation do not match.");
        }

        for (int i = 0; i < NON_TIERED_ARGUMENTS.length; i++) {
            verifyValidOption(NON_TIERED_ARGUMENTS[i], NON_TIERED_EXPECTED_OUTPUTS[i], NON_TIERED_EXIT[i], false);
        }

        for (int i = 0; i < TIERED_ARGUMENTS.length; i++) {
            verifyValidOption(TIERED_ARGUMENTS[i], TIERED_EXPECTED_OUTPUTS[i], TIERED_EXIT[i], true);
        }
    }
}
