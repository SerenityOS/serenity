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
 * @bug 8006298 8204055
 * @summary Using an unrecognized VM option should print the name of the option
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run driver UnrecognizedVMOption
 */

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;

public class UnrecognizedVMOption {
  public static void main(String[] args) throws Exception {
    // Note: -XX by itself is an unrecognized launcher option, the :
    // must be present for it to be passed through as a VM option.
    String[] badOptions = {
      "",  // empty option
      "bogus_option",
    };
    for (String option : badOptions) {
      ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
          "-XX:" + option, "-version");

      OutputAnalyzer output = new OutputAnalyzer(pb.start());
      output.shouldContain("Unrecognized VM option '" + option + "'");
      output.shouldHaveExitValue(1);
    }
  }
}
