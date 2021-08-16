/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8175312
 * @summary Test clhsdb 'g1regiondetails' and 'scanoops' commands for G1GC
 * @requires vm.gc.G1
 * @requires vm.hasSA & (vm.bits == "64" & os.maxMemory > 8g)
 * @library /test/lib /test/hotspot/jtreg/serviceability/sa
 * @run main/othervm/timeout=2400 ClhsdbRegionDetailsScanOopsForG1
 */

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import jdk.test.lib.apps.LingeredApp;
import jtreg.SkippedException;

public class ClhsdbRegionDetailsScanOopsForG1 {

    public static void main(String[] args) throws Exception {
        System.out.println("Starting ClhsdbRegionDetailsScanOopsForG1 test");

        LingeredAppWithLargeStringArray theApp = null;
        try {
            ClhsdbLauncher test = new ClhsdbLauncher();

            theApp = new LingeredAppWithLargeStringArray();
            LingeredApp.startApp(theApp,
                "-XX:+UseG1GC",
                "-Xmx8g",
                "-XX:G1HeapRegionSize=2m");
            System.out.println("Started LingeredAppWithLargeStringArray with pid " + theApp.getPid());

            List<String> cmds = List.of("g1regiondetails");
            Map<String, List<String>> expStrMap = new HashMap<>();
            Map<String, List<String>> unExpStrMap = new HashMap<>();

            // Test that the various types of regions are listed with the
            // 'g1regiondetails' command
            expStrMap.put("g1regiondetails", List.of(
                "Region",
                "Eden",
                "Survivor",
                "StartsHumongous",
                "ContinuesHumongous",
                "Free"));
            unExpStrMap.put("g1regiondetails", List.of("Unknown Region Type"));
            String regionDetailsOutput = test.run(theApp.getPid(), cmds,
                                                  expStrMap, unExpStrMap);
            // Test the output of 'scanoops' -- get the start and end addresses
            // from the StartsHumongous region. Ensure that it contains an
            // array of Strings.
            String[] snippets = regionDetailsOutput.split(":StartsHumongous");
            snippets = snippets[0].split("Region: ");
            String[] words = snippets[snippets.length - 1].split(",");
            // words[0] and words[1] represent the start and end addresses
            String cmd = "scanoops " + words[0] + " " + words[1];
            expStrMap = new HashMap<>();
            expStrMap.put(cmd, List.of("\\[Ljava/lang/String"));
            test.run(theApp.getPid(), List.of(cmd), expStrMap, null);
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
