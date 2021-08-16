/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @test TestPeriodicLogMessages
 * @bug 8216490
 * @requires vm.gc.G1
 * @summary Verify that log messages are printed as expected
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 * @modules java.management/sun.management
 * @run driver gc.g1.TestPeriodicLogMessages
 */

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class TestPeriodicLogMessages {

    public static void main(String[] args) throws Exception {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("-XX:+UseG1GC",
                                                                  "-XX:G1PeriodicGCInterval=0",
                                                                  "-Xlog:gc+init,gc+periodic=debug",
                                                                  "-Xmx10M",
                                                                  GCTest.class.getName());

        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldContain("Periodic GC: Disabled");
        output.shouldNotContain("Checking for periodic GC");
        output.shouldHaveExitValue(0);

        pb = ProcessTools.createJavaProcessBuilder("-XX:+UseG1GC",
                                                   "-XX:G1PeriodicGCInterval=100",
                                                   "-Xlog:gc+init,gc+periodic=debug",
                                                   "-Xmx10M",
                                                   GCTest.class.getName());

        output = new OutputAnalyzer(pb.start());
        output.shouldContain("Periodic GC: Enabled");
        output.shouldContain("Periodic GC Interval: 100ms");
        output.shouldContain("Checking for periodic GC");
        output.shouldHaveExitValue(0);
    }

    static class GCTest {
        public static void main(String [] args) throws Exception {
            System.out.println("Waiting for messages...");
            Thread.sleep(1000);
            System.out.println("Done");
        }
    }
}


