/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

package gc.arguments;

/*
 * @test TestNewRatioFlag
 * @bug 8025166
 * @summary Verify that heap devided among generations according to NewRatio
 * @requires vm.gc != "Z" & vm.gc != "Shenandoah"
 * @library /test/lib
 * @library /
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run driver gc.arguments.TestNewRatioFlag
 */

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.Utils;
import sun.hotspot.WhiteBox;

public class TestNewRatioFlag {

    public static final long M = 1024 * 1024;
    public static final long HEAP_SIZE = 100 * M;

    public static void main(String args[]) throws Exception {
        LinkedList<String> options = new LinkedList<>(
                Arrays.asList(Utils.getFilteredTestJavaOpts("(-XX:[^ ]*NewSize=[^ ]+)|(-Xm[ns][^ ]+)"))
        );

        testNewRatio(4, options);
        testNewRatio(6, options);
        testNewRatio(10, options);
        testNewRatio(15, options);
        testNewRatio(20, options);
    }

    /**
     * Verify that actual size of young gen conforms specified NewRatio
     *
     * @param ratio value of NewRatio option
     * @param options additional options for VM
     */
    public static void testNewRatio(int ratio, LinkedList<String> options) throws Exception {
        LinkedList<String> vmOptions = new LinkedList<>(options);
        Collections.addAll(vmOptions,
                "-Xbootclasspath/a:.",
                "-XX:+UnlockDiagnosticVMOptions",
                "-XX:+WhiteBoxAPI",
                "-XX:GCLockerEdenExpansionPercent=0",
                "-Xmx" + HEAP_SIZE,
                "-Xms" + HEAP_SIZE,
                "-XX:NewRatio=" + ratio,
                "-XX:-UseLargePages",
                NewRatioVerifier.class.getName(),
                Integer.toString(ratio)
        );

        ProcessBuilder procBuilder = GCArguments.createJavaProcessBuilder(vmOptions);
        OutputAnalyzer analyzer = new OutputAnalyzer(procBuilder.start());
        analyzer.shouldHaveExitValue(0);
        System.out.println(analyzer.getOutput());
    }

    public static class NewRatioVerifier {

        static WhiteBox wb = WhiteBox.getWhiteBox();

        public static void main(String args[]) {
            if (args.length != 1) {
                throw new IllegalArgumentException("Expected 1 arg: <expectedRatio>");
            }
            int expectedRatio = Integer.valueOf(args[0]);
            switch (GCTypes.YoungGCType.getYoungGCType()) {
                case DefNew:
                    verifyDefNewNewRatio(expectedRatio);
                    break;
                case PSNew:
                    verifyPSNewRatio(expectedRatio);
                    break;
                case G1:
                    verifyG1NewRatio(expectedRatio);
                    break;
                default:
                    throw new RuntimeException("Unexpected young GC type");
            }
        }

        /**
         * Verify NewSize for DefNew and ParNew collectors.
         *
         * Compare expected NewSize calculated according to sizing policies used by DefNew
         * with NewSize value reported by MemoryPoolMXBeans.
         */
        public static void verifyDefNewNewRatio(int expectedRatio) {
            long initEden = HeapRegionUsageTool.getEdenUsage().getInit();
            long initSurv = HeapRegionUsageTool.getSurvivorUsage().getInit();
            long initHeap = HeapRegionUsageTool.getHeapUsage().getInit();

            long newSize = initEden + 2 * initSurv;

            // See GenArguments::scale_by_NewRatio_aligned for calculation in the JVM.
            long expectedNewSize = HeapRegionUsageTool.alignDown(initHeap / (expectedRatio + 1),
                    wb.getHeapSpaceAlignment());

            if (expectedNewSize != newSize) {
                throw new RuntimeException("Expected young gen size is: " + expectedNewSize
                        + ", but observed new size is: " + newSize);
            }
        }

        /**
         * Verify NewSize for PS collector.
         * Expected NewSize calculated according to alignment policies used by PS
         * and then compared with actual NewSize obtained from MemoryPoolMXBeans.
         */
        public static void verifyPSNewRatio(int expectedRatio) {
            long initEden = HeapRegionUsageTool.getEdenUsage().getInit();
            long initSurv = HeapRegionUsageTool.getSurvivorUsage().getInit();
            long initHeap = HeapRegionUsageTool.getHeapUsage().getInit();

            long newSize = initEden + 2 * initSurv;

            // See GenArguments::scale_by_NewRatio_aligned for calculation in the JVM.
            long alignedDownNewSize = HeapRegionUsageTool.alignDown(initHeap / (expectedRatio + 1),
                    wb.getHeapSpaceAlignment());
            long expectedNewSize = HeapRegionUsageTool.alignUp(alignedDownNewSize,
                    wb.psVirtualSpaceAlignment());

            if (expectedNewSize != newSize) {
                throw new RuntimeException("Expected young gen size is: " + expectedNewSize
                        + ", but observed new size is: " + newSize);
            }
        }

        /**
         * Verify NewSize for G1 GC.
         * Amount of young regions calculated according to sizing policies used by G1
         * and then compared with actual number of young regions derived from
         * values reported by MemoryPoolMXBeans and region size.
         */
        public static void verifyG1NewRatio(int expectedRatio) {
            long initEden = HeapRegionUsageTool.getEdenUsage().getInit();
            long initSurv = HeapRegionUsageTool.getSurvivorUsage().getInit();
            long maxOld = HeapRegionUsageTool.getOldUsage().getMax();

            int regionSize = wb.g1RegionSize();
            int youngListLength = (int) ((initEden + initSurv) / regionSize);
            int maxRegions = (int) (maxOld / regionSize);
            int expectedYoungListLength = (int) (maxRegions / (double) (expectedRatio + 1));

            if (youngListLength != expectedYoungListLength) {
                throw new RuntimeException("Expected G1 young list length is: " + expectedYoungListLength
                        + ", but observed young list length is: " + youngListLength);
            }
        }
    }
}
