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
 * @bug 8221738
 * @summary Test that subsequent crashes will overwrite the file given to -XX:ErrorFile (unless %a is specified
 *           in the error file name)
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 * @requires (vm.debug == true)
 * @run driver ErrorFileOverwriteTest
 */

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

import java.io.*;
import java.util.regex.Pattern;

public class ErrorFileOverwriteTest {

  private static File findHsErrorFileInOutput(OutputAnalyzer output) {

    String hs_err_file = output.firstMatch("# *(\\S*hs_err_pid.*\\.log)", 1);
    if(hs_err_file ==null) {
      throw new RuntimeException("Did not find hs-err file in output.\n");
    }

    File f = new File(hs_err_file);
    if (!f.exists()) {
      throw new RuntimeException("hs-err file missing at "
              + f.getAbsolutePath() + ".\n");
    }

    return f;

  }

  private static void scanHsErrorFileForContent(File f, Pattern[] pattern) throws IOException {
    FileInputStream fis = new FileInputStream(f);
    BufferedReader br = new BufferedReader(new InputStreamReader(fis));
    String line = null;

    int currentPattern = 0;

    String lastLine = null;
    while ((line = br.readLine()) != null && currentPattern < pattern.length) {
      if (pattern[currentPattern].matcher(line).matches()) {
        System.out.println("Found: " + line + ".");
        currentPattern++;
      }
      lastLine = line;
    }
    br.close();

    if (currentPattern < pattern.length) {
      throw new RuntimeException("hs-err file incomplete (first missing pattern: " +  pattern[currentPattern] + ")");
    }

  }

  public static void do_test(boolean with_percent_p) throws Exception {

    // Crash twice.
    //
    // Second crash should, given an error file Without %p,
    // overwrite the first file. With %p it should not.

    String errorFileStem = "hs_err_pid_test";
    String errorFileName = errorFileStem + (with_percent_p ? "%p" : "") + ".log";

    System.out.println("Testing with error file name " + errorFileName + "...");

    System.out.println("First crash...");

    ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
            "-Xmx64M",
            "-XX:-CreateCoredumpOnCrash",
            "-XX:ErrorHandlerTest=1",
            "-XX:ErrorFile=" + errorFileName,
            "-version");

    OutputAnalyzer output_detail = new OutputAnalyzer(pb.start());

    output_detail.shouldMatch("# A fatal error has been detected by the Java Runtime Environment:.*");
    output_detail.shouldMatch("# An error report file with more information is saved as:.*");
    output_detail.shouldMatch("# " + errorFileStem + ".*");
    System.out.println("First crash: Found expected output on tty. Ok.");

    File f = findHsErrorFileInOutput(output_detail);
    System.out.println("First crash: Found hs error file at " + f.getAbsolutePath());

    scanHsErrorFileForContent(f, new Pattern[] {
            Pattern.compile("# *Internal Error.*"),
            Pattern.compile("Command Line:.*-XX:ErrorHandlerTest=1.*-XX:ErrorFile=" + errorFileStem + ".*")
    });
    System.out.println("First crash: hs error content as expected. Ok.");


    System.out.println("Second crash...");

    pb = ProcessTools.createJavaProcessBuilder(
            "-Xmx64M",
            "-XX:-CreateCoredumpOnCrash",
            "-XX:ErrorHandlerTest=2", // << now 2
            "-XX:ErrorFile=" + errorFileName,
            "-version");

    output_detail = new OutputAnalyzer(pb.start());

    output_detail.shouldMatch("# A fatal error has been detected by the Java Runtime Environment:.*");
    output_detail.shouldMatch("# An error report file with more information is saved as:.*");
    output_detail.shouldMatch("# " + errorFileStem + ".*");
    System.out.println("Second crash: Found expected output on tty. Ok.");

    File f2 = findHsErrorFileInOutput(output_detail);
    System.out.println("Second crash: Found hs error file at " + f2.getAbsolutePath());

    if (with_percent_p) {
      if (f2.getAbsolutePath() == f.getAbsolutePath()) {
        throw new RuntimeException("Unexpected overwriting of error file");
      }
    }

    scanHsErrorFileForContent(f2, new Pattern[] {
            Pattern.compile("# *Internal Error.*"),
            Pattern.compile("Command Line:.*-XX:ErrorHandlerTest=2.*-XX:ErrorFile=" + errorFileStem + ".*")
    });
    System.out.println("Second crash: hs error content as expected. Ok.");

  }

  public static void main(String[] args) throws Exception {
    do_test(false);
    do_test(true);
  }

}


