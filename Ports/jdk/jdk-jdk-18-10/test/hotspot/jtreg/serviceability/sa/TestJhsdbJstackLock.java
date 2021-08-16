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

import jdk.test.lib.apps.LingeredApp;
import jdk.test.lib.JDKToolLauncher;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.SA.SATestUtils;
import jdk.test.lib.Utils;

/**
 * @test
 * @requires vm.hasSA
 * @library /test/lib
 * @run driver TestJhsdbJstackLock
 */

public class TestJhsdbJstackLock {

    public static void main (String... args) throws Exception {
        SATestUtils.skipIfCannotAttach(); // throws SkippedException if attach not expected to work.
        LingeredApp app = null;

        try {
            app = new LingeredAppWithLock();
            LingeredApp.startApp(app);
            System.out.println ("Started LingeredApp with pid " + app.getPid());

            JDKToolLauncher launcher = JDKToolLauncher.createUsingTestJDK("jhsdb");
            launcher.addVMArgs(Utils.getFilteredTestJavaOpts("-showversion"));
            launcher.addToolArg("jstack");
            launcher.addToolArg("--pid");
            launcher.addToolArg(Long.toString(app.getPid()));

            ProcessBuilder pb = SATestUtils.createProcessBuilder(launcher);
            Process jhsdb = pb.start();
            OutputAnalyzer out = new OutputAnalyzer(jhsdb);

            jhsdb.waitFor();

            System.out.println(out.getStdout());
            System.err.println(out.getStderr());

            out.shouldMatch("^\\s+- locked <0x[0-9a-f]+> \\(a java\\.lang\\.Class for LingeredAppWithLock\\)$");
            out.shouldMatch("^\\s+- waiting to lock <0x[0-9a-f]+> \\(a java\\.lang\\.Class for LingeredAppWithLock\\)$");
            out.shouldMatch("^\\s+- locked <0x[0-9a-f]+> \\(a java\\.lang\\.Thread\\)$");
            out.shouldMatch("^\\s+- locked <0x[0-9a-f]+> \\(a java\\.lang\\.Class for int\\)$");

            out.stderrShouldBeEmptyIgnoreDeprecatedWarnings();

            System.out.println("Test Completed");
        } finally {
            LingeredApp.stopApp(app);
        }
    }
}
