/*
 * Copyright (c) 2017, 2018, Red Hat, Inc. All rights reserved.
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
 *
 */

/* @test
 * @summary Test selective barrier enabling works, by aggressively compiling HelloWorld with combinations
 *          of barrier flags
 * @requires vm.gc.Shenandoah
 * @library /test/lib
 * @run driver TestSelectiveBarrierFlags -Xint
 * @run driver TestSelectiveBarrierFlags -Xbatch -XX:CompileThreshold=100 -XX:TieredStopAtLevel=1
 * @run driver TestSelectiveBarrierFlags -Xbatch -XX:CompileThreshold=100 -XX:-TieredCompilation -XX:+IgnoreUnrecognizedVMOptions -XX:+ShenandoahVerifyOptoBarriers
 */

import java.util.*;
import java.util.concurrent.*;

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;

public class TestSelectiveBarrierFlags {

    public static void main(String[] args) throws Exception {
        String[][] opts = {
                new String[] { "ShenandoahLoadRefBarrier" },
                new String[] { "ShenandoahSATBBarrier", "ShenandoahIUBarrier" },
                new String[] { "ShenandoahCASBarrier" },
                new String[] { "ShenandoahCloneBarrier" },
                new String[] { "ShenandoahNMethodBarrier" },
                new String[] { "ShenandoahStackWatermarkBarrier" }
        };

        int size = 1;
        for (String[] l : opts) {
            size *= (l.length + 1);
        }

        ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        for (int c = 0; c < size; c++) {
            int t = c;

            List<String> conf = new ArrayList<>();
            conf.addAll(Arrays.asList(args));
            conf.add("-Xmx128m");
            conf.add("-XX:+UnlockDiagnosticVMOptions");
            conf.add("-XX:+UnlockExperimentalVMOptions");
            conf.add("-XX:+UseShenandoahGC");
            conf.add("-XX:ShenandoahGCMode=passive");

            StringBuilder sb = new StringBuilder();
            for (String[] l : opts) {
                // Make a choice which flag to select from the group.
                // Zero means no flag is selected from the group.
                int choice = t % (l.length + 1);
                for (int e = 0; e < l.length; e++) {
                    conf.add("-XX:" + ((choice == (e + 1)) ? "+" : "-") + l[e]);
                }
                t = t / (l.length + 1);
            }

            conf.add("TestSelectiveBarrierFlags$Test");

            pool.submit(() -> {
                try {
                    ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(conf.toArray(new String[0]));
                    OutputAnalyzer output = new OutputAnalyzer(pb.start());
                    output.shouldHaveExitValue(0);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            });
        }

        pool.shutdown();
        pool.awaitTermination(1, TimeUnit.HOURS);
    }

    public static class Test {
        public static void main(String... args) {
            System.out.println("HelloWorld");
        }
    }

}
