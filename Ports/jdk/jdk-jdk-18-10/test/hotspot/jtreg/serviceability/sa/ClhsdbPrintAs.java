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
 * @bug 8192985
 * @summary Test the clhsdb 'printas' command
 * @requires vm.hasSA
 * @library /test/lib
 * @run main/othervm ClhsdbPrintAs
 */

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import jdk.test.lib.apps.LingeredApp;
import jtreg.SkippedException;

public class ClhsdbPrintAs {

    public static void main(String[] args) throws Exception {
        System.out.println("Starting the ClhsdbPrintAs test");

        LingeredApp theApp = null;
        try {
            ClhsdbLauncher test = new ClhsdbLauncher();
            theApp = LingeredApp.startApp();
            System.out.println("Started LingeredApp with pid " + theApp.getPid());

            // Run the 'jstack -v' command to get the address of a the Method*
            // representing LingeredApp.steadyState
            List<String> cmds = List.of("jstack -v");
            Map<String, List<String>> expStrMap;

            String jstackOutput = test.run(theApp.getPid(), cmds, null, null);

            String[] snippets = jstackOutput.split("LingeredApp.steadyState");
            String addressString = null;

            String[] tokens = snippets[1].split("Method\\*=");
            String[] words = tokens[1].split(" ");
            addressString = words[0];

            cmds = new ArrayList<String>();
            expStrMap = new HashMap<>();

            String cmd = "printas Method " + addressString;
            cmds.add(cmd);
            expStrMap.put(cmd, List.of
                ("ConstMethod", "MethodCounters", "Method::_access_flags"));

            // Run the printas Method <addr> command to obtain the address
            // of ConstMethod*
            String methodDetailsOutput = test.run(theApp.getPid(), cmds, expStrMap, null);
            snippets = methodDetailsOutput.split("ConstMethod*");

            tokens = snippets[1].split(" ");
            for (String token : tokens) {
                if (token.contains("0x")) {
                    addressString = token.replace("\n", "");
                    break;
                }
            }

            cmds = new ArrayList<String>();
            expStrMap = new HashMap<>();

            cmd = "printas ConstMethod " + addressString;
            cmds.add(cmd);
            expStrMap.put(cmd, List.of
                ("ConstantPool", "_max_locals", "_flags"));

            // Run the printas constMethod <addr> command to obtain the address
            // of ConstantPool*
            String constMethodDetailsOutput = test.run(theApp.getPid(), cmds, expStrMap, null);
            snippets = constMethodDetailsOutput.split("ConstantPool*");

            tokens = snippets[1].split(" ");
            for (String token : tokens) {
                if (token.contains("0x")) {
                    addressString = token.replace("\n", "");
                    break;
                }
            }

            cmds = new ArrayList<String>();
            expStrMap = new HashMap<>();

            cmd = "printas ConstantPool " + addressString;
            cmds.add(cmd);
            expStrMap.put(cmd, List.of
                ("ConstantPoolCache", "_pool_holder", "InstanceKlass*"));
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
