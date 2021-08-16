/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test the OutputAnalyzer reporting functionality,
 *     such as printing additional diagnostic info
 *     (exit code, stdout, stderr, command line, etc.)
 * @modules java.management
 * @library /test/lib
 * @run main OutputAnalyzerReportingTest
 */

import jdk.test.lib.process.OutputAnalyzer;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class OutputAnalyzerReportingTest {

    public static void main(String[] args) throws Exception {
        // Create the output analyzer under test
        String stdout = "aaaaaa";
        String stderr = "bbbbbb";
        OutputAnalyzer output = new OutputAnalyzer(stdout, stderr);

        // Expected summary values should be the same for all cases,
        // since the outputAnalyzer object is the same
        String expectedExitValue = "-1";
        String expectedSummary =
                " stdout: [" + stdout + "];\n" +
                " stderr: [" + stderr + "]\n" +
                " exitValue = " + expectedExitValue + "\n";


        DiagnosticSummaryTestRunner testRunner =
                new DiagnosticSummaryTestRunner();

        // should have exit value
        testRunner.init(expectedSummary);
        int unexpectedExitValue = 2;
        try {
            output.shouldHaveExitValue(unexpectedExitValue);
        } catch (RuntimeException e) { }
        testRunner.closeAndCheckResults();

        // should not contain
        testRunner.init(expectedSummary);
        try {
            output.shouldNotContain(stdout);
        } catch (RuntimeException e) { }
        testRunner.closeAndCheckResults();

        // should contain
        testRunner.init(expectedSummary);
        try {
            output.shouldContain("unexpected-stuff");
        } catch (RuntimeException e) { }
        testRunner.closeAndCheckResults();

        // should not match
        testRunner.init(expectedSummary);
        try {
            output.shouldNotMatch("[a]");
        } catch (RuntimeException e) { }
        testRunner.closeAndCheckResults();

        // should match
        testRunner.init(expectedSummary);
        try {
            output.shouldMatch("[qwerty]");
        } catch (RuntimeException e) { }
        testRunner.closeAndCheckResults();

    }

    private static class DiagnosticSummaryTestRunner {
        private ByteArrayOutputStream byteStream =
                new ByteArrayOutputStream(10000);

        private String expectedSummary = "";
        private PrintStream errStream;


        public void init(String expectedSummary) {
            this.expectedSummary = expectedSummary;
            byteStream.reset();
            errStream = new PrintStream(byteStream);
            System.setErr(errStream);
        }

        public void closeAndCheckResults() {
            // check results
            errStream.close();
            String stdErrStr = byteStream.toString();
            if (!stdErrStr.contains(expectedSummary)) {
                throw new RuntimeException("The output does not contain "
                    + "the diagnostic message, or the message is incorrect");
            }
        }
    }

}
