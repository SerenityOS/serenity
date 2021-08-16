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
 * @test StackWalkTest
 * @bug 8160064
 * @summary -Xlog:stackwalk should produce logging from the source code
 * @library /test/lib
 * @run driver StackWalkTest
 */

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;

public class StackWalkTest {
    static void analyzeOutputOn(ProcessBuilder pb) throws Exception {
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldContain("Start walking");
        output.shouldContain("fill_in_frames");
        output.shouldHaveExitValue(0);
    }

    static void analyzeOutputOff(ProcessBuilder pb) throws Exception {
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldNotContain("[stackwalk]");
        output.shouldHaveExitValue(0);
    }

    public static void main(String... args) throws Exception {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("-Xlog:stackwalk=debug",
                                                                  InnerClass.class.getName());
        analyzeOutputOn(pb);

        pb = ProcessTools.createJavaProcessBuilder("-Xlog:stackwalk=off",
                                                   InnerClass.class.getName());
        analyzeOutputOff(pb);
    }

    public static class InnerClass {
        public static void main(String[] args) throws Exception {
            System.out.println("Testing stackwalk.");
            StackWalker sw = StackWalker.getInstance();
            sw.forEach(System.out::println);
        }
    }
}
