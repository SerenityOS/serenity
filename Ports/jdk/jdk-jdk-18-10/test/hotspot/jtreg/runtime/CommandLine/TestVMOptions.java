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
 * @bug 8060256
 * @summary Test various command line options
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run driver TestVMOptions
 */

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import java.io.File;

public class TestVMOptions {
  public static void main(String[] args) throws Exception {
    ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
        "-XX:bogus",
        "-XX:+IgnoreUnrecognizedVMOptions",
        "-XX:+PrintFlagsInitial");
    OutputAnalyzer output = new OutputAnalyzer(pb.start());
    output.shouldContain("bool UseSerialGC");

    pb = ProcessTools.createJavaProcessBuilder(
        "-XX:-PrintVMOptions", "-version");
    output = new OutputAnalyzer(pb.start());
    output.shouldMatch("(openjdk|java)\\sversion");

    File dir = new File(System.getProperty("test.src", "."));
    File file = new File(dir, "flagfile.txt");
    String s = file.getAbsolutePath();
    pb = ProcessTools.createJavaProcessBuilder("-XX:Flags="+s);
    output = new OutputAnalyzer(pb.start());
    output.shouldContain("VM option '-IgnoreUnrecognizedVMOptions'");
  }
}
