/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8065896
 * @summary Synchronous signals during error reporting may terminate or hang VM process
 * @library /test/lib
 * @requires vm.debug
 * @requires os.family != "windows"
 * @author Thomas Stuefe (SAP)
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run driver SecondaryErrorTest
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class SecondaryErrorTest {


  public static void main(String[] args) throws Exception {
    ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
        "-XX:+UnlockDiagnosticVMOptions",
        "-Xmx100M",
        "-XX:-CreateCoredumpOnCrash",
        "-XX:ErrorHandlerTest=15",
        "-XX:TestCrashInErrorHandler=14",
        "-version");

    OutputAnalyzer output_detail = new OutputAnalyzer(pb.start());

    // we should have crashed with a SIGFPE
    output_detail.shouldMatch("# A fatal error has been detected by the Java Runtime Environment:.*");
    output_detail.shouldMatch("#.+SIGFPE.*");

    // extract hs-err file
    String hs_err_file = output_detail.firstMatch("# *(\\S*hs_err_pid\\d+\\.log)", 1);
    if (hs_err_file == null) {
      throw new RuntimeException("Did not find hs-err file in output.\n");
    }

    // scan hs-err file: File should contain the "[error occurred during error reporting..]"
    // markers which show that the secondary error handling kicked in and handled the
    // error successfully. As an added test, we check that the last line contains "END.",
    // which is an end marker written in the last step and proves that hs-err file was
    // completely written.
    File f = new File(hs_err_file);
    if (!f.exists()) {
      throw new RuntimeException("hs-err file missing at "
          + f.getAbsolutePath() + ".\n");
    }

    System.out.println("Found hs_err file. Scanning...");

    FileInputStream fis = new FileInputStream(f);
    BufferedReader br = new BufferedReader(new InputStreamReader(fis));
    String line = null;

    Pattern [] pattern = new Pattern[] {
        Pattern.compile("Will crash now \\(TestCrashInErrorHandler=14\\)..."),
        Pattern.compile("\\[error occurred during error reporting \\(test secondary crash 1\\).*\\]"),
        Pattern.compile("Will crash now \\(TestCrashInErrorHandler=14\\)..."),
        Pattern.compile("\\[error occurred during error reporting \\(test secondary crash 2\\).*\\]"),
    };
    int currentPattern = 0;

    String lastLine = null;
    while ((line = br.readLine()) != null) {
      if (currentPattern < pattern.length) {
        if (pattern[currentPattern].matcher(line).matches()) {
          System.out.println("Found: " + line + ".");
          currentPattern ++;
        }
      }
      lastLine = line;
    }
    br.close();

    if (currentPattern < pattern.length) {
      throw new RuntimeException("hs-err file incomplete (first missing pattern: " +  currentPattern + ")");
    }

    if (!lastLine.equals("END.")) {
      throw new RuntimeException("hs-err file incomplete (missing END marker.)");
    } else {
      System.out.println("End marker found.");
    }

    System.out.println("OK.");

  }

}


