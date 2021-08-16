/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Sanity check the output of NMT
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:NativeMemoryTracking=summary -XX:+WhiteBoxAPI SummarySanityCheck
 */

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.JDKToolFinder;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import sun.hotspot.WhiteBox;

public class SummarySanityCheck {

  private static String jcmdout;
  public static void main(String args[]) throws Exception {
    // Grab my own PID
    String pid = Long.toString(ProcessTools.getProcessId());

    ProcessBuilder pb = new ProcessBuilder();

    // Run  'jcmd <pid> VM.native_memory summary scale=KB'
    pb.command(new String[] { JDKToolFinder.getJDKTool("jcmd"), pid, "VM.native_memory", "summary", "scale=KB"});
    OutputAnalyzer output = new OutputAnalyzer(pb.start());

    jcmdout = output.getOutput();
    // Split by '-' to get the 'groups'
    String[] lines = jcmdout.split("\n");

    if (lines.length == 0) {
      throwTestException("Failed to parse jcmd output");
    }

    long totalCommitted = 0, totalReserved = 0;
    long totalCommittedSum = 0, totalReservedSum = 0;

    // Match '- <mtType> (reserved=<reserved>KB, committed=<committed>KB)
    Pattern mtTypePattern = Pattern.compile("-\\s+(?<typename>[\\w\\s]+)\\(reserved=(?<reserved>\\d+)KB,\\scommitted=(?<committed>\\d+)KB\\)");
    // Match 'Total: reserved=<reserved>KB, committed=<committed>KB'
    Pattern totalMemoryPattern = Pattern.compile("Total\\:\\sreserved=(?<reserved>\\d+)KB,\\scommitted=(?<committed>\\d+)KB");

    for (int i = 0; i < lines.length; i++) {
      if (lines[i].startsWith("Total")) {
        Matcher totalMemoryMatcher = totalMemoryPattern.matcher(lines[i]);

        if (totalMemoryMatcher.matches()) {
          totalCommitted = Long.parseLong(totalMemoryMatcher.group("committed"));
          totalReserved = Long.parseLong(totalMemoryMatcher.group("reserved"));
        } else {
          throwTestException("Failed to match the expected groups in 'Total' memory part");
        }
      } else if (lines[i].startsWith("-")) {
        Matcher typeMatcher = mtTypePattern.matcher(lines[i]);
        if (typeMatcher.matches()) {
          long typeCommitted = Long.parseLong(typeMatcher.group("committed"));
          long typeReserved = Long.parseLong(typeMatcher.group("reserved"));

          // Make sure reserved is always less or equals
          if (typeCommitted > typeReserved) {
            throwTestException("Committed (" + typeCommitted + ") was more than Reserved ("
                + typeReserved + ") for mtType: " + typeMatcher.group("typename"));
          }

          // Add to total and compare them in the end
          totalCommittedSum += typeCommitted;
          totalReservedSum += typeReserved;
        } else {
          throwTestException("Failed to match the group on line " + i);
        }
      }
    }

    // See if they add up correctly, rounding is a problem so make sure we're within +/- 8KB
    long committedDiff = totalCommitted - totalCommittedSum;
    if (committedDiff > 8 || committedDiff < -8) {
      throwTestException("Total committed (" + totalCommitted + ") did not match the summarized committed (" + totalCommittedSum + ")" );
    }

    long reservedDiff = totalReserved - totalReservedSum;
    if (reservedDiff > 8 || reservedDiff < -8) {
      throwTestException("Total reserved (" + totalReserved + ") did not match the summarized reserved (" + totalReservedSum + ")" );
    }
  }

  private static void throwTestException(String reason) throws Exception {
      throw new Exception(reason + " . Stdout is :\n" + jcmdout);
  }
}
