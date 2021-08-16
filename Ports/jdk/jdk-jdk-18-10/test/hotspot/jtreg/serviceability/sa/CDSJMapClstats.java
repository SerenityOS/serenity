/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8204308
 * @summary Test the jhsdb jmap -clstats command with CDS enabled
 * @requires vm.hasSA & vm.cds
 * @library /test/lib
 * @run driver/timeout=2400 CDSJMapClstats
 */

import java.util.stream.Collectors;

import jdk.test.lib.Utils;
import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.cds.CDSOptions;
import jdk.test.lib.apps.LingeredApp;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.JDKToolLauncher;
import jdk.test.lib.SA.SATestUtils;

public class CDSJMapClstats {

    private static void runClstats(long lingeredAppPid) throws Exception {

        JDKToolLauncher launcher = JDKToolLauncher.createUsingTestJDK("jhsdb");
        launcher.addVMArgs(Utils.getTestJavaOpts());
        launcher.addToolArg("jmap");
        launcher.addToolArg("--clstats");
        launcher.addToolArg("--pid");
        launcher.addToolArg(Long.toString(lingeredAppPid));

        ProcessBuilder processBuilder = SATestUtils.createProcessBuilder(launcher);
        System.out.println(
            processBuilder.command().stream().collect(Collectors.joining(" ")));

        OutputAnalyzer SAOutput = ProcessTools.executeProcess(processBuilder);
        System.out.println(SAOutput.getOutput());
        SAOutput.shouldHaveExitValue(0);
        SAOutput.shouldContain("BootClassLoader");
    }


    public static void main(String[] args) throws Exception {
        System.out.println("Starting CDSJMapClstats test");
        SATestUtils.skipIfCannotAttach(); // throws SkippedException if attach not expected to work.
        String sharedArchiveName = "ArchiveForCDSJMapClstats.jsa";
        LingeredApp theApp = null;

        try {
            CDSOptions opts = (new CDSOptions()).setArchiveName(sharedArchiveName);
            CDSTestUtils.createArchiveAndCheck(opts);

            theApp = LingeredApp.startApp(
                "-XX:+UnlockDiagnosticVMOptions",
                "-XX:SharedArchiveFile=" + sharedArchiveName,
                "-Xshare:auto");
            System.out.println("Started LingeredApp with pid " + theApp.getPid());
            runClstats(theApp.getPid());
        } catch (Exception ex) {
            throw new RuntimeException("Test ERROR " + ex, ex);
        } finally {
            LingeredApp.stopApp(theApp);
        }
        System.out.println("Test PASSED");
    }
}
