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

/**
 * @test
 * @summary Test allocation of small object to result OOM, but not to crash JVM
 * @requires vm.gc.Shenandoah
 * @library /test/lib
 * @run driver TestAllocSmallObj
 */

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class TestAllocSmallObj {

    static final int COUNT = 16 * 1024 * 1024;

    static volatile Object sink;

    public static void work() throws Exception {
        Object[] root = new Object[COUNT];
        sink = root;
        for (int c = 0; c < COUNT; c++) {
            root[c] = new Object();
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 0) {
            work();
            return;
        }

        {
            ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
                    "-Xmx16m",
                    "-XX:+UnlockExperimentalVMOptions",
                    "-XX:+UseShenandoahGC",
                    TestAllocSmallObj.class.getName(),
                    "test");

            OutputAnalyzer analyzer = new OutputAnalyzer(pb.start());
            analyzer.shouldHaveExitValue(1);
            analyzer.shouldContain("java.lang.OutOfMemoryError: Java heap space");
        }

        {
            ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
                    "-Xmx1g",
                    "-XX:+UnlockExperimentalVMOptions",
                    "-XX:+UseShenandoahGC",
                    TestAllocSmallObj.class.getName(),
                    "test");

            OutputAnalyzer analyzer = new OutputAnalyzer(pb.start());
            analyzer.shouldHaveExitValue(0);
            analyzer.shouldNotContain("java.lang.OutOfMemoryError: Java heap space");
        }
    }
}
