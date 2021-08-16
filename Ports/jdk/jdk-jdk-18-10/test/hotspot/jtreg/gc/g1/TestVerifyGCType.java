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

package gc.g1;

/*
 * @test TestVerifyGCType
 * @summary Test the VerifyGCType flag to ensure basic functionality.
 * @requires vm.gc.G1
 * @library /test/lib
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run driver gc.g1.TestVerifyGCType
 */

import java.util.ArrayList;
import java.util.Collections;

import jdk.test.lib.Asserts;
import jdk.test.lib.Platform;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import sun.hotspot.WhiteBox;

public class TestVerifyGCType {
    public static final String VERIFY_TAG    = "[gc,verify]";
    public static final String VERIFY_BEFORE = "Verifying Before GC";
    public static final String VERIFY_DURING = "Verifying During GC";
    public static final String VERIFY_AFTER  = "Verifying After GC";

    public static void main(String args[]) throws Exception {
        testAllVerificationEnabled();
        testAllExplicitlyEnabled();
        testFullAndRemark();
        testConcurrentMark();
        if (Platform.isDebugBuild()) {
            testYoungEvacFail();
        }
        testBadVerificationType();
    }

    private static void testAllVerificationEnabled() throws Exception {
        // Test with all verification enabled
        OutputAnalyzer output = testWithVerificationType(new String[0]);
        output.shouldHaveExitValue(0);

        verifyCollection("Pause Young (Normal)", true, false, true, output.getStdout());
        verifyCollection("Pause Young (Concurrent Start)", true, false, true, output.getStdout());
        verifyCollection("Pause Young (Mixed)", true, false, true, output.getStdout());
        verifyCollection("Pause Young (Prepare Mixed)", true, false, true, output.getStdout());
        verifyCollection("Pause Remark", false, true, false, output.getStdout());
        verifyCollection("Pause Cleanup", false, true, false, output.getStdout());
        verifyCollection("Pause Full", true, true, true, output.getStdout());
    }

    private static void testAllExplicitlyEnabled() throws Exception {
        OutputAnalyzer output;
        // Test with all explicitly enabled
        output = testWithVerificationType(new String[] {
                "young-normal", "concurrent-start", "mixed", "remark", "cleanup", "full"});
        output.shouldHaveExitValue(0);

        verifyCollection("Pause Young (Normal)", true, false, true, output.getStdout());
        verifyCollection("Pause Young (Concurrent Start)", true, false, true, output.getStdout());
        verifyCollection("Pause Young (Mixed)", true, false, true, output.getStdout());
        verifyCollection("Pause Young (Prepare Mixed)", true, false, true, output.getStdout());
        verifyCollection("Pause Remark", false, true, false, output.getStdout());
        verifyCollection("Pause Cleanup", false, true, false, output.getStdout());
        verifyCollection("Pause Full", true, true, true, output.getStdout());
    }

    private static void testFullAndRemark() throws Exception {
        OutputAnalyzer output;
        // Test with full and remark
        output = testWithVerificationType(new String[] {"remark", "full"});
        output.shouldHaveExitValue(0);

        verifyCollection("Pause Young (Normal)", false, false, false, output.getStdout());
        verifyCollection("Pause Young (Concurrent Start)", false, false, false, output.getStdout());
        verifyCollection("Pause Young (Mixed)", false, false, false, output.getStdout());
        verifyCollection("Pause Young (Prepare Mixed)", false, false, false, output.getStdout());
        verifyCollection("Pause Remark", false, true, false, output.getStdout());
        verifyCollection("Pause Cleanup", false, false, false, output.getStdout());
        verifyCollection("Pause Full", true, true, true, output.getStdout());
    }

