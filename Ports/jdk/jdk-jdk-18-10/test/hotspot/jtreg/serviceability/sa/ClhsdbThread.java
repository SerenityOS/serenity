/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jdk.test.lib.apps.LingeredApp;
import jtreg.SkippedException;

/**
 * @test
 * @bug 8193352
 * @summary Test clhsdb 'thread' and 'threads' commands
 * @requires vm.hasSA
 * @library /test/lib
 * @run main/othervm ClhsdbThread
 */

public class ClhsdbThread {

    public static void main(String[] args) throws Exception {
        System.out.println("Starting ClhsdbThread test");

        LingeredApp theApp = null;
        try {
            ClhsdbLauncher test = new ClhsdbLauncher();
            theApp = LingeredApp.startApp();
            System.out.println("Started LingeredApp with pid " + theApp.getPid());

            List<String> cmds = List.of("thread", "thread -a", "threads");

            Map<String, List<String>> expStrMap = new HashMap<>();
            // Check for the presence of the usage string
            expStrMap.put("thread", List.of( "Usage: thread \\{ \\-a \\| id \\}"));
            expStrMap.put("thread -a", List.of(
                "State: BLOCKED",
                "Stack in use by Java",
                "Base of Stack",
                "Last_Java_SP:",
                "Address"));
            expStrMap.put("threads", List.of(
                "Finalizer",
                "Signal Dispatcher",
                "Common-Cleaner",
                "Stack in use by Java:",
                "State:",
                "Base of Stack:",
                "main"));
            Map<String, List<String>> unExpStrMap = new HashMap<>();
            unExpStrMap.put(
                "thread -a",
                List.of("Couldn't find thread \\-a"));

            String consolidatedOutput = test.run(
                theApp.getPid(),
                cmds,
                expStrMap,
                unExpStrMap);

            // Test the thread <id> command now. Obtain <id> from the
            // output of the previous 'threads' command. The word before
            // the token 'Finalizer' should denote the thread id of the
            // 'Finalizer' thread.

            String[] snippets = consolidatedOutput.split("Finalizer");
            String[] wordTokens = snippets[0].split(" ");
            String threadIdObtained = wordTokens[wordTokens.length - 1];

            // Weed out newlines and blurb before that.
            if (threadIdObtained.contains("\n")) {
                String[] threadIdTokens = threadIdObtained.split("\n");
                threadIdObtained = threadIdTokens[threadIdTokens.length - 1];
            }

            expStrMap = new HashMap<>();
            System.out.println("Thread Id obtained is: " + threadIdObtained);

            String cmd = "thread " + threadIdObtained;
            expStrMap.put(cmd, List.of(
                "Base of Stack:",
                "State:",
                "Last_Java_SP"));
            cmds = List.of(cmd);
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
