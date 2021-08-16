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
 * @test TestCompileCommand
 * @bug 8069389
 * @summary Regression tests of -XX:CompileCommand
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @requires vm.flagless
 * @run driver compiler.oracle.TestCompileCommand
 */

package compiler.oracle;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class TestCompileCommand {

    private static final String[][] ARGUMENTS = {
        {
            "-XX:CompileCommand=print,*01234567890123456789012345678901234567890123456789.*0123456789012345678901234567890123456789",
            "-version"
        }
    };

    private static final String[][] OUTPUTS = {
        {
            "print *01234567890123456789012345678901234567890123456789.*0123456789012345678901234567890123456789"
        }
    };

    private static void verifyValidOption(String[] arguments, String[] expected_outputs) throws Exception {
        ProcessBuilder pb;
        OutputAnalyzer out;

        pb = ProcessTools.createJavaProcessBuilder(arguments);
        out = new OutputAnalyzer(pb.start());

        for (String expected_output : expected_outputs) {
            out.shouldContain(expected_output);
        }

        out.shouldNotContain("CompileCommand: An error occurred during parsing");
        out.shouldHaveExitValue(0);
    }

    public static void main(String[] args) throws Exception {

        if (ARGUMENTS.length != OUTPUTS.length) {
            throw new RuntimeException("Test is set up incorrectly: length of arguments and expected outputs for type (1) options does not match.");
        }

        // Check if type (1) options are parsed correctly
        for (int i = 0; i < ARGUMENTS.length; i++) {
            verifyValidOption(ARGUMENTS[i], OUTPUTS[i]);
        }
    }
}
