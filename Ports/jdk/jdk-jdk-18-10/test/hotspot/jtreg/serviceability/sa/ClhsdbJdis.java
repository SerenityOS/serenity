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

/**
 * @test
 * @bug 8193124
 * @summary Test the clhsdb 'jdis' command
 * @requires vm.hasSA
 * @library /test/lib
 * @run main/othervm ClhsdbJdis
 */

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import jdk.test.lib.apps.LingeredApp;
import jtreg.SkippedException;

public class ClhsdbJdis {

    public static void main(String[] args) throws Exception {
        LingeredApp theApp = null;
        System.out.println("Starting the ClhsdbJdis test");

        try {
            ClhsdbLauncher test = new ClhsdbLauncher();
            theApp = LingeredApp.startApp();
            System.out.println("Started LingeredApp with pid " + theApp.getPid());

            // Run 'jstack -v' command to get the Method Address
            List<String> cmds = List.of("jstack -v");
            String output = test.run(theApp.getPid(), cmds, null, null);

            // Test the 'jdis' command passing in the address obtained from
            // the 'jstack -v' command
            cmds = new ArrayList<String>();

            String cmdStr = null;
            String[] parts = output.split("LingeredApp.steadyState");
            String[] tokens = parts[1].split(" ");
            for (String token : tokens) {
                if (token.contains("Method")) {
                    String[] address = token.split("=");
                    // address[1] represents the address of the Method
                    cmdStr = "jdis " + address[1];
                    cmds.add(cmdStr);
                    break;
                }
            }

            Map<String, List<String>> expStrMap = new HashMap<>();
            expStrMap.put(cmdStr, List.of(
                    "private static void steadyState\\(java\\.lang\\.Object\\)",
                    "Holder Class",
                    "public class jdk.test.lib.apps.LingeredApp @",
                    "public class jdk\\.test\\.lib\\.apps\\.LingeredApp @",
                    "Bytecode",
                    "line bci   bytecode",
                    "Exception Table",
                    "start bci end bci handler bci catch type",
                    "Constant Pool of \\[public class jdk\\.test\\.lib\\.apps\\.LingeredApp @"));

            test.run(theApp.getPid(), cmds, expStrMap, null);
        } catch (SkippedException e) {
            throw e;
        } catch (Exception ex) {
            throw new RuntimeException("Test ERROR " + ex, ex);
        } finally {
            LingeredApp.stopApp(theApp);
        }
        System.out.println("Test PASSED");
    }
}
