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
 * @summary Test OOME in separate thread is recoverable
 * @requires vm.gc.Shenandoah
 * @library /test/lib
 * @run driver TestThreadFailure
 */

import java.util.*;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class TestThreadFailure {

    static final int SIZE = 1024;
    static final int COUNT = 16;

    static class NastyThread extends Thread {
        @Override
        public void run() {
            List<Object> root = new ArrayList<Object>();
            while (true) {
                root.add(new Object[SIZE]);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 0) {
            for (int t = 0; t < COUNT; t++) {
                Thread thread = new NastyThread();
                thread.start();
                thread.join();
            }
            System.out.println("All good");
            return;
        }

        {
            ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
                    "-Xmx32m",
                    "-XX:+UnlockExperimentalVMOptions",
                    "-XX:+UseShenandoahGC",
                    TestThreadFailure.class.getName(),
                    "test");

            OutputAnalyzer analyzer = new OutputAnalyzer(pb.start());
            analyzer.shouldHaveExitValue(0);
            analyzer.shouldContain("java.lang.OutOfMemoryError");
            analyzer.shouldContain("All good");
        }
    }
}
