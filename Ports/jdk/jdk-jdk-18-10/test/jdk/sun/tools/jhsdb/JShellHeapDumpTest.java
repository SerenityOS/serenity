/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8225715
 * @requires vm.hasSA
 * @library /test/lib
 * @compile JShellHeapDumpTest.java
 * @run main/timeout=240 JShellHeapDumpTest
 */

import static jdk.test.lib.Asserts.assertTrue;

import java.io.IOException;
import java.io.File;
import java.util.List;
import java.util.Arrays;
import java.util.Map;

import jdk.test.lib.Utils;
import jdk.test.lib.hprof.parser.HprofReader;
import jdk.test.lib.JDKToolLauncher;
import jdk.test.lib.JDKToolFinder;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.SA.SATestUtils;

import jdk.jshell.JShell;

public class JShellHeapDumpTest {

    static Process jShellProcess;
    static boolean doSleep = true; // By default do a short sleep when app starts up

    public static void launch(String expectedMessage, List<String> toolArgs)
        throws IOException {

        try {
            launchJshell();
            long jShellPID = jShellProcess.pid();

            System.out.println("Starting " + toolArgs.get(0) + " against " + jShellPID);
            JDKToolLauncher launcher = JDKToolLauncher.createUsingTestJDK("jhsdb");
            launcher.addVMArgs(Utils.getFilteredTestJavaOpts("-Xcomp"));

            for (String cmd : toolArgs) {
                launcher.addToolArg(cmd);
            }

            launcher.addToolArg("--pid=" + Long.toString(jShellPID));

            ProcessBuilder processBuilder = SATestUtils.createProcessBuilder(launcher);
            OutputAnalyzer output = ProcessTools.executeProcess(processBuilder);
            System.out.println("jhsdb jmap stdout:");
            System.out.println(output.getStdout());
            System.out.println("jhsdb jmap stderr:");
            System.out.println(output.getStderr());
            System.out.println("###### End of all output:");
            output.shouldHaveExitValue(0);
        } catch (Exception ex) {
            throw new RuntimeException("Test ERROR " + ex, ex);
        } finally {
            if (jShellProcess.isAlive()) {
                System.out.println("Destroying jshell");
                jShellProcess.destroy();
                System.out.println("Jshell destroyed");
            } else {
                System.out.println("Jshell not alive");
            }
        }
    }

    public static void launch(String expectedMessage, String... toolArgs)
        throws IOException {

        launch(expectedMessage, Arrays.asList(toolArgs));
    }

    public static void printStackTraces(String file) throws IOException {
        try {
            String output = HprofReader.getStack(file, 0);
            // We only require JShellToolProvider to be in the output if we did the
            // short sleep. If we did not, the java process may not have executed far
            // enough along to even start the main thread.
            if (doSleep && !output.contains("JShellToolProvider")) {
                throw new RuntimeException("'JShellToolProvider' missing from stdout/stderr");
            }
        } catch (Exception ex) {
            throw new RuntimeException("Test ERROR " + ex, ex);
        }
    }

    public static void testHeapDump() throws IOException {
        File hprofFile = new File("jhsdb.jmap.heap." +
                             System.currentTimeMillis() + ".hprof");
        if (hprofFile.exists()) {
            hprofFile.delete();
        }

        launch("heap written to", "jmap",
               "--binaryheap", "--dumpfile=" + hprofFile.getAbsolutePath());

        assertTrue(hprofFile.exists() && hprofFile.isFile(),
                   "Could not create dump file " + hprofFile.getAbsolutePath());

        printStackTraces(hprofFile.getAbsolutePath());

        System.out.println("hprof file size: " + hprofFile.length());
        hprofFile.delete();
    }

    public static void launchJshell() throws IOException {
        System.out.println("Starting Jshell");
        long startTime = System.currentTimeMillis();
        try {
            ProcessBuilder pb = new ProcessBuilder(JDKToolFinder.getTestJDKTool("jshell"));
            jShellProcess = ProcessTools.startProcess("JShell", pb,
                                                      s -> {  // warm-up predicate
                                                          return s.contains("Welcome to JShell");
                                                      });
        } catch (Exception ex) {
            throw new RuntimeException("Test ERROR " + ex, ex);
        }

        long elapsedTime = System.currentTimeMillis() - startTime;
        System.out.println("Jshell Started in " + elapsedTime + "ms");

        // Give jshell a chance to fully start up. This makes SA more stable for the jmap dump.
        try {
            if (doSleep) {
                Thread.sleep(2000);
            }
        } catch (Exception e) {
        }
    }

    public static void main(String[] args) throws Exception {
        SATestUtils.skipIfCannotAttach(); // throws SkippedException if attach not expected to work.
        if (args.length == 1) {
            if (args[0].equals("nosleep")) {
                doSleep = false;
            } else {
                throw new RuntimeException("Invalid arg: " + args[0]);
            }
        } else if (args.length != 0) {
            throw new RuntimeException("Too many args: " + args.length);
        }
        testHeapDump();

        // The test throws RuntimeException on error.
        // IOException is thrown if Jshell can't start because of some bad
        // environment condition
        System.out.println("Test PASSED");
    }
}
