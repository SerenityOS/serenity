/*
 * Copyright (c) 2021, Huawei Technologies Co. Ltd. All rights reserved.
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
 * @test TestG1SkipCompaction
 * @summary Test for JDK-8262068 Improve G1 Full GC by skipping compaction
 *          for regions with high survival ratio.
 * @requires vm.gc.G1
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run main/othervm -Xms256m -Xmx256m TestG1SkipCompaction
 */
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.ArrayList;
import java.util.List;

import jdk.test.lib.Platform;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class TestG1SkipCompaction {
    public static void runTest() throws Exception {
        final String[] arguments = {
            "-XX:+UseG1GC",
            "-XX:MarkSweepDeadRatio=3",
            "-Xmx8m",
            "-Xms8M",
            "-Xlog:gc+phases=trace",
            "-XX:G1HeapRegionSize=1m",
            GCTest.class.getName()
            };
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(arguments);
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        System.out.println(output.getStdout());

        String pattern = ".*skip compaction region.*";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(output.getStdout());

        if (!m.find()) {
            throw new RuntimeException("Could not find any not compacted regions in the logs");
        }
    }

    public static void main(String[] args) throws Exception {
        runTest();
    }

    static class GCTest {
        public static List<char[]> memory;
        public static void main(String[] args) throws Exception {
            memory = new ArrayList<>();
            try {
                while (true) {
                    memory.add(new char[8 * 1024]);
                    System.gc();
                }
            } catch (OutOfMemoryError e) {
                memory = null;
                System.gc();
            }
        }
    }
}

