/*
 * Copyright (c) 2018, Red Hat, Inc. All rights reserved.
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
 * @summary Test that disabling wrong barriers fails early
 * @requires vm.gc.Shenandoah
 * @library /test/lib
 * @run driver TestWrongBarrierDisable
 */

import java.util.*;

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;

public class TestWrongBarrierDisable {

    public static void main(String[] args) throws Exception {
        String[] concurrent = {
                "ShenandoahLoadRefBarrier",
                "ShenandoahSATBBarrier",
                "ShenandoahCASBarrier",
                "ShenandoahCloneBarrier",
                "ShenandoahNMethodBarrier",
                "ShenandoahStackWatermarkBarrier",
        };
        String[] iu = {
                "ShenandoahLoadRefBarrier",
                "ShenandoahIUBarrier",
                "ShenandoahCASBarrier",
                "ShenandoahCloneBarrier",
                "ShenandoahNMethodBarrier",
                "ShenandoahStackWatermarkBarrier",
        };

        shouldFailAll("-XX:ShenandoahGCHeuristics=adaptive",   concurrent);
        shouldFailAll("-XX:ShenandoahGCHeuristics=static",     concurrent);
        shouldFailAll("-XX:ShenandoahGCHeuristics=compact",    concurrent);
        shouldFailAll("-XX:ShenandoahGCHeuristics=aggressive", concurrent);
        shouldFailAll("-XX:ShenandoahGCMode=iu",               iu);
        shouldPassAll("-XX:ShenandoahGCMode=passive",          concurrent);
        shouldPassAll("-XX:ShenandoahGCMode=passive",          iu);
    }

    private static void shouldFailAll(String h, String[] barriers) throws Exception {
        for (String b : barriers) {
            ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
                    "-Xmx128m",
                    "-XX:+UnlockDiagnosticVMOptions",
                    "-XX:+UnlockExperimentalVMOptions",
                    "-XX:+UseShenandoahGC",
                    h,
                    "-XX:-" + b,
                    "-version"
            );
            OutputAnalyzer output = new OutputAnalyzer(pb.start());
            output.shouldNotHaveExitValue(0);
            output.shouldContain("GC mode needs ");
            output.shouldContain("to work correctly");
        }
    }

    private static void shouldPassAll(String h, String[] barriers) throws Exception {
        for (String b : barriers) {
            ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
                    "-Xmx128m",
                    "-XX:+UnlockDiagnosticVMOptions",
                    "-XX:+UnlockExperimentalVMOptions",
                    "-XX:+UseShenandoahGC",
                    h,
                    "-XX:-" + b,
                    "-version"
            );
            OutputAnalyzer output = new OutputAnalyzer(pb.start());
            output.shouldHaveExitValue(0);
        }
    }

}
