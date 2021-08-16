/*
 * Copyright (c) 2018, 2019, Red Hat, Inc. All rights reserved.
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
 * @library /test/lib
 * @run driver TestClassUnloadingArguments
 */

import java.util.*;

import jdk.test.lib.Asserts;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;

public class TestClassUnloadingArguments {

    public static void testWith(String msg, boolean cu, boolean cuConc, String... args) throws Exception {
        String[] cmds = Arrays.copyOf(args, args.length + 3);
        cmds[args.length] = "-Xmx128m";
        cmds[args.length + 1] = "-XX:+PrintFlagsFinal";
        cmds[args.length + 2] = "-version";
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(cmds);
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldHaveExitValue(0);
        output.shouldContain("ClassUnloading");
        output.shouldContain("ClassUnloadingWithConcurrentMark");

        Asserts.assertEQ(output.firstMatch("(.+?) ClassUnloading.+?= (.+?) (.+?)", 2),
                Boolean.toString(cu),
                msg + ", but got wrong ClassUnloading");
        Asserts.assertEQ(output.firstMatch("(.+?) ClassUnloadingWithConcurrentMark.+?= (.+?) (.+?)", 2),
                Boolean.toString(cuConc),
                msg + ", but got wrong ClassUnloadingWithConcurrentMark");
    }

    public static void main(String[] args) throws Exception {
        testDefaultGC();
        testShenandoah();
    }

    public static void testDefaultGC() throws Exception {
        testWith("Default GC should have class unloading enabled",
                true, true);

        testWith("Default GC should disable everything",
                false, false,
                "-XX:-ClassUnloading");

        testWith("Default GC should disable conc unload",
                true, false,
                "-XX:-ClassUnloadingWithConcurrentMark");

        testWith("Default GC should not let conc unload to be enabled separately",
                false, false,
                "-XX:-ClassUnloading",
                "-XX:+ClassUnloadingWithConcurrentMark");
    }

    public static void testShenandoah() throws Exception {
        testWith("Shenandoah GC should have class unloading enabled",
                true, true,
                "-XX:+UnlockExperimentalVMOptions",
                "-XX:+UseShenandoahGC");

        testWith("Shenandoah GC should disable everything",
                false, false,
                "-XX:+UnlockExperimentalVMOptions",
                "-XX:+UseShenandoahGC",
                "-XX:-ClassUnloading");

        testWith("Shenandoah GC should enable conc unload",
                true, true,
                "-XX:+UnlockExperimentalVMOptions",
                "-XX:+UseShenandoahGC",
                "-XX:+ClassUnloadingWithConcurrentMark");

        testWith("Shenandoah GC should not let conc unload to be enabled separately",
                false, false,
                "-XX:+UnlockExperimentalVMOptions",
                "-XX:+UseShenandoahGC",
                "-XX:-ClassUnloading",
                "-XX:+ClassUnloadingWithConcurrentMark");
    }

}
