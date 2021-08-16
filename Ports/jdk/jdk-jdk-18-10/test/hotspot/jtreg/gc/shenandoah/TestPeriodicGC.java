/*
 * Copyright (c) 2017, 2020, Red Hat, Inc. All rights reserved.
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
 * @summary Test that periodic GC is working
 * @requires vm.gc.Shenandoah
 * @library /test/lib
 * @run driver TestPeriodicGC
 */

import java.util.*;

import jdk.test.lib.Asserts;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;

public class TestPeriodicGC {

    public static void testWith(String msg, boolean periodic, String... args) throws Exception {
        String[] cmds = Arrays.copyOf(args, args.length + 2);
        cmds[args.length] = TestPeriodicGC.class.getName();
        cmds[args.length + 1] = "test";
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(cmds);

        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldHaveExitValue(0);
        if (periodic && !output.getOutput().contains("Trigger: Time since last GC")) {
            throw new AssertionError(msg + ": Should have periodic GC in logs");
        }
        if (!periodic && output.getOutput().contains("Trigger: Time since last GC")) {
            throw new AssertionError(msg + ": Should not have periodic GC in logs");
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("test")) {
            Thread.sleep(5000); // stay idle
            return;
        }

        String[] enabled = new String[] {
                "adaptive",
                "compact",
                "static",
        };

        for (String h : enabled) {
            testWith("Zero interval with " + h,
                    false,
                    "-Xlog:gc",
                    "-XX:+UnlockDiagnosticVMOptions",
                    "-XX:+UnlockExperimentalVMOptions",
                    "-XX:+UseShenandoahGC",
                    "-XX:ShenandoahGCHeuristics=" + h,
                    "-XX:ShenandoahGuaranteedGCInterval=0"
            );

            testWith("Short interval with " + h,
                    true,
                    "-Xlog:gc",
                    "-XX:+UnlockDiagnosticVMOptions",
                    "-XX:+UnlockExperimentalVMOptions",
                    "-XX:+UseShenandoahGC",
                    "-XX:ShenandoahGCHeuristics=" + h,
                    "-XX:ShenandoahGuaranteedGCInterval=1000"
            );

            testWith("Long interval with " + h,
                    false,
                    "-Xlog:gc",
                    "-XX:+UnlockDiagnosticVMOptions",
                    "-XX:+UnlockExperimentalVMOptions",
                    "-XX:+UseShenandoahGC",
                    "-XX:ShenandoahGCHeuristics=" + h,
                    "-XX:ShenandoahGuaranteedGCInterval=100000" // deliberately too long
            );
        }

        testWith("Zero interval with iu mode",
                 false,
                 "-Xlog:gc",
                 "-XX:+UnlockDiagnosticVMOptions",
                 "-XX:+UnlockExperimentalVMOptions",
                 "-XX:+UseShenandoahGC",
                 "-XX:ShenandoahGCMode=iu",
                 "-XX:ShenandoahGuaranteedGCInterval=0"
        );

        testWith("Short interval with iu mode",
                 true,
                 "-Xlog:gc",
                 "-XX:+UnlockDiagnosticVMOptions",
                 "-XX:+UnlockExperimentalVMOptions",
                 "-XX:+UseShenandoahGC",
                 "-XX:ShenandoahGCMode=iu",
                 "-XX:ShenandoahGuaranteedGCInterval=1000"
        );

        testWith("Long interval with iu mode",
                 false,
                 "-Xlog:gc",
                 "-XX:+UnlockDiagnosticVMOptions",
                 "-XX:+UnlockExperimentalVMOptions",
                 "-XX:+UseShenandoahGC",
                 "-XX:ShenandoahGCMode=iu",
                 "-XX:ShenandoahGuaranteedGCInterval=100000" // deliberately too long
        );

        testWith("Short interval with aggressive",
                 false,
                 "-Xlog:gc",
                 "-XX:+UnlockDiagnosticVMOptions",
                 "-XX:+UnlockExperimentalVMOptions",
                 "-XX:+UseShenandoahGC",
                 "-XX:ShenandoahGCHeuristics=aggressive",
                 "-XX:ShenandoahGuaranteedGCInterval=1000"
        );

        testWith("Zero interval with passive",
                 false,
                 "-Xlog:gc",
                 "-XX:+UnlockDiagnosticVMOptions",
                 "-XX:+UnlockExperimentalVMOptions",
                 "-XX:+UseShenandoahGC",
                 "-XX:ShenandoahGCMode=passive",
                 "-XX:ShenandoahGuaranteedGCInterval=0"
        );

        testWith("Short interval with passive",
                 false,
                 "-Xlog:gc",
                 "-XX:+UnlockDiagnosticVMOptions",
                 "-XX:+UnlockExperimentalVMOptions",
                 "-XX:+UseShenandoahGC",
                 "-XX:ShenandoahGCMode=passive",
                 "-XX:ShenandoahGuaranteedGCInterval=1000"
        );
    }

}
