/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2021 NTT DATA.
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

import java.io.PrintStream;

import jdk.test.lib.JDKToolLauncher;
import jdk.test.lib.apps.LingeredApp;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.SA.SATestUtils;

import jtreg.SkippedException;

/**
 * @test
 * @bug 8265505
 * @summary Test clhsdb command which should be run on debugd server
 * @requires vm.hasSA
 * @requires os.family != "windows"
 * @library /test/lib
 * @run driver RunCommandOnServerTest
 */

public class RunCommandOnServerTest {

    public static void main(String[] args) throws Exception {
        SATestUtils.skipIfCannotAttach(); // throws SkippedException if attach not expected to work.
        SATestUtils.validateSADebugDPrivileges();

        LingeredApp theApp = null;
        DebugdUtils debugd = null;
        try {
            theApp = LingeredApp.startApp();
            System.out.println("Started LingeredApp with pid " + theApp.getPid());
            debugd = new DebugdUtils();
            debugd.attach(theApp.getPid());

            JDKToolLauncher jhsdbLauncher = JDKToolLauncher.createUsingTestJDK("jhsdb");
            jhsdbLauncher.addToolArg("clhsdb");
            jhsdbLauncher.addToolArg("--connect");
            jhsdbLauncher.addToolArg("localhost");

            Process jhsdb = (SATestUtils.createProcessBuilder(jhsdbLauncher)).start();
            OutputAnalyzer out = new OutputAnalyzer(jhsdb);

            try (PrintStream console = new PrintStream(jhsdb.getOutputStream(), true)) {
                console.println("echo true");
                console.println("verbose true");
                console.println("findsym gHotSpotVMTypes");
                console.println("quit");
            }

            jhsdb.waitFor();
            System.out.println(out.getStdout());
            System.err.println(out.getStderr());

            out.stderrShouldBeEmptyIgnoreDeprecatedWarnings();
            out.shouldMatch("^0x[0-9a-f]+: .+/libjvm\\.(so|dylib) \\+ 0x[0-9a-f]+$");
            out.shouldHaveExitValue(0);

            // This will detect most SA failures, including during the attach.
            out.shouldNotMatch("^sun.jvm.hotspot.debugger.DebuggerException:.*$");
            // This will detect unexpected exceptions, like NPEs and asserts, that are caught
            // by sun.jvm.hotspot.CommandProcessor.
            out.shouldNotMatch("^Error: .*$");
        } catch (SkippedException se) {
            throw se;
        } catch (Exception ex) {
            throw new RuntimeException("Test ERROR " + ex, ex);
        } finally {
            if (debugd != null) {
                debugd.detach();
            }
            LingeredApp.stopApp(theApp);
        }
        System.out.println("Test PASSED");
    }
}
