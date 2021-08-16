/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @test TestPLABSizeBounds
 * @bug 8134857
 * @summary Regression test to ensure that G1 supports PLAB sizes of half a region size.
 * @requires vm.gc.G1
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run driver gc.g1.TestPLABSizeBounds
 */

import java.util.ArrayList;

import jdk.test.lib.Platform;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class TestPLABSizeBounds {

    public static final int M = 1024 * 1024;

    /**
     * Starts the VM with the given region size and the given PLAB size arguments. The VM start should
     * succeed if shouldSucceed is true, otherwise it should fail.
     *
     * @param regionSize The region size the VM should be started with in bytes.
     * @param plabSize The young and old gen PLAB sizes the VM should be started with in machine words.
     * @param shouldSucceed The expected result of the VM invocation.
     */
    public static void runTest(int regionSize, int plabSize, boolean shouldSucceed) throws Exception {
        ArrayList<String> testArguments = new ArrayList<String>();

        testArguments.add("-XX:+UseG1GC");
        testArguments.add("-Xmx256M");
        testArguments.add("-XX:G1HeapRegionSize=" + regionSize);
        testArguments.add("-XX:YoungPLABSize=" + plabSize);
        testArguments.add("-XX:OldPLABSize=" + plabSize);
        testArguments.add(GCTest.class.getName());

        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(testArguments);
        OutputAnalyzer output = new OutputAnalyzer(pb.start());

        if (shouldSucceed) {
            output.shouldHaveExitValue(0);
        } else {
            output.shouldHaveExitValue(1);
        }
    }

    public static void runRegionTest(int regionSize) throws Exception {
        final int regionSizeInBytes = regionSize * M;
        final int wordSize = Platform.is32bit() ? 4 : 8;

        runTest(regionSizeInBytes, (regionSizeInBytes / wordSize) / 2 - 1, true);
        runTest(regionSizeInBytes, (regionSizeInBytes / wordSize) / 2, true);
        runTest(regionSizeInBytes, (regionSizeInBytes / wordSize) / 2 + 1, false);
    }

    public static void main(String[] args) throws Exception {
        runRegionTest(1);
        runRegionTest(2);
        runRegionTest(4);
        runRegionTest(8);
        runRegionTest(16);
        runRegionTest(32);
    }

    static class GCTest {
        public static void main(String [] args) {
            System.out.println("Test completed.");
        }
    }
}

