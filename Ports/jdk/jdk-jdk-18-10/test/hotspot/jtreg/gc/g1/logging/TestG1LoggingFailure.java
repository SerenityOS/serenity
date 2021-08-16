/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @test TestG1LoggingFailure
 * @bug 8151034
 * @summary Regression test for G1 logging at OOME
 * @requires vm.gc.G1
 * @library /test/lib
 * @modules java.management
 * @build gc.g1.logging.TestG1LoggingFailure
 * @run main/timeout=300 gc.g1.logging.TestG1LoggingFailure
 */
package gc.g1.logging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.Utils;

public class TestG1LoggingFailure {

    public static void main(String[] args) throws Throwable {
        List<String> options = new ArrayList<>();
        Collections.addAll(options, Utils.getTestJavaOpts());
        Collections.addAll(options,
                "-XX:+UseG1GC",
                "-Xmx20m",
                "-Xmn10m",
                "-Xlog:gc=info",
                "-XX:G1HeapRegionSize=1m"
        );

        options.add(Alloc.class.getName());

        // According to https://bugs.openjdk.java.net/browse/JDK-8146009 failure happens not every time.
        // Will try to reproduce this failure.
        for (int iteration = 0; iteration < 40; ++iteration) {
            startVM(options);
        }
    }

    private static void startVM(List<String> options) throws Throwable, RuntimeException {
        OutputAnalyzer out = ProcessTools.executeTestJvm(options);

        out.shouldNotContain("pure virtual method called");

        if (out.getExitValue() == 0) {
            System.out.println(out.getOutput());
            throw new RuntimeException("Expects Alloc failure.");
        }
    }

    // Simple class to be executed in separate VM.
    static class Alloc {

        public static final int CHUNK = 1024;
        public static ArrayList<Object> arr = new ArrayList<>();

        public static void main(String[] args) {
            try {
                while (true) {
                    arr.add(new byte[CHUNK]);
                }
            } catch (OutOfMemoryError oome) {
            }
            while (true) {
                arr.add(new byte[CHUNK]);
            }
        }
    }
}
