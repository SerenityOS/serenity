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

import java.util.Arrays;
import java.util.List;

import jdk.test.lib.Utils;
import jdk.test.lib.apps.LingeredApp;
import jdk.test.lib.JDKToolLauncher;
import jdk.test.lib.Platform;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.SA.SATestUtils;

/**
 * @test
 * @bug 8242165 8242162
 * @summary Test "jhsdb jinfo --sysprops", "jinfo -sysprops", and clhsdb "sysprops" commands
 * @requires vm.hasSA
 * @library /test/lib
 * @run driver TestSysProps
 */

public class TestSysProps {
    public static void findProp(String[] propLines, String propname, String cmdName) {
        boolean found = false;
        for (String propLine : propLines) {
            if (propLine.startsWith(propname)) {
                found = true;
                break;
            }
        }
        if (!found) {
            throw new RuntimeException("Could not find property in " + cmdName + " output: " + propname);
        }
    }

    public static void countProps(String[] propLines, int expectedCount, String cmdName) {
        int numProps = 0;
        for (String propLine : propLines) {
            // Ignore the debug output lines (they start with '[' and printed
            // in the output when the test is run with -Xlog VM option).
            if (!propLine.startsWith("[") && propLine.indexOf("=") != -1) {
                numProps++;
            }
        }
        if (numProps != expectedCount) {
            throw new RuntimeException("Wrong number of " + cmdName + " properties: " + numProps);
        }
    }

    public static void main (String... args) throws Exception {
        SATestUtils.skipIfCannotAttach(); // throws SkippedException if attach not expected to work.
        LingeredAppSysProps app = null;

        try {
            app = new LingeredAppSysProps();
            LingeredApp.startApp(app);
            System.out.println("Started LingeredAppSysProps with pid " + app.getPid());

            // Get properties using the SA version of jinfo

            JDKToolLauncher jhsdbLauncher = JDKToolLauncher.createUsingTestJDK("jhsdb");
            jhsdbLauncher.addVMArgs(Utils.getTestJavaOpts());
            jhsdbLauncher.addToolArg("jinfo");
            jhsdbLauncher.addToolArg("--sysprops");
            jhsdbLauncher.addToolArg("--pid");
            jhsdbLauncher.addToolArg(Long.toString(app.getPid()));

            ProcessBuilder jhsdbPb = SATestUtils.createProcessBuilder(jhsdbLauncher);
            System.out.println("> " + ProcessTools.getCommandLine(jhsdbPb));
            Process jhsdb = jhsdbPb.start();
            OutputAnalyzer jhsdbOut = new OutputAnalyzer(jhsdb);

            jhsdb.waitFor();

            System.out.println(jhsdbOut.getStdout());
            System.err.println(jhsdbOut.getStderr());

            jhsdbOut.shouldMatch("Debugger attached successfully.");

            // Get the properties using the Attach API version of jinfo

            JDKToolLauncher jinfoLauncher = JDKToolLauncher.createUsingTestJDK("jinfo");
            jinfoLauncher.addVMArgs(Utils.getTestJavaOpts());
            jinfoLauncher.addToolArg("-sysprops");
            jinfoLauncher.addToolArg(Long.toString(app.getPid()));

            List<String> cmdStringList = Arrays.asList(jinfoLauncher.getCommand());
            ProcessBuilder jinfoPb = new ProcessBuilder(cmdStringList);
            System.out.println("> " + ProcessTools.getCommandLine(jinfoPb));
            Process jinfo = jinfoPb.start();
            OutputAnalyzer jinfoOut = new OutputAnalyzer(jinfo);

            jinfo.waitFor();

            System.out.println(jinfoOut.getStdout());
            System.err.println(jinfoOut.getStderr());

            jinfoOut.shouldMatch("Java System Properties:");

            // Get the properties using "clhsdb sysprops".

            System.out.println("clhsdb sysprops output:");
            ClhsdbLauncher test = new ClhsdbLauncher();
            List<String> cmds = List.of("sysprops");
            String output = test.run(app.getPid(), cmds, null, null);
            OutputAnalyzer clhsdbOut = new OutputAnalyzer(output);
            clhsdbOut.shouldMatch("java.specification.version");

            // Get the output from LingeredAppSysProps, which has printed all the
            // system properties using java.

            app.stopApp();
            System.out.println("LingeredAppSysProps output:");
            System.out.println(app.getOutput().getStdout());
            System.err.println(app.getOutput().getStderr());
            OutputAnalyzer appOut = new OutputAnalyzer(app.getOutput().getStdout());
            appOut.shouldMatch("-- listing properties --");

            // Now make sure the above 3 outputs all contain the same list of properties.
            // We don't compare the property values since sometimes they get truncated
            // in one list but not the other, and also special characters are not always
            // handled the same.

            String[] jhsdbLines = jhsdbOut.getStdout().split("\\R");
            String[] jinfoLines = jinfoOut.getStdout().split("\\R");
            String[] clhsdbLines = clhsdbOut.getStdout().split("\\R");
            String[] appLines   = app.getOutput().getStdout().split("\\R");
            int numAppProps = 0;
            boolean foundStartOfList = false;
            for (String appProp : appLines) {
                // Skip any output that occurs before the first property
                if (!foundStartOfList) {
                    if (appProp.indexOf("-- listing properties --") != -1) {
                        foundStartOfList = true;
                    }
                    continue;
                }

                // Find the next property in the app output
                int idx = appProp.indexOf("=");
                if (idx == -1) continue; // This line does not contain a property
                String propname = appProp.substring(0, idx);
                System.out.println("Found prop " + propname);
                numAppProps++;

                // Make sure we can find the property in each of the other 3 lists
                findProp(jhsdbLines, propname, "jhsdb jinfo");
                findProp(jinfoLines, propname, "jinfo");
                findProp(clhsdbLines, propname, "clhsdb sysprops");
            }

            // Make sure we found a reasonable number of properties in the app output. It should
            // be close to 45, but the spec only mandates 29, so this is what we check for. The
            // main reason for this check is just to make sure something didn't go drastically
            // wrong, resulting in no properties in the app output, meaning that no comparison
            // was actually done with the other sets of output.
            System.out.println(numAppProps + " properties found.");
            if (numAppProps < 29) {
                throw new RuntimeException("Did not find at least 29 properties: " + numAppProps);
            }

            // Make sure each list has the same number of properties.
            countProps(jhsdbLines, numAppProps, "jhsdb jinfo");
            countProps(jinfoLines, numAppProps, "jinfo");
            countProps(clhsdbLines, numAppProps, "clhsdb sysprops");

            System.out.println("Test Completed");
        } finally {
            LingeredApp.stopApp(app);
        }
    }
}
