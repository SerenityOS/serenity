/*
 * Copyright (c) 2017, 2019, Red Hat, Inc. All rights reserved.
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
 * @summary Test that loop mining arguments are sane
 * @requires vm.gc.Shenandoah
 * @requires vm.flavor == "server"
 * @library /test/lib
 * @run driver TestLoopMiningArguments
 */

import java.util.*;

import jdk.test.lib.Asserts;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;

public class TestLoopMiningArguments {

    public static void testWith(String msg, boolean cls, int iters, String... args) throws Exception {
        String[] cmds = Arrays.copyOf(args, args.length + 3);
        cmds[args.length] = "-Xmx128m";
        cmds[args.length + 1] = "-XX:+PrintFlagsFinal";
        cmds[args.length + 2] = "-version";
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(cmds);
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldHaveExitValue(0);
        output.shouldContain("UseCountedLoopSafepoints");
        output.shouldContain("LoopStripMiningIter");

        Asserts.assertEQ(output.firstMatch("(.+?) UseCountedLoopSafepoints.+?= (.+?) (.+?)", 2), Boolean.toString(cls), msg + ", but got wrong CLS");
        Asserts.assertEQ(output.firstMatch("(.+?) LoopStripMiningIter.+?= (.+?) (.+?)", 2), String.valueOf(iters), msg + ", but got wrong LSM");
    }

    public static void main(String[] args) throws Exception {
        testWith("Shenandoah should have CLS and LSM enabled",
                true, 1000,
                "-XX:+UnlockExperimentalVMOptions",
                "-XX:+UseShenandoahGC"
        );

        testWith("Shenandoah with +CLS should set LSM = 1",
                true, 1,
                "-XX:+UnlockExperimentalVMOptions",
                "-XX:+UseShenandoahGC",
                "-XX:+UseCountedLoopSafepoints"
        );

        testWith("Shenandoah GC with +CLS should not override LSM>1",
                true, 10,
                "-XX:+UnlockExperimentalVMOptions",
                "-XX:+UseShenandoahGC",
                "-XX:LoopStripMiningIter=10",
                "-XX:+UseCountedLoopSafepoints"
        );

        testWith("Shenandoah GC with +CLS should not override LSM=1",
                true, 1,
                "-XX:+UnlockExperimentalVMOptions",
                "-XX:+UseShenandoahGC",
                "-XX:LoopStripMiningIter=1",
                "-XX:+UseCountedLoopSafepoints"
        );

        testWith("Shenandoah GC with +CLS should override LSM=0 to 1",
                true, 1,
                "-XX:+UnlockExperimentalVMOptions",
                "-XX:+UseShenandoahGC",
                "-XX:LoopStripMiningIter=0",
                "-XX:+UseCountedLoopSafepoints"
        );

        testWith("Shenandoah GC with -CLS should set LSM = 0",
                false, 0,
                "-XX:+UnlockExperimentalVMOptions",
                "-XX:+UseShenandoahGC",
                "-XX:-UseCountedLoopSafepoints"
        );

        testWith("Shenandoah GC with -CLS should override LSM to 0",
                false, 0,
                "-XX:+UnlockExperimentalVMOptions",
                "-XX:+UseShenandoahGC",
                "-XX:LoopStripMiningIter=10",
                "-XX:-UseCountedLoopSafepoints"
        );
    }

}
