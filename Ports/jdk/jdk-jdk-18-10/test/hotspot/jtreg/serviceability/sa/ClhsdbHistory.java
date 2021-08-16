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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jdk.test.lib.apps.LingeredApp;
import jdk.test.lib.Utils;
import jtreg.SkippedException;

/**
 * @test
 * @bug 8190198
 * @bug 8217612
 * @bug 8217845
 * @summary Test clhsdb command line history support
 * @requires vm.hasSA
 * @library /test/lib
 * @run main/othervm ClhsdbHistory
 */

public class ClhsdbHistory {

    public static void testHistory() throws Exception {
        System.out.println("Starting ClhsdbHistory basic test");

        LingeredApp theApp = null;
        try {
            ClhsdbLauncher test = new ClhsdbLauncher();
            theApp = LingeredApp.startApp();
            System.out.println("Started LingeredApp with pid " + theApp.getPid());

            List<String> cmds = List.of(
                    "echo true",
                    "assert false",
                    "!!",                 // !! repeats previous command
                    "versioncheck !$",    // !$ is last argument from previous command
                    "assert foo bar baz",
                    "versioncheck !*",    // !* is all arguments from previous command
                    "versioncheck !$ !$", // !$ !$ should result in "baz baz"
                    "assert maybe never",
                    "!!- !*",             // !!- is the previous command, minus the last arg
                    "!echo",              // match previous echo command, with args
                    "assert \\!foo",      // quote the ! so it is not used for command history expansion
                    "!10",                // match the 10th command in the history, with args
                    "history");

            // Unfortunately we can't create a map table that maps the clhsdb commands above
            // to the expected output because the commands as you see them above won't be in
            // the output. Instead their expanded forms will. So instead we just rely on
            // the history output looking as expected.
            Map<String, List<String>> expStrMap = new HashMap<>();
            expStrMap.put("history", List.of(
                "0 echo true",    // issued by ClhsdbLauncher
                "1 verbose true", // issued by ClhsdbLauncher
                "2 echo true",
                "3 assert false",
                "4 assert false",
                "5 versioncheck false",
                "6 assert foo bar baz",
                "7 versioncheck foo bar baz",
                "8 versioncheck baz baz",
                "9 assert maybe never",
                "10 assert maybe maybe never",
                "11 echo true",
                "12 assert !foo",
                "13 assert maybe maybe never",
                "14 history"));

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

    public static void main(String[] args) throws Exception {
        testHistory();
    }
}
