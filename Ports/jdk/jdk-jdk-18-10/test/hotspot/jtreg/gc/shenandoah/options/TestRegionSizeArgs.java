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
 * @summary Test that Shenandoah region size args are checked
 * @requires vm.gc.Shenandoah
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run driver TestRegionSizeArgs
 */

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;

public class TestRegionSizeArgs {
    public static void main(String[] args) throws Exception {
        testInvalidRegionSizes();
        testMinRegionSize();
        testMaxRegionSize();
    }

    private static void testInvalidRegionSizes() throws Exception {

        {
            ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("-XX:+UnlockExperimentalVMOptions",
                    "-XX:+UseShenandoahGC",
                    "-Xms4m",
                    "-Xmx1g",
                    "-version");
            OutputAnalyzer output = new OutputAnalyzer(pb.start());
            output.shouldHaveExitValue(0);
        }

        {
            ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("-XX:+UnlockExperimentalVMOptions",
                    "-XX:+UseShenandoahGC",
                    "-Xms8m",
                    "-Xmx1g",
                    "-version");
            OutputAnalyzer output = new OutputAnalyzer(pb.start());
            output.shouldHaveExitValue(0);
        }

        {
            ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("-XX:+UnlockExperimentalVMOptions",
                    "-XX:+UseShenandoahGC",
                    "-Xms100m",
                    "-Xmx1g",
                    "-XX:ShenandoahRegionSize=200m",
                    "-version");
            OutputAnalyzer output = new OutputAnalyzer(pb.start());
            output.shouldMatch("Invalid -XX:ShenandoahRegionSize option");
            output.shouldHaveExitValue(1);
        }

        {
            ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("-XX:+UnlockExperimentalVMOptions",
                    "-XX:+UseShenandoahGC",
                    "-Xms100m",
                    "-Xmx1g",
                    "-XX:ShenandoahRegionSize=9m",
                    "-version");
            OutputAnalyzer output = new OutputAnalyzer(pb.start());
            output.shouldHaveExitValue(0);
        }

        {
            ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("-XX:+UnlockExperimentalVMOptions",
                    "-XX:+UseShenandoahGC",
                    "-Xms100m",
                    "-Xmx1g",
                    "-XX:ShenandoahRegionSize=255K",
                    "-version");
            OutputAnalyzer output = new OutputAnalyzer(pb.start());
            output.shouldMatch("Invalid -XX:ShenandoahRegionSize option");
            output.shouldHaveExitValue(1);
        }

        {
            ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("-XX:+UnlockExperimentalVMOptions",
                    "-XX:+UseShenandoahGC",
                    "-Xms100m",
                    "-Xmx1g",
                    "-XX:ShenandoahRegionSize=260K",
                    "-version");
            OutputAnalyzer output = new OutputAnalyzer(pb.start());
            output.shouldHaveExitValue(0);
        }

        {
            ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("-XX:+UnlockExperimentalVMOptions",
                    "-XX:+UseShenandoahGC",
                    "-Xms1g",
                    "-Xmx1g",
                    "-XX:ShenandoahRegionSize=32M",
                    "-version");
            OutputAnalyzer output = new OutputAnalyzer(pb.start());
            output.shouldHaveExitValue(0);
        }

        {
            ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("-XX:+UnlockExperimentalVMOptions",
                    "-XX:+UseShenandoahGC",
                    "-Xms1g",
                    "-Xmx1g",
                    "-XX:ShenandoahRegionSize=64M",
                    "-version");
            OutputAnalyzer output = new OutputAnalyzer(pb.start());
            output.shouldMatch("Invalid -XX:ShenandoahRegionSize option");
            output.shouldHaveExitValue(1);
        }

        {
            ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("-XX:+UnlockExperimentalVMOptions",
                    "-XX:+UseShenandoahGC",
                    "-Xms1g",
                    "-Xmx1g",
                    "-XX:ShenandoahRegionSize=256K",
                    "-version");
            OutputAnalyzer output = new OutputAnalyzer(pb.start());
            output.shouldHaveExitValue(0);
        }

        {
            ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("-XX:+UnlockExperimentalVMOptions",
                    "-XX:+UseShenandoahGC",
                    "-Xms1g",
                    "-Xmx1g",
                    "-XX:ShenandoahRegionSize=128K",
                    "-version");
            OutputAnalyzer output = new OutputAnalyzer(pb.start());
            output.shouldMatch("Invalid -XX:ShenandoahRegionSize option");
            output.shouldHaveExitValue(1);
        }
    }

    private static void testMinRegionSize() throws Exception {

        {
            ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("-XX:+UnlockExperimentalVMOptions",
                    "-XX:+UseShenandoahGC",
                    "-Xms100m",
                    "-Xmx1g",
                    "-XX:ShenandoahMinRegionSize=255K",
                    "-version");
            OutputAnalyzer output = new OutputAnalyzer(pb.start());
            output.shouldMatch("Invalid -XX:ShenandoahMinRegionSize option");
            output.shouldHaveExitValue(1);
        }

        {
            ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("-XX:+UnlockExperimentalVMOptions",
                    "-XX:+UseShenandoahGC",
                    "-Xms100m",
                    "-Xmx1g",
                    "-XX:ShenandoahMinRegionSize=1M",
                    "-XX:ShenandoahMaxRegionSize=260K",
                    "-version");
            OutputAnalyzer output = new OutputAnalyzer(pb.start());
            output.shouldMatch("Invalid -XX:ShenandoahMinRegionSize or -XX:ShenandoahMaxRegionSize");
            output.shouldHaveExitValue(1);
        }
        {
            ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("-XX:+UnlockExperimentalVMOptions",
                    "-XX:+UseShenandoahGC",
                    "-Xms100m",
                    "-Xmx1g",
                    "-XX:ShenandoahMinRegionSize=200m",
                    "-version");
            OutputAnalyzer output = new OutputAnalyzer(pb.start());
            output.shouldMatch("Invalid -XX:ShenandoahMinRegionSize option");
            output.shouldHaveExitValue(1);
        }

        {
            ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("-XX:+UnlockExperimentalVMOptions",
                    "-XX:+UseShenandoahGC",
                    "-Xms100m",
                    "-Xmx1g",
                    "-XX:ShenandoahMinRegionSize=9m",
                    "-version");
            OutputAnalyzer output = new OutputAnalyzer(pb.start());
            output.shouldHaveExitValue(0);
        }

    }

    private static void testMaxRegionSize() throws Exception {

        {
            ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("-XX:+UnlockExperimentalVMOptions",
                    "-XX:+UseShenandoahGC",
                    "-Xms100m",
                    "-Xmx1g",
                    "-XX:ShenandoahMaxRegionSize=255K",
                    "-version");
            OutputAnalyzer output = new OutputAnalyzer(pb.start());
            output.shouldMatch("Invalid -XX:ShenandoahMaxRegionSize option");
            output.shouldHaveExitValue(1);
        }

        {
            ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("-XX:+UnlockExperimentalVMOptions",
                    "-XX:+UseShenandoahGC",
                    "-Xms100m",
                    "-Xmx1g",
                    "-XX:ShenandoahMinRegionSize=1M",
                    "-XX:ShenandoahMaxRegionSize=260K",
                    "-version");
            OutputAnalyzer output = new OutputAnalyzer(pb.start());
            output.shouldMatch("Invalid -XX:ShenandoahMinRegionSize or -XX:ShenandoahMaxRegionSize");
            output.shouldHaveExitValue(1);
        }
    }
}
