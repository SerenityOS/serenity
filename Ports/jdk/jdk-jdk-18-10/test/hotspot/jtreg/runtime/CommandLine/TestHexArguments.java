/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8042885
 * @summary Make sure there is no error using hexadecimal format in vm options
 * @author Yumin Qi
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run driver TestHexArguments
 */

import java.io.File;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;

public class TestHexArguments {
    public static void main(String args[]) throws Exception {
      ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
          "-XX:SharedBaseAddress=0x1D000000", "-version");
      OutputAnalyzer output = new OutputAnalyzer(pb.start());
      output.shouldNotContain("Could not create the Java Virtual Machine");
      output.shouldHaveExitValue(0);

      pb = ProcessTools.createJavaProcessBuilder(
          "-XX:SharedBaseAddress=1D000000", "-version");
      output = new OutputAnalyzer(pb.start());
      output.shouldContain("Could not create the Java Virtual Machine");
  }
}
