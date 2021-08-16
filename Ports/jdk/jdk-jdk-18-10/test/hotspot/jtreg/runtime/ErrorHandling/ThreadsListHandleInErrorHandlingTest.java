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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.Platform;
import jdk.test.lib.process.ProcessTools;

/*
 * @test
 * @requires (vm.debug == true)
 * @bug 8167108
 * @summary ThreadsListHandle info should be in error handling output.
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @run driver ThreadsListHandleInErrorHandlingTest
 */

/*
 * This test was created using SafeFetchInErrorHandlingTest.java
 * as a guide.
 */
public class ThreadsListHandleInErrorHandlingTest {
  public static void main(String[] args) throws Exception {

    // The -XX:ErrorHandlerTest=N option requires debug bits.
    // Need to disable ShowRegistersOnAssert: that flag causes registers to be shown, which calls os::print_location,
    // which - as part of its checks - will iterate the threads list under a ThreadListHandle, changing the max nesting
    // counters and confusing this test.
    ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
        "-XX:+UnlockDiagnosticVMOptions",
        "-XX:+EnableThreadSMRStatistics",
        "-Xmx100M",
        "-XX:ErrorHandlerTest=16",
        "-XX:-CreateCoredumpOnCrash",
        "-XX:-ShowRegistersOnAssert",
        "-version");

    OutputAnalyzer output_detail = new OutputAnalyzer(pb.start());

    // We should have crashed with a specific fatal error:
    output_detail.shouldMatch("# A fatal error has been detected by the Java Runtime Environment:.*");
    System.out.println("Found fatal error header.");
    output_detail.shouldMatch("# +fatal error: Force crash with an active ThreadsListHandle.");
    System.out.println("Found specific fatal error.");

    // Extract hs_err_pid file.
    String hs_err_file = output_detail.firstMatch("# *(\\S*hs_err_pid\\d+\\.log)", 1);
    if (hs_err_file == null) {
        throw new RuntimeException("Did not find hs_err_pid file in output.\n");
    }

    File f = new File(hs_err_file);
    if (!f.exists()) {
        throw new RuntimeException("hs_err_pid file missing at "
                                   + f.getAbsolutePath() + ".\n");
    }

    System.out.println("Found hs_err_pid file. Scanning...");

    FileInputStream fis = new FileInputStream(f);
    BufferedReader br = new BufferedReader(new InputStreamReader(fis));
    String line = null;

    Pattern [] pattern = new Pattern[] {
        // The "Current thread" line should show a hazard ptr
        // and no nested hazard ptrs:
        Pattern.compile("Current thread .* _threads_hazard_ptr=0x[0-9A-Fa-f][0-9A-Fa-f]*, _nested_threads_hazard_ptr_cnt=0.*"),
        // We should have a section of Threads class SMR info:
        Pattern.compile("Threads class SMR info:"),
        // We should have had a single nested ThreadsListHandle since
        // ThreadsSMRSupport::print_info_on() now protects itself with
        // a ThreadsListHandle:
        Pattern.compile(".*, _nested_thread_list_max=1"),
        // The current thread (marked with '=>') in the threads list
        // should show no nested hazard ptrs:
        Pattern.compile("=>.* JavaThread \"main\" .*, _nested_threads_hazard_ptr_cnt=0.*"),
    };
    int currentPattern = 0;

    String lastLine = null;
    while ((line = br.readLine()) != null) {
        if (currentPattern < pattern.length) {
            if (pattern[currentPattern].matcher(line).matches()) {
                System.out.println("Found: " + line + ".");
                currentPattern++;
            }
        }
        lastLine = line;
    }
    br.close();

    if (currentPattern < pattern.length) {
        throw new RuntimeException("hs_err_pid file incomplete (first missing pattern: " +  currentPattern + ")");
    }

    if (!lastLine.equals("END.")) {
        throw new RuntimeException("hs-err file incomplete (missing END marker.)");
    } else {
        System.out.println("End marker found.");
    }

    System.out.println("Done scanning hs_err_pid_file.");
    System.out.println("PASSED.");
  }
}