    private static void testConcurrentMark() throws Exception {
        OutputAnalyzer output;
        // Test with full and remark
        output = testWithVerificationType(new String[] {"concurrent-start", "cleanup", "remark"});
        output.shouldHaveExitValue(0);

        verifyCollection("Pause Young (Normal)", false, false, false, output.getStdout());
        verifyCollection("Pause Young (Concurrent Start)", true, false, true, output.getStdout());
        verifyCollection("Pause Young (Mixed)", false, false, false, output.getStdout());
        verifyCollection("Pause Young (Prepare Mixed)", false, false, false, output.getStdout());
        verifyCollection("Pause Remark", false, true, false, output.getStdout());
        verifyCollection("Pause Cleanup", false, true, false, output.getStdout());
        verifyCollection("Pause Full", false, false, false, output.getStdout());
    }

    private static void testYoungEvacFail() throws Exception {
        OutputAnalyzer output;
        output = testWithVerificationType(new String[] {"young-evac-fail"},
                                          new String[] {"-XX:+G1EvacuationFailureALot",
                                                        "-XX:G1EvacuationFailureALotCount=100",
                                                        "-XX:G1EvacuationFailureALotInterval=1",
                                                        "-XX:+UnlockDiagnosticVMOptions",
                                                        "-XX:-G1UsePreventiveGC"});
        output.shouldHaveExitValue(0);

        verifyCollection("Pause Young (Normal)", false, false, true, output.getStdout());
        verifyCollection("Pause Young (Concurrent Start)", false, false, true, output.getStdout());
        verifyCollection("Pause Young (Mixed)", false, false, true, output.getStdout());
        verifyCollection("Pause Young (Prepare Mixed)", false, false, true, output.getStdout());
        verifyCollection("Pause Remark", false, false, false, output.getStdout());
        verifyCollection("Pause Cleanup", false, false, false, output.getStdout());
        verifyCollection("Pause Full", false, false, false, output.getStdout());
    }


    private static void testBadVerificationType() throws Exception {
        OutputAnalyzer output;
        // Test bad type
        output = testWithVerificationType(new String[] {"old"});
        output.shouldHaveExitValue(0);

        output.shouldMatch("VerifyGCType: '.*' is unknown. Available types are: young-normal, young-evac-fail, concurrent-start, mixed, remark, cleanup and full");
        verifyCollection("Pause Young (Normal)", true, false, true, output.getStdout());
        verifyCollection("Pause Young (Concurrent Start)", true, false, true, output.getStdout());
        verifyCollection("Pause Young (Mixed)", true, false, true, output.getStdout());
        verifyCollection("Pause Young (Prepare Mixed)", true, false, true, output.getStdout());
        verifyCollection("Pause Remark", false, true, false, output.getStdout());
        verifyCollection("Pause Cleanup", false, true, false, output.getStdout());
        verifyCollection("Pause Full", true, true, true, output.getStdout());
    }

    private static OutputAnalyzer testWithVerificationType(String[] types, String... extraOpts) throws Exception {
        ArrayList<String> basicOpts = new ArrayList<>();
        Collections.addAll(basicOpts, new String[] {
                                       "-Xbootclasspath/a:.",
                                       "-XX:+UnlockDiagnosticVMOptions",
                                       "-XX:+UseG1GC",
                                       "-XX:+WhiteBoxAPI",
                                       "-Xlog:gc,gc+start,gc+verify=info",
                                       "-Xms16m",
                                       "-Xmx16m",
                                       "-XX:ParallelGCThreads=1",
                                       "-XX:G1HeapWastePercent=1",
                                       "-XX:+VerifyBeforeGC",
                                       "-XX:+VerifyAfterGC",
                                       "-XX:+VerifyDuringGC"});

        for(String verifyType : types) {
            basicOpts.add("-XX:VerifyGCType="+verifyType);
        }

        Collections.addAll(basicOpts, extraOpts);

        basicOpts.add(TriggerGCs.class.getName());

        ProcessBuilder procBuilder =  ProcessTools.createJavaProcessBuilder(basicOpts);
        OutputAnalyzer analyzer = new OutputAnalyzer(procBuilder.start());

        return analyzer;
    }

