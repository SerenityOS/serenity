/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8248231
 * @summary Test to verify lambda serialization uses the correct UTF-8 encoding
 * @library /test/lib
 * @build jdk.test.lib.JDKToolFinder
 *        jdk.test.lib.process.ProcessTools
 * @run testng LambdaFileEncodingSerialization
 */
import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.ArrayList;

import org.testng.annotations.Test;
import static org.testng.Assert.assertTrue;

import jdk.test.lib.JDKToolFinder;
import jdk.test.lib.process.ProcessTools;

public class LambdaFileEncodingSerialization {

    private static final String TEST_NAME = "TestLambdaFileEncodingSerialization";

    @Test
    public static void testDeserializeLambdaEncoding() throws Throwable {

        String javac = JDKToolFinder.getTestJDKTool("javac");
        String java = JDKToolFinder.getTestJDKTool("java");

        String srcDir = System.getProperty("test.src");

        // Compile <TEST_NAME>.java using ISO-8859-1 encoding
        String opts = "-J-Dfile.encoding=ISO-8859-1 -cp . -d .";
        String file = srcDir + File.separator + TEST_NAME + ".java";
        int exitCode = runCmd(javac, opts, file);
        assertTrue(exitCode == 0, "Command " + javac + " " + opts + " " + file + " , failed exitCode = "+exitCode);

        // Execute TEST_NAME containing the re-serialized lambda
        opts = "-cp .";
        file = TEST_NAME;
        exitCode = runCmd(java, opts, file);
        assertTrue(exitCode == 0, "Command " + java + " " + opts + " " + file + " , failed exitCode = "+exitCode);
    }

    // Run a command
    private static int runCmd(String prog, String options, String file) throws Throwable {

        List<String> argList = new ArrayList<String>();
        argList.add(prog);
        argList.addAll(Arrays.asList(options.split(" ")));
        argList.add(file);

        ProcessBuilder pb = new ProcessBuilder(argList);
        Map<String, String> env = pb.environment();
        env.put("LC_ALL", "en_US.UTF-8"); // Ensure locale supports the test requirements, lambda with a UTF char

        int exitCode = ProcessTools.executeCommand(pb).outputTo(System.out)
                                          .errorTo(System.err)
                                          .getExitValue();
        return exitCode;
    }

}

