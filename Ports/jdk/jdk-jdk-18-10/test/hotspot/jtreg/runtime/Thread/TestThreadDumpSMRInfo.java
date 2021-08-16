/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     8167108
 * @summary Checks whether jstack reports a "Threads class SMR info" section.
 *
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+EnableThreadSMRStatistics TestThreadDumpSMRInfo
 */

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.JDKToolFinder;

public class TestThreadDumpSMRInfo {
    // jstack tends to be closely bound to the VM that we are running
    // so use getTestJDKTool() instead of getCompileJDKTool() or even
    // getJDKTool() which can fall back to "compile.jdk".
    final static String JSTACK = JDKToolFinder.getTestJDKTool("jstack");
    final static String PID = "" + ProcessHandle.current().pid();

    // Here's a sample "Threads class SMR info" section:
    //
    // Threads class SMR info:
    // _java_thread_list=0x0000000000ce8da0, length=23, elements={
    // 0x000000000043a800, 0x0000000000aee800, 0x0000000000b06800, 0x0000000000b26000,
    // 0x0000000000b28800, 0x0000000000b2b000, 0x0000000000b2e000, 0x0000000000b30000,
    // 0x0000000000b32800, 0x0000000000b35000, 0x0000000000b3f000, 0x0000000000b41800,
    // 0x0000000000b44000, 0x0000000000b46800, 0x0000000000b48800, 0x0000000000b53000,
    // 0x0000000000b55800, 0x0000000000b57800, 0x0000000000b5a000, 0x0000000000b5c800,
    // 0x0000000000cc8800, 0x0000000000fd9800, 0x0000000000ef4800
    // }
    // _java_thread_list_alloc_cnt=24, _java_thread_list_free_cnt=23, _java_thread_list_max=23, _nested_thread_list_max=0
    // _delete_lock_wait_cnt=0, _delete_lock_wait_max=0
    // _to_delete_list_cnt=0, _to_delete_list_max=1

    final static String HEADER_STR = "Threads class SMR info:";

    static boolean verbose = false;

    public static void main(String[] args) throws Exception {
        if (args.length != 0) {
            int arg_i = 0;
            if (args[arg_i].equals("-v")) {
                verbose = true;
                arg_i++;
            }
        }

        ProcessBuilder pb = new ProcessBuilder(JSTACK, PID);
        OutputAnalyzer output = new OutputAnalyzer(pb.start());

        if (verbose) {
            System.out.println("stdout: " + output.getStdout());
        }

        output.shouldHaveExitValue(0);
        System.out.println("INFO: jstack ran successfully.");

        output.shouldContain(HEADER_STR);
        System.out.println("INFO: Found: '" + HEADER_STR + "' in jstack output.");

        System.out.println("Test PASSED.");
    }
}
