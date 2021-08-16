/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8163346
 * @summary Test hashing of extended characters in Serviceability Agent.
 * @requires vm.hasSA
 * @library /test/lib
 * @compile -encoding utf8 HeapDumpTest.java
 * @run main/timeout=240 HeapDumpTest
 */

import static jdk.test.lib.Asserts.assertTrue;

import java.io.IOException;
import java.io.File;
import java.util.List;
import java.util.Arrays;

import jdk.test.lib.Utils;
import jdk.test.lib.apps.LingeredApp;
import jdk.test.lib.hprof.parser.HprofReader;
import jdk.test.lib.JDKToolLauncher;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.SA.SATestUtils;

public class HeapDumpTest {

    private static LingeredAppWithExtendedChars theApp = null;
    private static final String SUCCESS_STRING = "heap written to";
    /**
     *
     * @param vmArgs  - tool arguments to launch jhsdb
     * @return exit code of tool
     */
    public static void launch(int expectedExitValue, List<String> toolArgs)
        throws IOException {

        System.out.println("Starting LingeredApp");
        try {
            theApp = new LingeredAppWithExtendedChars();
            LingeredApp.startApp(theApp, "-Xmx256m");

            System.out.println(theApp.\u00CB);
            System.out.println("Starting " + toolArgs.get(0) + " against " + theApp.getPid());
            JDKToolLauncher launcher = JDKToolLauncher.createUsingTestJDK("jhsdb");
            launcher.addVMArgs(Utils.getFilteredTestJavaOpts("-Xcomp"));

            for (String cmd : toolArgs) {
                launcher.addToolArg(cmd);
            }

            launcher.addToolArg("--pid=" + Long.toString(theApp.getPid()));

            ProcessBuilder processBuilder = SATestUtils.createProcessBuilder(launcher);
            processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
            OutputAnalyzer output = ProcessTools.executeProcess(processBuilder);
            System.out.println("stdout:");
            System.out.println(output.getStdout());
            System.out.println("stderr:");
            System.out.println(output.getStderr());
            output.shouldHaveExitValue(expectedExitValue);
            if (expectedExitValue == 0) {
                output.shouldContain(SUCCESS_STRING);
            } else {
                output.stdoutShouldNotContain(SUCCESS_STRING);
            }
        } catch (Exception ex) {
            throw new RuntimeException("Test ERROR " + ex, ex);
        } finally {
            LingeredApp.stopApp(theApp);
        }
    }

    public static void launch(int expectedExitValue, String... toolArgs)
        throws IOException {
        launch(expectedExitValue, Arrays.asList(toolArgs));
    }

    public static void printStackTraces(String file) throws IOException {
        try {
            String output = HprofReader.getStack(file, 0);
            if (!output.contains("LingeredApp.steadyState")) {
                throw new RuntimeException("'LingeredApp.steadyState' missing from stdout/stderr");
            }
        } catch (Exception ex) {
            throw new RuntimeException("Test ERROR " + ex, ex);
        }
    }

    public static void testHeapDump(SubTest subtest) throws IOException {
        String gzOption = subtest.getGzOption();
        boolean checkSuccess = subtest.needCheckSuccess();
        int expectedExitValue = checkSuccess ? 0 : 1;

        File dump = new File("jhsdb.jmap.heap." +
                             System.currentTimeMillis() + ".hprof");
        if (dump.exists()) {
            dump.delete();
        }
        if (gzOption == null || gzOption.length() == 0) {
            launch(expectedExitValue, "jmap",
                   "--binaryheap", "--dumpfile=" + dump.getAbsolutePath());
        } else {
            launch(expectedExitValue, "jmap",
                   "--binaryheap", gzOption, "--dumpfile=" + dump.getAbsolutePath());
        }

        if (checkSuccess) {
            assertTrue(dump.exists() && dump.isFile(),
                       "Could not create dump file " + dump.getAbsolutePath());

            printStackTraces(dump.getAbsolutePath());
            dump.delete();
        } else {
            assertTrue(!dump.exists(), "Unexpected file created: " + dump.getAbsolutePath());
        }
    }

    public static void main(String[] args) throws Exception {
        SATestUtils.skipIfCannotAttach(); // throws SkippedException if attach not expected to work.
        SubTest[] subtests = new SubTest[] {
                new SubTest("", true/*checkSuccess*/),
                new SubTest("--gz=1", true),
                new SubTest("--gz=9", true),
                new SubTest("--gz=0", false),
                new SubTest("--gz=100", false),
                new SubTest("--gz=", false),
                new SubTest("--gz", false),
        };
        // Run subtests
        for (int i = 0; i < subtests.length;i++) {
            testHeapDump(subtests[i]);
        }
        // The test throws RuntimeException on error.
        // IOException is thrown if LingeredApp can't start because of some bad
        // environment condition
        System.out.println("Test PASSED");
    }

    private static class SubTest {
        private String gzOption;
        boolean needCheckSuccess;

        public SubTest(String gzOpt, boolean checkSuccess) {
            gzOption = gzOpt;
            needCheckSuccess = checkSuccess;
        }

        public String getGzOption() { return gzOption; }
        public boolean needCheckSuccess() { return needCheckSuccess; }
    }
}
