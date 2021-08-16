/*
 * Copyright (c) 2016, 2018, Red Hat, Inc. All rights reserved.
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

/*
 * @test
 * @summary Test that Shenandoah GC thread counts are handled well
 * @requires vm.gc.Shenandoah
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run driver TestThreadCounts
 */

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;

public class TestThreadCounts {
    public static void main(String[] args) throws Exception {
        for (int conc = 0; conc < 16; conc++) {
            for (int par = 0; par < 16; par++) {
                testWith(conc, par);
            }
        }
    }

    private static void testWith(int conc, int par) throws Exception {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
                "-Xmx128m",
                "-XX:+UnlockDiagnosticVMOptions",
                "-XX:+UnlockExperimentalVMOptions",
                "-XX:+UseShenandoahGC",
                "-XX:ConcGCThreads=" + conc,
                "-XX:ParallelGCThreads=" + par,
                "-version");
        OutputAnalyzer output = new OutputAnalyzer(pb.start());

        if (conc == 0) {
            output.shouldContain("Shenandoah expects ConcGCThreads > 0");
            output.shouldHaveExitValue(1);
        } else if (par == 0) {
            output.shouldContain("Shenandoah expects ParallelGCThreads > 0");
            output.shouldHaveExitValue(1);
        } else if (conc > par) {
            output.shouldContain("Shenandoah expects ConcGCThreads <= ParallelGCThreads");
            output.shouldHaveExitValue(1);
        } else {
            output.shouldNotContain("Shenandoah expects ConcGCThreads <= ParallelGCThreads");
            output.shouldHaveExitValue(0);
        }
    }

}
