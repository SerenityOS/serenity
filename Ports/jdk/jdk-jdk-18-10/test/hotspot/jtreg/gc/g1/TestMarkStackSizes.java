/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

package gc.g1;

/*
 * @test TestMarkStackSizes
 * @bug 8238855
 * @summary Consistency checks for marking flag related options.
 * @requires vm.gc.G1
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run driver gc.g1.TestMarkStackSizes
 */

import java.util.ArrayList;
import java.util.Collections;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class TestMarkStackSizes {
    private static void runTest(boolean shouldSucceed, String... extraArgs) throws Exception {
        ArrayList<String> testArguments = new ArrayList<String>();

        testArguments.add("-XX:+UseG1GC");
        testArguments.add("-Xmx12m");
        testArguments.add("-XX:+PrintFlagsFinal");
        Collections.addAll(testArguments, extraArgs);
        testArguments.add("-version");

        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(testArguments);

        OutputAnalyzer output = new OutputAnalyzer(pb.start());

        System.out.println(output.getStderr());

        if (shouldSucceed) {
            long markStackSize = Long.parseLong(output.firstMatch("MarkStackSize\\s+=\\s(\\d+)",1));
            long markStackSizeMax = Long.parseLong(output.firstMatch("MarkStackSizeMax\\s+=\\s(\\d+)",1));
            long concGCThreads = Long.parseLong(output.firstMatch("ConcGCThreads\\s+=\\s(\\d+)",1));
            long parallelGCThreads = Long.parseLong(output.firstMatch("ParallelGCThreads\\s+=\\s(\\d+)",1));

            System.out.println("MarkStackSize=" + markStackSize + " MarkStackSizeMax=" + markStackSizeMax +
                               "ConcGCThreads=" + concGCThreads + " ParallelGCThreads=" + parallelGCThreads);

            output.shouldHaveExitValue(0);
        } else {
            output.shouldNotHaveExitValue(0);
        }
    }

    public static void main(String[] args) throws Exception {
        runTest(false, "-XX:MarkStackSize=0");
        runTest(false, "-XX:MarkStackSizeMax=0");

        runTest(true, "-XX:MarkStackSize=100", "-XX:MarkStackSizeMax=101");
        runTest(true, "-XX:MarkStackSize=101", "-XX:MarkStackSizeMax=101");
        runTest(false, "-XX:MarkStackSize=101", "-XX:MarkStackSizeMax=100");

        runTest(true, "-XX:ConcGCThreads=3", "-XX:ParallelGCThreads=3");
        runTest(true, "-XX:ConcGCThreads=0", "-XX:ParallelGCThreads=3");
        runTest(true, "-XX:ConcGCThreads=4", "-XX:ParallelGCThreads=3");

        // With that high ParallelGCThreads the default ergonomics would calculate
        // a mark stack size higher than maximum mark stack size.
        runTest(true, "-XX:ParallelGCThreads=100", "-XX:MarkStackSizeMax=100");
    }
}
