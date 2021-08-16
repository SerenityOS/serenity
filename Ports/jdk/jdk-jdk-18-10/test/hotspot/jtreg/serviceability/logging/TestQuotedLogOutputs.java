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
 * @test TestQuotedLogOutputs
 * @summary Ensure proper parsing of quoted output names for -Xlog arguments.
 * @requires vm.flagless
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @comment after JDK-8224505, this has to be run in othervm mode
 * @run main/othervm TestQuotedLogOutputs
 */

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import jdk.test.lib.Asserts;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;

public class TestQuotedLogOutputs {

    public static void main(String[] args) throws Exception {
        // Ensure log files can be specified with full path.
        // On windows, this means that the file name will contain
        // a colon ('C:\log.txt' for example), which is used to
        // separate -Xlog: options (-Xlog:tags:filename:decorators).
        // Try to log to a file in our current directory, using its absolute path.
        String baseName = "test file.log";
        Path filePath = Paths.get(baseName).toAbsolutePath();
        String fileName = filePath.toString();
        File file = filePath.toFile();

        // In case the file already exists, attempt to delete it before running the test
        file.delete();

        // Depending on if we're on Windows or not the quotation marks must be escaped,
        // otherwise they will be stripped from the command line arguments.
        String quote;
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            quote = "\\\""; // quote should be \" (escaped quote)
        } else {
            quote = "\""; // quote should be " (no escape needed)
        }

        // Test a few variations with valid log output specifiers
        String[] validOutputs = new String[] {
            quote + fileName + quote,
            "file=" + quote + fileName + quote,
            quote + fileName + quote + ":",
            quote + fileName + quote + "::"
        };
        for (String logOutput : validOutputs) {
            // Run with logging=trace on stdout so that we can verify the log configuration afterwards.
            ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("-Xlog:logging=trace",
                                                                      "-Xlog:all=trace:" + logOutput,
                                                                      "-version");
            OutputAnalyzer output = new OutputAnalyzer(pb.start());
            output.shouldHaveExitValue(0);
            Asserts.assertTrue(file.exists());
            file.deleteOnExit(); // Clean up after test
            output.shouldMatch("\\[logging *\\].*" + baseName); // Expect to see the log output listed
        }

        // Test a bunch of invalid output specifications and ensure the VM fails with these
        String[] invalidOutputs = new String[] {
            quote,
            quote + quote, // should fail because the VM will try to create a file without a name
            quote + quote + quote,
            quote + quote + quote + quote,
            quote + quote + quote + quote + quote,
            "prefix" + quote + quote + "suffix",
            "prefix" + quote + quote,
            quote + quote + "suffix",
            quote + "A" + quote + quote + "B" + quote,
            quote + "A" + quote + "B" + quote + "C" + quote,
            "A" + quote + quote + "B"
        };
        for (String logOutput : invalidOutputs) {
            ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("-Xlog:logging=trace",
                                                                      "-Xlog:all=trace:" + logOutput,
                                                                      "-version");
            OutputAnalyzer output = new OutputAnalyzer(pb.start());
            output.shouldHaveExitValue(1);
            // Ensure error message was logged
            output.shouldMatch("([Mm]issing terminating quote)"
                + "|(Error opening log file '')"
                + "|(Output name can not be partially quoted)");
        }
    }
}

