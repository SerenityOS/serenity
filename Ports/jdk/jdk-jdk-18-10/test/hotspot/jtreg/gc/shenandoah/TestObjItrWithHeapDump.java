/*
 * Copyright (c) 2019, Red Hat, Inc. All rights reserved.
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
 * @summary Test heap dump triggered heap object iteration
 * @bug 8225014
 * @requires vm.gc.Shenandoah
 * @library /test/lib
 * @run driver TestObjItrWithHeapDump
 */

import java.util.*;

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;

public class TestObjItrWithHeapDump {
    public static void testWith(String... args) throws Exception {
        String[] cmds = Arrays.copyOf(args, args.length + 2);
        cmds[args.length] = TestObjItrWithHeapDump.class.getName();
        cmds[args.length + 1] = "test";
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(cmds);

        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldHaveExitValue(0);
        output.shouldContain("Class Histogram (before full gc)");
        output.shouldContain("Class Histogram (after full gc)");
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("test")) {
            System.gc();
            System.exit(0);
        }

        String[][][] modeHeuristics = new String[][][] {
             {{"satb"},    {"adaptive", "compact", "static", "aggressive"}},
             {{"iu"},      {"adaptive", "aggressive"}},
             {{"passive"}, {"passive"}}
        };

        for (String[][] mh : modeHeuristics) {
            String mode = mh[0][0];
            String[] heuristics = mh[1];
            for (String h : heuristics) {
                testWith("-XX:+UnlockDiagnosticVMOptions",
                         "-XX:+UnlockExperimentalVMOptions",
                         "-XX:+UseShenandoahGC",
                         "-XX:-ShenandoahDegeneratedGC",
                         "-XX:ShenandoahGCMode=" + mode,
                         "-XX:ShenandoahGCHeuristics=" + h,
                         "-Xlog:gc+classhisto=trace",
                         "-XX:-ExplicitGCInvokesConcurrent",
                         "-Xmx512M"
                );
            }
        }
    }
}
