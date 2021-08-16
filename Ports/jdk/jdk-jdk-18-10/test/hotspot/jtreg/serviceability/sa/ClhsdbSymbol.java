/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
import jtreg.SkippedException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @test
 * @bug 8261095
 * @summary Test the clhsdb 'symbol' command on live process
 * @requires vm.hasSA
 * @library /test/lib
 * @run main/othervm ClhsdbSymbol
 */

public class ClhsdbSymbol {

    public static void main(String[] args) throws Exception {
        System.out.println("Starting the ClhsdbSymbol test");
        LingeredApp theApp = null;

        try {
            List<String> cmds = null;
            String cmdStr = null;
            Map<String, List<String>> expStrMap = null;
            ClhsdbLauncher test = new ClhsdbLauncher();
            theApp = LingeredApp.startApp();
            System.out.println("Started LingeredApp with pid " + theApp.getPid());

            // Use command "class java.lang.Thread" to get the address of the InstanceKlass for java.lang.Thread
            cmdStr = "class java.lang.Thread";
            cmds = List.of(cmdStr);
            expStrMap = new HashMap<>();
            expStrMap.put(cmdStr, List.of("java/lang/Thread"));
            String classOutput = test.run(theApp.getPid(), cmds, expStrMap, null);

            // Extract the thread InstanceKlass address from the output, which looks similar to the following:
            //   java/lang/Thread @0x000000080001d940
            String threadAddress = classOutput.lines()
                    .filter(part -> part.startsWith("java/lang/Thread"))
                    .map(part -> part.split(" @"))
                    .findFirst()
                    .map(addresses -> addresses[1])
                    .orElseThrow(() -> new RuntimeException(
                            "Cannot find address of the InstanceKlass for java.lang.Thread in output"));


            // Use "inspect" on the thread address we extracted in the previous step
            cmdStr = "inspect " + threadAddress;
            cmds = List.of(cmdStr);
            expStrMap = new HashMap<>();
            expStrMap.put(cmdStr, List.of("Symbol"));
            String inspectOutput = test.run(theApp.getPid(), cmds, expStrMap, null);

            // The inspect command output will have one line of output that looks like the following.
            //   Symbol* Klass::_name: Symbol @ 0x0000000800471120
            // Extract the Symbol address from it.
            String symbolAddress = inspectOutput.lines()
                    .filter(part -> part.startsWith("Symbol"))
                    .map(part -> part.split("@ "))
                    .findFirst().map(symbolParts -> symbolParts[1])
                    .orElseThrow(() -> new RuntimeException("Cannot find address with Symbol instance"));

            // Run "symbol" command on the Symbol instance address extracted in the previous step.
            // It should produce the symbol for java/lang/Thread.
            cmdStr = "symbol " + symbolAddress;
            cmds = List.of(cmdStr);
            expStrMap = new HashMap<>();
            expStrMap.put(cmdStr, List.of("#java/lang/Thread"));
            test.run(theApp.getPid(), cmds, expStrMap, null);
        } catch (SkippedException se) {
            throw se;
        } catch (Exception ex) {
            throw new RuntimeException("Test ERROR " + ex, ex);
        } finally {
            LingeredApp.stopApp(theApp);
        }
        System.out.println("Test PASSED");
    }
}
