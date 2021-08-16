/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2019, SAP. All rights reserved.
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
 * @bug 8220786
 * @summary Test ErrorFileToStderr and ErrorFileToStdout
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 * @requires (vm.debug == true)
 * @run driver ErrorFileRedirectTest
 */

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.regex.Pattern;

public class ErrorFileRedirectTest {

  public static void do_test(boolean redirectStdout, boolean redirectStderr) throws Exception {

    ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
            "-Xmx64M",
            "-XX:-CreateCoredumpOnCrash",
            "-XX:ErrorHandlerTest=14",
            "-XX:" + (redirectStdout ? "+" : "-") + "ErrorFileToStdout",
            "-XX:" + (redirectStderr ? "+" : "-") + "ErrorFileToStderr",
            "-version");

    OutputAnalyzer output_detail = new OutputAnalyzer(pb.start());

    // we should have crashed with a SIGSEGV
    output_detail.shouldMatch("# A fatal error has been detected by the Java Runtime Environment:.*");
    output_detail.shouldMatch("# +(?:SIGSEGV|SIGBUS|EXCEPTION_ACCESS_VIOLATION).*");

    // If no redirection happened, we should find a mention of the file in the output.
    String hs_err_file = output_detail.firstMatch("# *(\\S*hs_err_pid\\d+\\.log)", 1);
    if (redirectStdout == false && redirectStderr == false) {
      if (hs_err_file == null) {
        throw new RuntimeException("Expected hs-err file but none found.");
      } else {
        System.out.println("Found hs error file mentioned as expected: " + hs_err_file);
      }
    } else {
      if (hs_err_file != null) {
        throw new RuntimeException("Found unexpected mention of hs-err file (we did redirect the output so no file should have been written).");
      } else {
        System.out.println("No mention of an hs-err file - ok! ");
      }
    }

    // Check the output. Note that since stderr was specified last it has preference if both are set.
    if (redirectStdout == true && redirectStderr == false) {
      output_detail.stdoutShouldContain("---------------  S U M M A R Y ------------");
      output_detail.stderrShouldNotContain("---------------  S U M M A R Y ------------");
      System.out.println("Found report on stderr - ok! ");
    } else if (redirectStderr == true) {
      output_detail.stderrShouldContain("---------------  S U M M A R Y ------------");
      output_detail.stdoutShouldNotContain("---------------  S U M M A R Y ------------");
      System.out.println("Found report on stdout - ok! ");
    }

    System.out.println("OK.");

  }

  public static void main(String[] args) throws Exception {
    do_test(false, false);
    do_test(false, true);
    do_test(true, false);
    do_test(true, true);
  }

}


