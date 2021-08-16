/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2018, SAP. All rights reserved.
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
 * @bug 8214975
 * @summary No hs-err file if fatal error is raised during dynamic initialization.
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 * @requires (vm.debug == true)
 * @requires os.family == "linux"
 * @run driver VeryEarlyAssertTest
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.regex.Pattern;
import java.util.Map;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class VeryEarlyAssertTest {

  public static void main(String[] args) throws Exception {


    ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
            "-version");
    Map<String, String> env = pb.environment();
    env.put("HOTSPOT_FATAL_ERROR_DURING_DYNAMIC_INITIALIZATION", "1");

    OutputAnalyzer output_detail = new OutputAnalyzer(pb.start());

    // we should have crashed with an assert with a specific message:
    output_detail.shouldMatch("# A fatal error has been detected by the Java Runtime Environment:.*");
    output_detail.shouldMatch("#.*HOTSPOT_FATAL_ERROR_DURING_DYNAMIC_INITIALIZATION.*");

    // extract hs-err file
    String hs_err_file = output_detail.firstMatch("# *(\\S*hs_err_pid\\d+\\.log)", 1);
    if (hs_err_file == null) {
      throw new RuntimeException("Did not find hs-err file in output.\n");
    }

    // scan hs-err file: File should contain the same assertion message. Other than that,
    // do not expect too much: file will be littered with secondary errors. The test
    // should test that we get a hs-err file at all.
    File f = new File(hs_err_file);
    if (!f.exists()) {
      throw new RuntimeException("hs-err file missing at "
              + f.getAbsolutePath() + ".\n");
    }

    System.out.println("Found hs_err file. Scanning...");

    FileInputStream fis = new FileInputStream(f);
    BufferedReader br = new BufferedReader(new InputStreamReader(fis));
    String line = null;

    Pattern[] pattern = new Pattern[]{
            Pattern.compile(".*HOTSPOT_FATAL_ERROR_DURING_DYNAMIC_INITIALIZATION.*")
    };
    int currentPattern = 0;

    boolean endMarkerFound = false;
    while ((line = br.readLine()) != null) {
      if (currentPattern < pattern.length) {
        if (pattern[currentPattern].matcher(line).matches()) {
          System.out.println("Found: " + line + ".");
          currentPattern++;
        }
      }
      if (line.equals("END.")) {
        endMarkerFound = true;
        break;
      }
    }
    br.close();

    if (currentPattern < pattern.length) {
      throw new RuntimeException("hs-err file incomplete (first missing pattern: " + currentPattern + ")");
    }

    if (!endMarkerFound) {
      throw new RuntimeException("hs-err file incomplete (missing END marker.)");
    } else {
      System.out.println("End marker found.");
    }

    System.out.println("OK.");

  }

}
