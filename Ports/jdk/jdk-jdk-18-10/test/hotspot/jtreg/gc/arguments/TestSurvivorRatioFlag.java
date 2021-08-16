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
 * @test TestSurvivorRatioFlag
 * @summary Verify that actual survivor ratio is equal to specified SurvivorRatio value
 * @requires vm.gc != "Z" & vm.gc != "Shenandoah"
 * @library /test/lib
 * @library /
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run driver gc.arguments.TestSurvivorRatioFlag
 */

import java.lang.management.MemoryUsage;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.Utils;
import sun.hotspot.WhiteBox;

public class TestSurvivorRatioFlag {

    public static final long M = 1024 * 1024;
    public static final long HEAP_SIZE = 200 * M;
    public static final long NEW_SIZE = 100 * M;

    public static void main(String args[]) throws Exception {
        LinkedList<String> options = new LinkedList<>(
                Arrays.asList(Utils.getFilteredTestJavaOpts("-XX:[^ ]*SurvivorRatio=[^ ]+"))
        );

        testSurvivorRatio(3, options);
        testSurvivorRatio(6, options);
        testSurvivorRatio(10, options);
        testSurvivorRatio(15, options);
        testSurvivorRatio(20, options);
    }

    /**
     * Verify that actual survivor ratio equal to specified.
     *
     * @param ratio survivor ratio that be verified
     * @param options additional options to JVM
     */
    public static void testSurvivorRatio(int ratio, LinkedList<String> options) throws Exception {

        LinkedList<String> vmOptions = new LinkedList<>(options);

        Collections.addAll(vmOptions,
                "-Xbootclasspath/a:.",
                "--add-exports=java.base/jdk.internal.misc=ALL-UNNAMED",
                "-XX:+UnlockDiagnosticVMOptions",
                "-XX:+WhiteBoxAPI",
                "-XX:GCLockerEdenExpansionPercent=0",
                "-XX:MaxNewSize=" + NEW_SIZE,
                "-XX:NewSize=" + NEW_SIZE,
                "-Xmx" + HEAP_SIZE,
                "-Xms" + HEAP_SIZE,
                "-XX:SurvivorRatio=" + ratio,
                SurvivorRatioVerifier.class.getName(),
                Integer.toString(ratio)
        );

        ProcessBuilder procBuilder = GCArguments.createJavaProcessBuilder(vmOptions);
        OutputAnalyzer analyzer = new OutputAnalyzer(procBuilder.start());
        analyzer.shouldHaveExitValue(0);
    }

    /**
     * Class that verifies survivor ratio.
     */
    public static class SurvivorRatioVerifier {

        static WhiteBox wb = WhiteBox.getWhiteBox();

        public static final int MAX_ITERATIONS = 10;
        public static final int ARRAY_LENGTH = 10000;
        public static final int CHUNK_SIZE = 10000;

        public static void main(String args[]) throws Exception {
            if (args.length != 1) {
                throw new IllegalArgumentException("Expected 1 arg: <ratio>");
            }
            final int ratio = Integer.valueOf(args[0]);

            AllocationHelper allocator = new AllocationHelper(MAX_ITERATIONS, ARRAY_LENGTH, CHUNK_SIZE, () -> (verifySurvivorRatio(ratio)));
            allocator.allocateMemoryAndVerify();
        }

        /**
         * Verify that actual survivor ratio is equal to expected.
         * Depending on selected young GC we verify that:
         * - for DefNew and ParNew: eden_size / survivor_size is close to expectedRatio;
         * - for PSNew:             survivor_size equal to young_gen_size / expectedRatio;
         * - for G1:                survivor_regions <= young_list_length / expectedRatio.
         */
        public static Void verifySurvivorRatio(int expectedRatio) {
            GCTypes.YoungGCType type = GCTypes.YoungGCType.getYoungGCType();
            switch (type) {
                case DefNew:
                    verifyDefNewSurvivorRatio(expectedRatio);
                    break;
                case PSNew:
                    verifyPSSurvivorRatio(expectedRatio);
                    break;
                case G1:
                    verifyG1SurvivorRatio(expectedRatio);
                    break;
                default:
                    throw new RuntimeException("Unexpected young GC type");
            }
            return null;
        }

        private static void verifyDefNewSurvivorRatio(int expectedRatio) {
            MemoryUsage edenUsage = HeapRegionUsageTool.getEdenUsage();
            MemoryUsage survivorUsage = HeapRegionUsageTool.getSurvivorUsage();

            int actualRatio = (int) (edenUsage.getCommitted() / survivorUsage.getCommitted());
            if (Math.abs(actualRatio - expectedRatio) > 1) {
                throw new RuntimeException("Expected survivor ratio is: " + expectedRatio
                        + ", but observed ratio is: " + actualRatio);
            }
        }

        private static void verifyPSSurvivorRatio(int expectedRatio) {
            MemoryUsage edenUsage = HeapRegionUsageTool.getEdenUsage();
            MemoryUsage survivorUsage = HeapRegionUsageTool.getSurvivorUsage();

            long youngGenSize = edenUsage.getMax() + 2 * survivorUsage.getMax();
            // for Paralle GC Min/InitialSurvivorRatio = SurvivorRatio + 2
            long expectedSize = HeapRegionUsageTool.alignDown(youngGenSize / (expectedRatio + 2),
                    wb.psHeapGenerationAlignment());

            if (expectedSize != survivorUsage.getCommitted()) {
                throw new RuntimeException("Expected survivor size is: " + expectedSize
                        + ", but observed size is: " + survivorUsage.getCommitted());
            }
        }

        private static void verifyG1SurvivorRatio(int expectedRatio) {
            MemoryUsage survivorUsage = HeapRegionUsageTool.getSurvivorUsage();

            int regionSize = wb.g1RegionSize();
            int youngListLength = (int) Math.max(NEW_SIZE / regionSize, 1);
            int expectedSurvivorRegions = (int) Math.ceil(youngListLength / (double) expectedRatio);
            int observedSurvivorRegions = (int) (survivorUsage.getCommitted() / regionSize);

            if (expectedSurvivorRegions < observedSurvivorRegions) {
                throw new RuntimeException("Expected amount of G1 survivor regions is "
                        + expectedSurvivorRegions + ", but observed "
                        + observedSurvivorRegions);
            }
        }
    }
}
