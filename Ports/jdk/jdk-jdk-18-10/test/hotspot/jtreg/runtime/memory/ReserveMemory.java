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

// Aix commits on touch, so this test won't work.
/*
 * @test
 * @bug 8012015
 * @requires !(os.family == "aix")
 * @requires vm.flagless
 * @summary Make sure reserved (but uncommitted) memory is not accessible
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run driver ReserveMemory
 */

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.Platform;

import sun.hotspot.WhiteBox;

public class ReserveMemory {
  public static void main(String args[]) throws Exception {
    if (args.length > 0) {
      // expected to crash
      WhiteBox.getWhiteBox().readReservedMemory();
    } else {
      ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
            "-Xbootclasspath/a:.",
            "-XX:+UnlockDiagnosticVMOptions",
            "-XX:+WhiteBoxAPI",
            "-XX:-CreateCoredumpOnCrash",
            "-Xmx128m",
            "ReserveMemory",
            "test");

      OutputAnalyzer output = new OutputAnalyzer(pb.start());
      output.shouldNotHaveExitValue(0);
      if (Platform.isWindows()) {
        output.shouldContain("EXCEPTION_ACCESS_VIOLATION");
      } else if (Platform.isOSX()) {
        output.shouldContain("SIGBUS");
      } else {
        output.shouldContain("SIGSEGV");
      }
    }
  }
}
