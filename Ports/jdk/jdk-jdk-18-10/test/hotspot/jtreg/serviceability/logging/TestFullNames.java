/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @test TestFullNames
 * @bug 8215398
 * @summary Ensure proper parsing of unquoted full output names for -Xlog arguments.
 * @requires vm.flagless
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @run driver TestFullNames
 */

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import jdk.test.lib.Asserts;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;

public class TestFullNames {

    public static void main(String[] args) throws Exception {
        // Ensure log files can be specified with full path.
        // On windows, this means that the file name will contain
        // a colon ('C:\log.txt' for example), which is used to
        // separate -Xlog: options (-Xlog:tags:filename:decorators).
        // Try to log to a file in our current directory, using its absolute path.
        String baseName = "testfile.log";
        Path filePath = Paths.get(baseName).toAbsolutePath();
        String fileName = filePath.toString();
        File file = filePath.toFile();

        // In case the file already exists, attempt to delete it before running the test
        file.delete();

        // Test full pathnames without quotes.
        String[] validOutputs = new String[] {
            "file=" + fileName,
            fileName
        };
        for (String logOutput : validOutputs) {
            Asserts.assertFalse(file.exists());
            // Run with logging=trace on stdout so that we can verify the log configuration afterwards.
            ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("-Xlog:logging=trace",
                                                                      "-Xlog:all=trace:" + logOutput,
                                                                      "-version");
            OutputAnalyzer output = new OutputAnalyzer(pb.start());
            output.shouldHaveExitValue(0);
            Asserts.assertTrue(file.exists());
            file.delete();
            output.shouldMatch("\\[logging *\\].*" + baseName); // Expect to see the log output listed
        }
    }
}

