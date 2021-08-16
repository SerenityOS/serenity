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
 * @bug 8174994
 * @summary Test the clhsdb commands 'jstack', 'printall', 'where' with CDS enabled
 * @requires vm.hasSA & vm.cds
 * @library /test/lib
 * @run main/othervm/timeout=2400 -Xmx1g ClhsdbCDSJstackPrintAll
 */

import java.util.List;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.cds.CDSOptions;
import jdk.test.lib.apps.LingeredApp;
import jtreg.SkippedException;

public class ClhsdbCDSJstackPrintAll {

    public static void main(String[] args) throws Exception {
        System.out.println("Starting ClhsdbCDSJstackPrintAll test");
        String sharedArchiveName = "ArchiveForClhsdbJstackPrintAll.jsa";
        LingeredApp theApp = null;

        try {
            CDSOptions opts = (new CDSOptions()).setArchiveName(sharedArchiveName);
            CDSTestUtils.createArchiveAndCheck(opts);

            ClhsdbLauncher test = new ClhsdbLauncher();
            theApp = LingeredApp.startApp(
                "-XX:+UnlockDiagnosticVMOptions",
                "-XX:SharedArchiveFile=" + sharedArchiveName,
                "-Xshare:auto");
            System.out.println("Started LingeredApp with pid " + theApp.getPid());

            // Ensure that UseSharedSpaces is turned on.
            List<String> cmds = List.of("flags UseSharedSpaces");

            String useSharedSpacesOutput = test.run(theApp.getPid(), cmds,
                                                    null, null);

            if (useSharedSpacesOutput == null) {
                LingeredApp.stopApp(theApp);
                // Attach permission issues.
                throw new SkippedException("Could not determine the UseSharedSpaces value");
            }

            if (useSharedSpacesOutput.contains("UseSharedSpaces = false")) {
                // CDS archive is not mapped. Skip the rest of the test.
                LingeredApp.stopApp(theApp);
                throw new SkippedException("The CDS archive is not mapped");
            }

            cmds = List.of("jstack -v", "printall", "where -a");

            Map<String, List<String>> expStrMap = new HashMap<>();
            Map<String, List<String>> unExpStrMap = new HashMap<>();
            expStrMap.put("jstack -v", List.of(
                "No deadlocks found",
                "Common-Cleaner",
                "Signal Dispatcher",
                "Method*",
                "LingeredApp.steadyState"));
            unExpStrMap.put("jstack -v", List.of(
                "sun.jvm.hotspot.types.WrongTypeException",
                "No suitable match for type of address"));
            expStrMap.put("printall", List.of(
                "aload_0",
                "_nofast_aload_0",
                "_nofast_getfield",
                "_nofast_putfield",
                "Constant Pool of",
                "public static void main\\(java.lang.String\\[\\]\\)",
                "Bytecode",
                "invokevirtual",
                "checkcast",
                "Exception Table",
                "invokedynamic"));
            unExpStrMap.put("printall", List.of(
                "No suitable match for type of address",
                "illegal code",
                "Failure occurred at bci"));
            expStrMap.put("where -a", List.of(
                "Java Stack Trace for SteadyStateThread",
                "private static void steadyState"));
            unExpStrMap.put("where -a", List.of(
                "illegal code",
                "Failure occurred at bci"));
            test.run(theApp.getPid(), cmds, expStrMap, unExpStrMap);
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
