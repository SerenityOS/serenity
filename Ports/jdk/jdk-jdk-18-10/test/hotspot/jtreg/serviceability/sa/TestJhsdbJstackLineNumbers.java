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

import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.TreeSet;

import jdk.test.lib.apps.LingeredApp;
import jdk.test.lib.JDKToolLauncher;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.SA.SATestUtils;

/**
 * @test
 * @requires vm.hasSA
 * @requires os.arch=="amd64" | os.arch=="x86_64"
 * @requires os.family=="windows" | os.family == "linux" | os.family == "mac"
 * @requires vm.flagless
 * @library /test/lib
 * @run driver TestJhsdbJstackLineNumbers
 */

/*
 * This test makes sure that SA gets the most accurate value for the line number of
 * the current (topmost) frame. Many SA ports just rely on frame->bcp, but it is
 * usually out of date since the current BCP is cached in a register and only flushed
 * to frame->bcp when the register is needed for something else. Therefore SA ports
 * need to fetch the register that the BCP is stored in and see if it is valid,
 * and only defer to frame->bcp if it is not valid.
 *
 * The test works by spawning a process that sits in a 10 line loop in the busywork() method,
 * all while the main test does repeated jstacks on the process. The expectation is
 * that at least 5 of the lines in the busywork() loop will eventually show up in at
 * least one of the jstack runs.
 */

class LingeredAppWithBusyWork extends LingeredApp {
    static volatile boolean stop = false;

    private static int busywork(int[] x) {
        int i = 0;
        while (!stop) {
            i = x[0];
            i += x[1];
            i += x[2];
            i += x[3];
            i += x[4];
            i += x[5];
            i += x[6];
            i += x[7];
        }
        return i;
    }

    public static void main(String... args) {
        Thread t = new Thread(() -> {
            busywork(new int[]{0,1,2,3,4,5,6,7});
        });

        try {
            t.setName("BusyWorkThread");
            t.start();
            LingeredApp.main(args);
            stop = true;
            t.join();
        } catch (InterruptedException e) {
        }
    }
}

public class TestJhsdbJstackLineNumbers {
    // This is the number of lines in the busywork main loop
    static final int TOTAL_BUSYWORK_LOOP_LINES = 10;
    // The minimum number of lines that we must at some point see in the jstack output
    static final int MIN_BUSYWORK_LOOP_LINES = 5;

    static final int MAX_NUMBER_OF_JSTACK_RUNS = 25;

    private static OutputAnalyzer runJstack(String... toolArgs) throws Exception {
        JDKToolLauncher launcher = JDKToolLauncher.createUsingTestJDK("jhsdb");
        launcher.addToolArg("jstack");
        if (toolArgs != null) {
            for (String toolArg : toolArgs) {
                launcher.addToolArg(toolArg);
            }
        }

        ProcessBuilder processBuilder = SATestUtils.createProcessBuilder(launcher);
        System.out.println(processBuilder.command().stream().collect(Collectors.joining(" ")));
        OutputAnalyzer output = ProcessTools.executeProcess(processBuilder);

        return output;
    }

    public static void runTest(long pid) throws Exception {
        // Keep running jstack until the target app is in the "busywork" method.
        String output;
        int maxRetries = 5;
        do {
            if (maxRetries-- == 0) {
                throw new RuntimeException("Failed: LingeredAppWithBusyWork never entered busywork() method.");
            }
            OutputAnalyzer jstackOut = runJstack("--pid", Long.toString(pid));
            output = jstackOut.getOutput();
            System.out.println(output);
        } while (!output.contains("busywork"));

        // This is for tracking all the line numbers in busywork() that we've seen.
        // Since it is a TreeSet, it will always be sorted and have no duplicates.
        TreeSet<Integer> lineNumbersSeen = new TreeSet<Integer>();

        // Keep running jstack until we see a sufficient number of different line
        // numbers in the busywork() loop.
        for (int x = 0; x < MAX_NUMBER_OF_JSTACK_RUNS; x++) {
            OutputAnalyzer jstackOut = runJstack("--pid", Long.toString(pid));
            output = jstackOut.getOutput();
            // The stack dump will have a line that looks like:
            //   - LingeredAppWithBusyWork.busywork(int[]) @bci=32, line=74 (Interpreted frame)
            // We want to match on the line number, "74" in this example. We also match on the
            // full line just so we can print it out.
            Pattern LINE_PATTERN = Pattern.compile(
                ".+(- LingeredAppWithBusyWork.busywork\\(int\\[\\]\\) \\@bci\\=[0-9]+, line\\=([0-9]+) \\(Interpreted frame\\)).+", Pattern.DOTALL);
            Matcher matcher = LINE_PATTERN.matcher(output);
            if (matcher.matches()) {
                System.out.println(matcher.group(1)); // print matching stack trace line
                int lineNum = Integer.valueOf(matcher.group(2)); // get matching line number
                lineNumbersSeen.add(lineNum);
                if (lineNumbersSeen.size() == MIN_BUSYWORK_LOOP_LINES) {
                    // We're done!
                    System.out.println("Found needed line numbers after " + (x+1) + " iterations");
                    break;
                }
            } else {
                System.out.println("failed to match");
                System.out.println(output);
                continue; // Keep trying. This can happen on rare occasions when the stack cannot be determined.
            }
        }
        System.out.println("Found Line Numbers: " + lineNumbersSeen);

        // Make sure we saw the minimum required number of lines in busywork().
        if (lineNumbersSeen.size() < MIN_BUSYWORK_LOOP_LINES) {
            throw new RuntimeException("Failed: Didn't find enough line numbers: " + lineNumbersSeen);
        }

        // Make sure the distance between the lowest and highest line numbers seen
        // is not more than the number of lines in the busywork() loop.
        if (lineNumbersSeen.last() - lineNumbersSeen.first() > TOTAL_BUSYWORK_LOOP_LINES) {
            throw new RuntimeException("Failed: lowest and highest line numbers are too far apart: " + lineNumbersSeen);
        }

    }

    public static void main(String... args) throws Exception {
        SATestUtils.skipIfCannotAttach(); // throws SkippedException if attach not expected to work.

        LingeredApp theApp = null;
        try {
            // Launch the LingeredAppWithBusyWork process with the busywork() loop
            theApp = new LingeredAppWithBusyWork();
            LingeredApp.startAppExactJvmOpts(theApp, "-Xint");
            System.out.println("Started LingeredApp with pid " + theApp.getPid());

            runTest(theApp.getPid());
        } finally {
            LingeredApp.stopApp(theApp);
            System.out.println("LingeredAppWithBusyWork finished");
        }
    }
}