    private static void verifyCollection(String name, boolean expectBefore, boolean expectDuring, boolean expectAfter, String data) {
        CollectionInfo ci = CollectionInfo.parseFirst(name, data);
        Asserts.assertTrue(ci != null, "Expected GC not found: " + name + "\n" + data);

        // Verify Before
        verifyType(ci, expectBefore, VERIFY_BEFORE);
        // Verify During
        verifyType(ci, expectDuring, VERIFY_DURING);
        // Verify After
        verifyType(ci, expectAfter, VERIFY_AFTER);
    }

    private static void verifyType(CollectionInfo ci, boolean shouldExist, String pattern) {
        if (shouldExist) {
            Asserts.assertTrue(ci.containsVerification(pattern), "Missing expected verification pattern " + pattern + " for: " + ci.getName());
        } else {
            Asserts.assertFalse(ci.containsVerification(pattern), "Found unexpected verification pattern " + pattern + " for: " + ci.getName());
        }
    }

    public static class CollectionInfo {
        String name;
        ArrayList<String> verification;
        public CollectionInfo(String name) {
            this.name = name;
            this.verification = new ArrayList<>();
            System.out.println("Created CollectionInfo: " + name);
        }

        public String getName() {
            return name;
        }

        public void addVerification(String verify) {
            System.out.println("Adding: " + verify);
            verification.add(verify);
        }

        public boolean containsVerification(String contains) {
            for (String entry : verification) {
                if (entry.contains(contains)) {
                    return true;
                }
            }
            return false;
        }

        static CollectionInfo parseFirst(String name, String data) {
            CollectionInfo result = null;
            int firstIndex = data.indexOf(name);
            if (firstIndex == -1) {
                return result;
            }
            int nextIndex = data.indexOf(name, firstIndex + 1);
            if (nextIndex == -1) {
                return result;
            }
            // Found an entry for this name
            result = new CollectionInfo(name);
            String collectionData = data.substring(firstIndex, nextIndex + name.length());
            for (String line : collectionData.split(System.getProperty("line.separator"))) {
                if (line.contains(VERIFY_TAG)) {
                    result.addVerification(line);
                }
            }
            return result;
        }
    }

    public static class TriggerGCs {

        // This class triggers GCs; we need to make sure that in all of the young gcs
        // at least some objects survive so that evacuation failure can happen.
        public static void main(String args[]) throws Exception {
            WhiteBox wb = WhiteBox.getWhiteBox();
            // Allocate some memory that can be turned into garbage.
            Object[] used = alloc1M();

            wb.youngGC(); // young-normal

            // Trigger the different GCs using the WhiteBox API.
            wb.fullGC();  // full

            // Memory have been promoted to old by full GC. Free
            // some memory to be reclaimed by concurrent cycle.
            partialFree(used);

            used = alloc1M();
            wb.g1StartConcMarkCycle(); // concurrent-start, remark and cleanup
            partialFree(used);

            // Sleep to make sure concurrent cycle is done
            while (wb.g1InConcurrentMark()) {
                Thread.sleep(1000);
            }

            // Trigger two young GCs, first will be young-prepare-mixed, second will be mixed.
            used = alloc1M();
            wb.youngGC(); // young-prepare-mixed
            partialFree(used);

            used = alloc1M();
            wb.youngGC(); // mixed
            partialFree(used);
        }

        private static Object[] alloc1M() {
            Object[] ret = new Object[1024];
            // Alloc 1024 1k byte arrays (~1M)
            for (int i = 0; i < ret.length; i++) {
                ret[i] = new byte[1024];
            }
            return ret;
        }

        private static void partialFree(Object[] array) {
            // Free every other element
            for (int i = 0; i < array.length; i+=2) {
                array[i] = null;
            }
        }
    }
}
