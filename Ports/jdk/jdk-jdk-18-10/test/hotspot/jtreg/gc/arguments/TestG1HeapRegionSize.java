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

package gc.arguments;

/*
 * @test TestG1HeapRegionSize
 * @bug 8021879
 * @requires vm.gc.G1
 * @summary Verify that the flag G1HeapRegionSize is updated properly
 * @modules java.base/jdk.internal.misc
 * @modules java.management/sun.management
 * @library /test/lib
 * @library /
 * @run driver gc.arguments.TestG1HeapRegionSize
 */

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.ArrayList;
import java.util.Arrays;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class TestG1HeapRegionSize {

  private static void checkG1HeapRegionSize(String[] flags, int expectedValue, int exitValue) throws Exception {
    ArrayList<String> flagList = new ArrayList<String>();
    flagList.addAll(Arrays.asList(flags));
    flagList.add("-XX:+UseG1GC");
    flagList.add("-XX:+PrintFlagsFinal");
    flagList.add("-version");

    ProcessBuilder pb = GCArguments.createJavaProcessBuilder(flagList);
    OutputAnalyzer output = new OutputAnalyzer(pb.start());
    output.shouldHaveExitValue(exitValue);

    if (exitValue == 0) {
      String stdout = output.getStdout();
      int flagValue = getFlagValue("G1HeapRegionSize", stdout);
      if (flagValue != expectedValue) {
        throw new RuntimeException("Wrong value for G1HeapRegionSize. Expected " + expectedValue + " but got " + flagValue);
      }
    }
  }

  private static int getFlagValue(String flag, String where) {
    Matcher m = Pattern.compile(flag + "\\s+:?=\\s+\\d+").matcher(where);
    if (!m.find()) {
      throw new RuntimeException("Could not find value for flag " + flag + " in output string");
    }
    String match = m.group();
    return Integer.parseInt(match.substring(match.lastIndexOf(" ") + 1, match.length()));
  }

  public static void main(String args[]) throws Exception {
    final int M = 1024 * 1024;

    checkG1HeapRegionSize(new String[] { "-Xmx64m"   /* default is 1m */        },  1*M, 0);
    checkG1HeapRegionSize(new String[] { "-Xmx64m",  "-XX:G1HeapRegionSize=2m"  },  2*M, 0);
    checkG1HeapRegionSize(new String[] { "-Xmx64m",  "-XX:G1HeapRegionSize=3m"  },  4*M, 0);
    checkG1HeapRegionSize(new String[] { "-Xmx256m", "-XX:G1HeapRegionSize=32m" }, 32*M, 0);
    checkG1HeapRegionSize(new String[] { "-Xmx256m", "-XX:G1HeapRegionSize=64m" }, 32*M, 1);
  }
}
