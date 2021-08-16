/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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


/*
 * @test
 *
 * @summary converted from VM Testbase gc/huge/quicklook/largeheap/MemOptions.
 * @requires vm.bits == 64
 * @requires os.maxMemory > 4G
 *
 * @library /test/lib
 * @run driver gc.huge.quicklook.largeheap.MemOptions.MemOptionsTest
 */

package gc.huge.quicklook.largeheap.MemOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

/*
 * Test JVM startup with different memory options.
 *
 * This checks that 64-bit VMs can start with huge values of memory
 * options. It is intended to be run on machines with more than 4G
 * available memory
 *
 * Based on InitMaxHeapSize, InitNegativeHeapSize, InitMinHeapSize,
 * InitZeroHeapSize
 */
public class MemOptionsTest {
    public static void main(String args[]) throws IOException {
        new MemOptionsTest().test();
    }

    private final ArrayList<String> failed = new ArrayList<>();

    public void test() throws IOException {
        positive("Maximum heap size within 32-bit address range", "-Xmx2G");
        positive("Maximum heap size at 32-bit address range", "-Xmx4G");
        positive("Maximum heap size outside 32-bit address range", "-Xmx5G");
        negative("Maximum heap size of negative value", "-Xmx-1m");
        negative("Maximum heap size of zero value", "-Xmx0m");

        // negative("Less than minimum required heap size", "-Xms2176k", "-Xmx2176k");
        // positive("Minimum required heap size", "-Xms2177k", "-Xmx2177k");

        positive("Initial heap size within 32-bit address range", "-Xms2G", "-Xmx2G");
        positive("Initial heap size at 32-bit address range", "-Xms4G", "-Xmx4G");
        positive("Initial heap size outside 32-bit address range", "-Xms4200M", "-Xmx5G");
        negative("Initial heap size of negative value", "-Xms-1m");
        positive("Initial heap size of zero value", "-Xms0m");

        // positive("Initial young generation size within 32-bit range", "-Xmx3G", "-XX:NewSize=2G");
        // positive("Initial young generation size at 32-bit range", "-Xmx5G", "-XX:NewSize=4G");
        // positive("Initial young generation size outside 32-bit range", "-Xmx5G", "-XX:NewSize=4G");

        // positive("Initial old generation size within 32-bit range", "-Xmx3G", "-XX:OldSize=2G");
        // positive("Initial old generation size at 32-bit range", "-Xmx5G", "-XX:OldSize=4G");
        // positive("Initial old generation size outside 32-bit range", "-Xmx5G", "-XX:OldSize=4G");

        if (!failed.isEmpty()) {
            throw new AssertionError(String.format("%d cases failed : %s", failed.size(), failed));
        }
    }

    private void positive(String name, String... opts) throws IOException {
        System.out.println(name);
        var cmd = new ArrayList<String>();
        Collections.addAll(cmd, opts);
        cmd.add(MemStat.class.getName());
        var pb = ProcessTools.createTestJvm(cmd);
        var output = new OutputAnalyzer(pb.start());
        if (output.getExitValue() != 0) {
            output.reportDiagnosticSummary();
            failed.add(name);
        }
    }

    private void negative(String name, String... opts) throws IOException {
        System.out.println(name);
        var cmd = new ArrayList<String>();
        Collections.addAll(cmd, opts);
        cmd.add(MemStat.class.getName());
        var pb = ProcessTools.createTestJvm(cmd);
        var output = new OutputAnalyzer(pb.start());
        if (output.getExitValue() == 0) {
            output.reportDiagnosticSummary();
            failed.add(name);
        }
    }

    public static class MemStat {
        public static void main(String [] args) {
            Runtime runtime = Runtime.getRuntime();
            System.out.println("Max memory   : "  + runtime.maxMemory());
            System.out.println("Total memory : "  + runtime.totalMemory());
            System.out.println("Free memory  : "  + runtime.freeMemory());
        }
    }
}
