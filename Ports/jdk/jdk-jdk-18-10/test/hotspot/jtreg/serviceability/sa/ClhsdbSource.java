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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jdk.test.lib.apps.LingeredApp;
import jtreg.SkippedException;

/**
 * @test
 * @bug 8192823
 * @summary Test clhsdb source command
 * @requires vm.hasSA
 * @library /test/lib
 * @run main/othervm ClhsdbSource
 */

public class ClhsdbSource {

    public static void main(String[] args) throws Exception {
        System.out.println("Starting ClhsdbSource test");

        LingeredApp theApp = null;
        try {
            ClhsdbLauncher test = new ClhsdbLauncher();
            theApp = LingeredApp.startApp();
            System.out.println("Started LingeredApp with pid " + theApp.getPid());

            Path file = Paths.get("clhsdb_cmd_file");
            Files.write(file, "jstack -v\nhelp".getBytes());
            List<String> cmds = List.of("source clhsdb_cmd_file");

            Map<String, List<String>> expStrMap = new HashMap<>();
            expStrMap.put("source clhsdb_cmd_file", List.of(
                    "No deadlocks found",
                    "Common\\-Cleaner",
                    "Signal Dispatcher",
                    "java.lang.ref.Finalizer\\$FinalizerThread.run",
                    "java.lang.ref.Reference",
                    "Method\\*",
                    "LingeredApp.steadyState",
                    "Available commands:",
                    "attach pid \\| exec core",
                    "intConstant \\[ name \\[ value \\] \\]",
                    "type \\[ type \\[ name super isOop isInteger isUnsigned size \\] \\]"));

            Map<String, List<String>> unExpStrMap = new HashMap<>();
            unExpStrMap.put("source clhsdb_cmd_file", List.of(
                        "No such file or directory"));

            test.run(theApp.getPid(), cmds, expStrMap, unExpStrMap);
            Files.delete(file);
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
