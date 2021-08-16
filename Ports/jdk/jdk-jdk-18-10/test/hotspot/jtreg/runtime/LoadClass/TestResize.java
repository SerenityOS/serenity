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
 * @bug 8184765
 * @summary make sure the SystemDictionary gets resized when load factor is too high
 * @requires vm.debug
 * @requires vm.flagless
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @compile TriggerResize.java
 * @run driver TestResize
 */

import jdk.test.lib.Platform;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.Process;
import java.lang.ProcessBuilder;
import java.util.Scanner;

public class TestResize {

  static double MAX_LOAD_FACTOR = 5.0; // see _resize_load_trigger in dictionary.cpp

  static int getInt(String string) {
    int start = 0;
    for (int i = 0; i < string.length(); i++) {
      if (!Character.isDigit(string.charAt(i))) {
        start++;
      } else {
        break;
      }
    }
    int end = start;
    for (int i = end; i < string.length(); i++) {
      if (Character.isDigit(string.charAt(i))) {
        end++;
      } else {
        break;
      }
    }
    return Integer.parseInt(string.substring(start, end));
  }

  static void analyzeOutputOn(ProcessBuilder pb) throws Exception {
    OutputAnalyzer analyzer = new OutputAnalyzer(pb.start());
    String output = analyzer.getStdout();
    analyzer.shouldHaveExitValue(0);

    boolean resized = false;

    // Split string into lines using platform independent end of line marker.
    String[] lines = output.split("\\R");
    for (String line : lines) {
      if (!resized) {
        // ex. [0.563s][info][safepoint,cleanup] resizing system dictionaries, 0.0000002 secs
        if (line.contains("resizing system dictionaries")) {
          resized = true;
        }
      } else if (resized && line.startsWith("Java dictionary (")) {
        // ex. Java dictionary (table_size=10103, classes=5002)
        Scanner scanner = new Scanner(line);
        scanner.next(); // skip "Java"
        scanner.next(); // skip "dictionary"
        int table_size = getInt(scanner.next()); // process "(table_size=40423"
        int classes = getInt(scanner.next()); // process ", classes=50002"
        scanner.close();

        double loadFactor = (double)classes / (double)table_size;
        if (loadFactor > MAX_LOAD_FACTOR) {

          // We've hit an error, so print all of the output.
          System.out.println(output);

          throw new RuntimeException("Load factor too high, expected MAX " + MAX_LOAD_FACTOR +
            ", got " + loadFactor + " [table size " + table_size + ", number of clases " + classes + "]");
        } else {
          System.out.println("PASS table_size: " + table_size + ", classes: " + classes +
          ", load factor: " + loadFactor + " <= " + MAX_LOAD_FACTOR);
          // There are more than one system dictionary to check, so keep looking...
        }
      }
    }

    if (!resized) {
      System.out.println("PASS trivially. No resizing occurred, so did not check the load.");
    }
  }

  public static void main(String[] args) throws Exception {
    // -XX:+PrintSystemDictionaryAtExit will print the details of system dictionary,
    // that will allow us to calculate the table's load factor.
    // -Xlog:safepoint+cleanup will print out cleanup details at safepoint
    // that will allow us to detect if the system dictionary resized.
    ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("-XX:+PrintSystemDictionaryAtExit",
                                                              "-Xlog:safepoint+cleanup",
                                                              "TriggerResize",
                                                              "50000");
    analyzeOutputOn(pb);
  }
}
