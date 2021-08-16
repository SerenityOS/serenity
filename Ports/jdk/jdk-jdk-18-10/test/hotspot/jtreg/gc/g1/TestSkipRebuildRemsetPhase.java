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

package gc.g1;

/*
 * @test TestSkipRebuildRemsetPhase
 * @summary Skip Rebuild Remset Phase if the Remark pause does not identify any rebuild candidates.
 *          Fill up a region to above the set G1MixedGCLiveThresholdPercent.
 * @requires vm.gc.G1
 * @library /test/lib
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run driver gc.g1.TestSkipRebuildRemsetPhase
 */

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.Asserts;
import sun.hotspot.WhiteBox;

public class TestSkipRebuildRemsetPhase {
    public static void main(String[] args) throws Exception {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("-Xbootclasspath/a:.",
                                                                  "-XX:+UseG1GC",
                                                                  "-XX:+UnlockExperimentalVMOptions",
                                                                  "-XX:+UnlockDiagnosticVMOptions",
                                                                  "-XX:+WhiteBoxAPI",
                                                                  "-XX:G1MixedGCLiveThresholdPercent=20",
                                                                  "-Xlog:gc+marking=debug,gc+phases=debug,gc+remset+tracking=trace",
                                                                  "-Xms10M",
                                                                  "-Xmx10M",
                                                                  GCTest.class.getName());
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldContain("Skipping Remembered Set Rebuild.");
        output.shouldContain("No Remembered Sets to update after rebuild");
        output.shouldHaveExitValue(0);
    }

    public static class GCTest {
        public static void main(String args[]) throws Exception {
            WhiteBox wb = WhiteBox.getWhiteBox();
            // Allocate some memory less than region size.
            Object used = alloc();

            // Trigger the full GC using the WhiteBox API.
            wb.fullGC();  // full

            // Memory objects have been promoted to old by full GC.
            // Concurrent cycle should not select any regions for rebuilding
            wb.g1StartConcMarkCycle(); // concurrent-start, remark and cleanup

            // Sleep to make sure concurrent cycle is done
            while (wb.g1InConcurrentMark()) {
                Thread.sleep(1000);
            }

            System.out.println(used);
        }

        private static Object alloc() {
            // Since G1MixedGCLiveThresholdPercent is 20%, make sure to allocate object larger than that
            // so that it will not be collected and the expected message printed.
            final int objectSize = WhiteBox.getWhiteBox().g1RegionSize() / 3;
            Object ret = new byte[objectSize];
            return ret;
        }
    }
}
