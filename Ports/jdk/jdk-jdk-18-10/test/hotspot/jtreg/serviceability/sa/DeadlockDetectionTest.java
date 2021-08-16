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

/**
 * @test
 * @summary Test deadlock detection
 * @requires vm.hasSA
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 * @modules java.management
 * @run driver DeadlockDetectionTest
 */

import java.util.stream.Collectors;

import jdk.test.lib.apps.LingeredApp;
import jdk.test.lib.apps.LingeredAppWithDeadlock;
import jdk.test.lib.JDKToolLauncher;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.SA.SATestUtils;
import jdk.test.lib.Utils;

import jtreg.SkippedException;

public class DeadlockDetectionTest {

    private static LingeredAppWithDeadlock theApp = null;

    private static OutputAnalyzer jstack(String... toolArgs) throws Exception {
        JDKToolLauncher launcher = JDKToolLauncher.createUsingTestJDK("jhsdb");
        launcher.addVMArgs(Utils.getTestJavaOpts());
        launcher.addToolArg("jstack");
        if (toolArgs != null) {
            for (String toolArg : toolArgs) {
                launcher.addToolArg(toolArg);
            }
        }

        ProcessBuilder processBuilder = SATestUtils.createProcessBuilder(launcher);
        System.out.println(processBuilder.command().stream().collect(Collectors.joining(" ")));
        OutputAnalyzer output = ProcessTools.executeProcess(processBuilder);
        System.out.println(output.getOutput());

        return output;
    }

    public static void main(String[] args) throws Exception {
        SATestUtils.skipIfCannotAttach(); // throws SkippedException if attach not expected to work.
        System.out.println("Starting DeadlockDetectionTest");

        if (!LingeredApp.isLastModifiedWorking()) {
            // Exact behaviour of the test depends on operating system and the test nature,
            // so just print the warning and continue
            System.err.println("Warning! Last modified time doesn't work.");
        }

        try {
            theApp = new LingeredAppWithDeadlock();
            LingeredApp.startApp(theApp, "-XX:+UsePerfData");
            OutputAnalyzer output = jstack("--pid", Long.toString(theApp.getPid()));
            System.out.println(output.getOutput());

            if (output.getExitValue() == 3) {
                throw new SkippedException("Test can't run for some reason");
            } else {
                output.shouldHaveExitValue(0);
                output.shouldContain("Found a total of 1 deadlock.");
            }
        } finally {
            LingeredApp.stopApp(theApp);
        }
    }
}
