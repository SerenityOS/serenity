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

import sun.hotspot.code.Compiler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import jdk.test.lib.apps.LingeredApp;
import jtreg.SkippedException;
import sun.hotspot.gc.GC;

/**
 * @test
 * @summary Test the 'universe' command of jhsdb clhsdb.
 * @requires vm.hasSA
 * @bug 8190307
 * @library /test/lib
 * @build jdk.test.lib.apps.*
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:. TestUniverse
 */

public class TestUniverse {

    private static void testClhsdbForUniverse(long lingeredAppPid, GC gc) throws Exception {
        ClhsdbLauncher launcher = new ClhsdbLauncher();
        List<String> cmds = List.of("universe");
        Map<String, List<String>> expStrMap = new HashMap<>();
        List<String> expStrings = new ArrayList<String>();
        expStrings.add("Heap Parameters");

        switch (gc) {
        case Serial:
            expStrings.add("Gen 1:   old");
            break;

        case Parallel:
            expStrings.add("ParallelScavengeHeap");
            expStrings.add("PSYoungGen");
            expStrings.add("eden");
            break;

        case G1:
            expStrings.add("garbage-first heap");
            expStrings.add("region size");
            expStrings.add("G1 Young Generation:");
            expStrings.add("regions  =");
            break;

        case Epsilon:
            expStrings.add("Epsilon heap");
            expStrings.add("reserved");
            expStrings.add("committed");
            expStrings.add("used");
            break;

        case Z:
            expStrings.add("ZHeap");
            break;

        case Shenandoah:
            expStrings.add("Shenandoah heap");
            break;
        }

        expStrMap.put("universe", expStrings);
        launcher.run(lingeredAppPid, cmds, expStrMap, null);
    }

    private static void test(GC gc) throws Exception {
        LingeredApp app = null;
        try {
            app = LingeredApp.startApp("-XX:+UnlockExperimentalVMOptions", "-XX:+Use" + gc + "GC");
            System.out.println ("Started LingeredApp with " + gc + "GC and pid " + app.getPid());
            testClhsdbForUniverse(app.getPid(), gc);
        } finally {
            LingeredApp.stopApp(app);
        }
    }

    private static boolean isSelectedAndSupported(GC gc) {
        if (!gc.isSelected()) {
            // Not selected
            return false;
        }

        if (Compiler.isGraalEnabled()) {
            if (gc == GC.Epsilon || gc == GC.Z || gc == GC.Shenandoah) {
                // Not supported
                System.out.println ("Skipped testing of " + gc + "GC, not supported by Graal");
                return false;
            }
        }

        // Selected and supported
        return true;
    }

    public static void main (String... args) throws Exception {
        System.out.println("Starting TestUniverse");
        try {
            for (GC gc: GC.values()) {
                if (isSelectedAndSupported(gc)) {
                    test(gc);
                }
            }
        } catch (SkippedException se) {
            throw se;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            throw new Error("Test failed with " + e);
        }
    }
}
