/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test TestPrintRegionRememberedSetInfo
 * @bug 8014240
 * @summary Test output of G1PrintRegionRememberedSetInfo
 * @requires vm.gc.G1
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run driver gc.g1.TestPrintRegionRememberedSetInfo
 * @author thomas.schatzl@oracle.com
 */

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import java.lang.Thread;
import java.util.ArrayList;
import java.util.Arrays;

class RunAndWaitForMarking {
    public static void main(String[] args) {
        System.gc();
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
        }
    }
}

public class TestPrintRegionRememberedSetInfo {

    public static String runTest(String arg) throws Exception {
        ArrayList<String> finalargs = new ArrayList<String>();
        String[] defaultArgs = new String[] {
            "-XX:+UseG1GC",
            "-Xmx10m",
            "-XX:+ExplicitGCInvokesConcurrent",
            "-XX:+UnlockDiagnosticVMOptions",
            "-XX:G1HeapRegionSize=1M",
            "-XX:InitiatingHeapOccupancyPercent=0",
        };

        finalargs.addAll(Arrays.asList(defaultArgs));
        finalargs.add(arg);

        finalargs.add(RunAndWaitForMarking.class.getName());

        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(finalargs);
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldHaveExitValue(0);

        String result = output.getStdout();
        return result;
    }

    public static void main(String[] args) throws Exception {
        String result;

        result = runTest("-Xlog:gc+liveness=trace");
        // check that we got region statistics output
        if (result.indexOf("PHASE") == -1) {
            throw new RuntimeException("Unexpected output from -XX:+PrintRegionLivenessInfo found.");
        }

        result = runTest("-Xlog:gc+liveness");
        if (result.indexOf("remset") != -1) {
            throw new RuntimeException("Should find remembered set information in output.");
        }
    }
}

