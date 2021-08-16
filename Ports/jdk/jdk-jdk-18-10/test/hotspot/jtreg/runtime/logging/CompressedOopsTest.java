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
 * @test
 * @bug 8149991
 * @requires vm.bits == 64 & vm.opt.final.UseCompressedOops == true
 * @summary -Xlog:gc+heap+coops=info should have output from the code
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run driver CompressedOopsTest
 */

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.Platform;
import jdk.test.lib.process.ProcessTools;

public class CompressedOopsTest {
    static void analyzeOutputOn(ProcessBuilder pb) throws Exception {
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldContain("[gc,heap,coops] Heap address");
        output.shouldHaveExitValue(0);
    }

    static void analyzeOutputOff(ProcessBuilder pb) throws Exception {
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldNotContain("[gc,heap,coops]");
        output.shouldHaveExitValue(0);
    }

    public static void main(String[] args) throws Exception {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("-XX:+UseCompressedOops",
                                                   "-Xlog:gc+heap+coops=debug",
                                                   InnerClass.class.getName());
        analyzeOutputOn(pb);

        pb = ProcessTools.createJavaProcessBuilder("-XX:+UseCompressedOops",
                                                   "-Xlog:gc+heap+coops",
                                                   InnerClass.class.getName());
        // No coops logging on info level.
        analyzeOutputOff(pb);

        pb = ProcessTools.createJavaProcessBuilder("-XX:+UseCompressedOops",
                                                   "-Xlog:gc+heap+coops=off",
                                                   InnerClass.class.getName());
        analyzeOutputOff(pb);
    }

    public static class InnerClass {
        public static void main(String[] args) throws Exception {
            System.out.println("Compressed Oops (gc+heap+coops) test");
        }
    }
}
