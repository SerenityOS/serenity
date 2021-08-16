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

package gc.stress;

/*
 * @test TestReclaimStringsLeaksMemory
 * @bug 8180048
 * @summary Ensure that during a Full GC interned string memory is reclaimed completely.
 * @requires vm.gc == "null"
 * @requires !vm.debug
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 * @run driver gc.stress.TestReclaimStringsLeaksMemory
 * @run driver gc.stress.TestReclaimStringsLeaksMemory -XX:+UseSerialGC
 * @run driver gc.stress.TestReclaimStringsLeaksMemory -XX:+UseParallelGC
 * @run driver gc.stress.TestReclaimStringsLeaksMemory -XX:+UseG1GC
 */

import java.util.Arrays;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import jdk.test.lib.Asserts;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class TestReclaimStringsLeaksMemory {

    // The amount of memory in B reserved in the "Symbol" category that indicates a memory leak for
    // this test.
    public static final int ReservedThreshold = 70000000;

    public static void main(String[] args) throws Exception {
        ArrayList<String> baseargs = new ArrayList<>(Arrays.asList("-Xms256M",
                                                                   "-Xmx256M",
                                                                   "-Xlog:gc*,stringtable*=debug:gc.log",
                                                                   "-XX:NativeMemoryTracking=summary",
                                                                   "-XX:+UnlockDiagnosticVMOptions",
                                                                   "-XX:+PrintNMTStatistics" ));
        baseargs.addAll(Arrays.asList(args));
        baseargs.add(GCTest.class.getName());
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(baseargs);
        verifySymbolMemoryUsageNotTooHigh(new OutputAnalyzer(pb.start()));
    }

    private static void verifySymbolMemoryUsageNotTooHigh(OutputAnalyzer output) throws Exception {
        String stdout = output.getStdout();
        System.out.println(stdout);

        Pattern p = Pattern.compile("Symbol \\(reserved=(\\d*)");
        Matcher m = p.matcher(stdout);

        if (!m.find()) {
            throw new RuntimeException("Could not find Symbol memory usage in NMT output");
        }

        int reserved = Integer.parseInt(m.group(1));
        Asserts.assertLT(reserved, ReservedThreshold, "Reserved memory size is " + reserved + "B which is greater than or equal to " + ReservedThreshold + "B indicating a memory leak");

        output.shouldHaveExitValue(0);
    }

    static class GCTest {
        public static final String BaseName = "SomeRandomBaseString";
        public static volatile String lastString;

        public static void main(String [] args) {
            for (int iterations = 0; iterations < 20;) {
                for (int i = 0; i < 1000000; i++) {
                    lastString = (BaseName + i).intern();
                }
                if (++iterations % 5 == 0) {
                    System.gc();
                }
            }
            // Do one last GC and sleep to give ServiceThread a chance to run.
            System.out.println("One last gc");
            System.gc();
            for (int i = 0; i < 100; i++) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ex) {
                }
            }
            System.out.println("End of test");
        }
    }
}

