/*
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8263707
 * @summary Test range check for constant array and NewMultiArray is removed properly
 * @author Hui Shi
 *
 * @requires vm.flagless
 * @requires vm.debug == true & vm.compiler1.enabled
 *
 * @library /test/lib
 *
 * @run driver compiler.c1.TestRangeCheckEliminated
 */

package compiler.c1;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class TestRangeCheckEliminated {
    static final String eliminated = "can be fully eliminated";
    public static void main(String[] args) throws Throwable {
        boolean error = false;
        String[] procArgs = new String[] {
            "-XX:CompileCommand=compileonly,*test_constant_array::constant_array_rc",
            "-XX:TieredStopAtLevel=1",
            "-XX:+TraceRangeCheckElimination",
            "-XX:-BackgroundCompilation",
            "-XX:CompileThreshold=500",
            test_constant_array.class.getName()
         };

        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(procArgs);
        String output = new OutputAnalyzer(pb.start()).getOutput();
        // should have 2 "can be fully eliminated"
        System.out.println(output);
        if ((output.split(eliminated, -1).length - 1) == 2) {
            System.out.println("test_constant_array pass");
        } else {
            System.out.println("test_constant_array fail");
            error = true;
        }

        procArgs = new String[] {
            "-XX:CompileCommand=compileonly,*test_multi_constant_array::multi_constant_array_rc",
            "-XX:TieredStopAtLevel=1",
            "-XX:+TraceRangeCheckElimination",
            "-XX:-BackgroundCompilation",
            "-XX:CompileThreshold=500",
            test_multi_constant_array.class.getName()
        };

        pb = ProcessTools.createJavaProcessBuilder(procArgs);
        output = new OutputAnalyzer(pb.start()).getOutput();
        // should have 1 "can be fully eliminated"
        System.out.println(output);
        if ((output.split(eliminated, -1).length - 1) == 1) {
            System.out.println("test_multi_constant_array pass");
        } else {
            System.out.println("test_multi_constant_array fail");
            error = true;
        }

        procArgs = new String[] {
            "-XX:CompileCommand=compileonly,*test_multi_new_array::multi_new_array_rc",
            "-XX:TieredStopAtLevel=1",
            "-XX:+TraceRangeCheckElimination",
            "-XX:-BackgroundCompilation",
            "-XX:CompileThreshold=500",
            test_multi_new_array.class.getName()
         };

        pb = ProcessTools.createJavaProcessBuilder(procArgs);
        output = new OutputAnalyzer(pb.start()).getOutput();
        // should have 2 "can be fully eliminated"
        System.out.println(output);
        if ((output.split(eliminated, -1).length - 1) == 2) {
            System.out.println("test_multi_new_array pass");
        } else {
            System.out.println("test_multi_new_array fail");
            error = true;
        }

        if (error) {
            throw new InternalError();
        }
    }

    public static class test_constant_array {
        static final int constant_array[] =
            {50,60,55,67,70,62,65,70,70,81,72,66,77,80,69};
        static void constant_array_rc() {
            constant_array[1] += 5;
        }

        public static void main(String[] args) {
            for(int i = 0; i < 1_000; i++) {
                constant_array_rc();
            }
        }
    }

    public static class test_multi_constant_array {
        static final int constant_multi_array[][] = {
            {50,60,55,67,70}, {62,65,70,70,81}, {72,66,77,80,69}};
        static void multi_constant_array_rc() {
            constant_multi_array[2][3] += 5;
        }

        public static void main(String[] args) {
            for(int i = 0; i < 1_000; i++) {
                multi_constant_array_rc();
            }
        }
    }

    public static class test_multi_new_array {
        static void foo(int i) {}
        static void multi_new_array_rc(int index) {
            int na[] = new int[800];
            int nma[][] = new int[600][2];
            nma[20][1] += 5;   // optimize rc on NewMultiArray first dimension
            nma[index][0] = 0; // index < 600 after this statement
            foo(na[index]);    // index must < 800, remove rc
        }

        public static void main(String[] args) {
            for(int i = 0; i < 600; i++) {
                multi_new_array_rc(i);
            }
        }
    }
}
