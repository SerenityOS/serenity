/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2018, Google and/or its affiliates. All rights reserved.
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

package gc.logging;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import jdk.test.lib.Asserts;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import sun.hotspot.WhiteBox;

/*
 * @test TestMetaSpaceLog
 * @bug 8211123
 * @summary Ensure that the Metaspace is updated in the log
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @requires vm.gc != "Epsilon"
 * @requires vm.gc != "Z"
 * @requires os.maxMemory >= 2G
 *
 * @compile TestMetaSpaceLog.java
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run driver gc.logging.TestMetaSpaceLog
 */

public class TestMetaSpaceLog {
  private static Pattern metaSpaceRegexp;

  static {
    // Do this once here.
    // Scan for Metaspace update notices as part of the GC log, e.g. in this form:
    // [gc,metaspace   ] GC(0) Metaspace: 11895K(14208K)->11895K(14208K) NonClass: 10552K(12544K)->10552K(12544K) Class: 1343K(1664K)->1343K(1664K)
    metaSpaceRegexp = Pattern.compile(".*Metaspace: ([0-9]+).*->([0-9]+).*");
  }

  public static void main(String[] args) throws Exception {
    testMetaSpaceUpdate();
  }

  private static void verifyContainsMetaSpaceUpdate(OutputAnalyzer output) {
    // At least one metaspace line from GC should show GC being collected.
    boolean foundCollectedMetaSpace = output.asLines().stream()
        .filter(s -> s.contains("[gc,metaspace"))
        .anyMatch(TestMetaSpaceLog::check);
    Asserts.assertTrue(foundCollectedMetaSpace);
  }

  private static boolean check(String line) {
    Matcher m = metaSpaceRegexp.matcher(line);
    if (m.matches()) {
      // Numbers for Metaspace occupation should grow.
      long before = Long.parseLong(m.group(1));
      long after = Long.parseLong(m.group(2));
      return before > after;
    }
    return false;
  }

  private static void testMetaSpaceUpdate() throws Exception {
    // Propagate test.src for the jar file.
    String testSrc= "-Dtest.src=" + System.getProperty("test.src", ".");

    ProcessBuilder pb =
      ProcessTools.createTestJvm(
          "-Xlog:gc*",
          "-Xbootclasspath/a:.",
          "-XX:+UnlockDiagnosticVMOptions",
          "-XX:+WhiteBoxAPI",
          "-Xmx1000M",
          "-Xms1000M",
          testSrc, StressMetaSpace.class.getName());

    OutputAnalyzer output = null;
    try {
      output = new OutputAnalyzer(pb.start());
      verifyContainsMetaSpaceUpdate(output);
    } catch (Exception e) {
      // For error diagnosis: print and throw.
      e.printStackTrace();
      output.reportDiagnosticSummary();
      throw e;
    }
  }

  static class StressMetaSpace {
    private static URL[] urls = new URL[1];

    static {
      try {
        File jarFile = new File(System.getProperty("test.src") + "/testcases.jar");
        urls[0] = jarFile.toURI().toURL();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    public static void main(String args[]) {
      WhiteBox wb = WhiteBox.getWhiteBox();
      for(int i = 0; i < 10000; i++) {
        loadClass(wb);
      }
      wb.fullGC();
    }

    public static void loadClass(WhiteBox wb) {
      try {
        URLClassLoader ucl = new URLClassLoader(urls);
        Class.forName("case00", false, ucl);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}
