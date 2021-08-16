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

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import jdk.test.lib.Utils;
import jdk.test.lib.JDKToolLauncher;
import jdk.test.lib.JDKToolFinder;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.SA.SATestUtils;

/**
 * This is a framework to run 'jhsdb clhsdb' commands.
 * See open/test/hotspot/jtreg/serviceability/sa/ClhsdbLongConstant.java for
 * an example of how to write a test.
 */

public class ClhsdbLauncher {

    private Process toolProcess;

    public ClhsdbLauncher() {
        toolProcess = null;
    }

    /**
     *
     * Launches 'jhsdb clhsdb' and attaches to the Lingered App process.
     * @param lingeredAppPid  - pid of the Lingered App or one its sub-classes.
     */
    private void attach(long lingeredAppPid)
        throws IOException {
        JDKToolLauncher launcher = JDKToolLauncher.createUsingTestJDK("jhsdb");
        launcher.addVMArgs(Utils.getTestJavaOpts());
        launcher.addToolArg("clhsdb");
        if (lingeredAppPid != -1) {
            launcher.addToolArg("--pid=" + Long.toString(lingeredAppPid));
            System.out.println("Starting clhsdb against " + lingeredAppPid);
        }

        ProcessBuilder processBuilder = SATestUtils.createProcessBuilder(launcher);
        toolProcess = processBuilder.start();
    }

    /**
     *
     * Launches 'jhsdb clhsdb' and loads a core file.
     * @param coreFileName - Name of the corefile to be loaded.
     */
    private void loadCore(String coreFileName)
        throws IOException {

        JDKToolLauncher launcher = JDKToolLauncher.createUsingTestJDK("jhsdb");
        launcher.addVMArgs(Utils.getTestJavaOpts());
        launcher.addToolArg("clhsdb");
        launcher.addToolArg("--core=" + coreFileName);
        launcher.addToolArg("--exe=" + JDKToolFinder.getTestJDKTool("java"));
        System.out.println("Starting clhsdb against corefile " + coreFileName +
                           " and exe " + JDKToolFinder.getTestJDKTool("java"));

        ProcessBuilder processBuilder = new ProcessBuilder(launcher.getCommand());
        toolProcess = processBuilder.start();
    }

    /**
     *
     * Runs 'jhsdb clhsdb' commands and checks for expected and unexpected strings.
     * @param commands  - clhsdb commands to execute.
     * @param expectedStrMap - Map of expected strings per command which need to
     *                         be checked in the output of the command.
     * @param unExpectedStrMap - Map of unexpected strings per command which should
     *                           not be present in the output of the command.
     * @return Output of the commands as a String.
     */
    private String runCmd(List<String> commands,
                          Map<String, List<String>> expectedStrMap,
                          Map<String, List<String>> unExpectedStrMap)
        throws IOException, InterruptedException {
        String output;

        if (commands == null) {
            throw new RuntimeException("CLHSDB command must be provided\n");
        }

        // We want to execute clhsdb "echo" and "verbose" commands before the
        // requested commands. We can't just issue these commands separately
        // because code below won't work correctly if all executed commands are
        // not in the commands list. Since the commands list is immutable, we
        // need to allocate a mutable one that we can add the extra commands too.
        List<String> savedCommands = commands;
        commands = new java.util.LinkedList<String>();

        // Enable echoing of all commands so we see them in the output.
        commands.add("echo true");

        // Enable verbose exception tracing so we see the full exception backtrace
        // when there is a failure.
        commands.add("verbose true");

        // Now add all the original commands after the "echo" and "verbose" commands.
        commands.addAll(savedCommands);

        try (OutputStream out = toolProcess.getOutputStream()) {
            for (String cmd : commands) {
                out.write((cmd + "\n").getBytes());
            }
            out.write("quit\n".getBytes());
            out.flush();
        }

        OutputAnalyzer oa = new OutputAnalyzer(toolProcess);
        try {
            toolProcess.waitFor();
        } catch (InterruptedException ie) {
            toolProcess.destroyForcibly();
            throw new Error("Problem awaiting the child process: " + ie);
        }

        oa.shouldHaveExitValue(0);
        output = oa.getOutput();
        System.out.println("Output: ");
        System.out.println(output);

        // -Xcheck:jni might be set via TEST_VM_OPTS. Make sure there are no warnings.
        oa.shouldNotMatch("^WARNING: JNI local refs:.*$");
        oa.shouldNotMatch("^WARNING in native method:.*$");
        // This will detect most SA failures, including during the attach.
        oa.shouldNotMatch("^sun.jvm.hotspot.debugger.DebuggerException:.*$");
        // This will detect unexpected exceptions, like NPEs and asserts, that are caught
        // by sun.jvm.hotspot.CommandProcessor.
        oa.shouldNotMatch("^Error: .*$");

        String[] parts = output.split("hsdb>");
        for (String cmd : commands) {
            int index = commands.indexOf(cmd) + 1;
            OutputAnalyzer out = new OutputAnalyzer(parts[index]);
            out.shouldNotMatch("Unrecognized command.");

            if (expectedStrMap != null) {
                List<String> expectedStr = expectedStrMap.get(cmd);
                if (expectedStr != null) {
                    for (String exp : expectedStr) {
                        out.shouldMatch(exp);
                    }
                }
            }

            if (unExpectedStrMap != null) {
                List<String> unExpectedStr = unExpectedStrMap.get(cmd);
                if (unExpectedStr != null) {
                    for (String unExp : unExpectedStr) {
                        out.shouldNotMatch(unExp);
                    }
                }
            }
        }
        return output;
    }

    /**
     *
     * Launches 'jhsdb clhsdb', attaches to the Lingered App, executes the commands,
     * checks for expected and unexpected strings.
     * @param lingeredAppPid  - pid of the Lingered App or one its sub-classes.
     * @param commands  - clhsdb commands to execute.
     * @param expectedStrMap - Map of expected strings per command which need to
     *                         be checked in the output of the command.
     * @param unExpectedStrMap - Map of unexpected strings per command which should
     *                           not be present in the output of the command.
     * @return Output of the commands as a String.
     */
    public String run(long lingeredAppPid,
                      List<String> commands,
                      Map<String, List<String>> expectedStrMap,
                      Map<String, List<String>> unExpectedStrMap)
        throws Exception {

        SATestUtils.skipIfCannotAttach(); // throws SkippedException if attach not expected to work.
        attach(lingeredAppPid);
        return runCmd(commands, expectedStrMap, unExpectedStrMap);
    }

    /**
     *
     * Launches 'jhsdb clhsdb', loads a core file, executes the commands,
     * checks for expected and unexpected strings.
     * @param coreFileName - Name of the core file to be debugged.
     * @param commands  - clhsdb commands to execute.
     * @param expectedStrMap - Map of expected strings per command which need to
     *                         be checked in the output of the command.
     * @param unExpectedStrMap - Map of unexpected strings per command which should
     *                           not be present in the output of the command.
     * @return Output of the commands as a String.
     */
    public String runOnCore(String coreFileName,
                            List<String> commands,
                            Map<String, List<String>> expectedStrMap,
                            Map<String, List<String>> unExpectedStrMap)
        throws IOException, InterruptedException {

        loadCore(coreFileName);
        return runCmd(commands, expectedStrMap, unExpectedStrMap);
    }
}
