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
 *
 */

/*
 * @test
 * @summary Test the -XX:+IgnoreEmptyClassPaths flag
 * @requires vm.cds
 * @library /test/lib
 * @compile test-classes/Hello.java
 * @compile test-classes/HelloMore.java
 * @run driver IgnoreEmptyClassPaths
 */

import java.io.File;
import jdk.test.lib.process.OutputAnalyzer;

public class IgnoreEmptyClassPaths {

  public static void main(String[] args) throws Exception {
    String jar1 = JarBuilder.getOrCreateHelloJar();
    String jar2 = JarBuilder.build("IgnoreEmptyClassPaths_more", "HelloMore");

    String sep = File.pathSeparator;
    String cp_dump = jar1 + sep + jar2 + sep;
    String cp_exec = sep + jar1 + sep + sep + jar2 + sep;

    TestCommon.testDump(cp_dump, TestCommon.list("Hello", "HelloMore"),
                        "-Xlog:class+path=info", "-XX:+IgnoreEmptyClassPaths");

    TestCommon.run(
        "-verbose:class",
        "-cp", cp_exec,
        "-XX:+IgnoreEmptyClassPaths", // should affect classpath even if placed after the "-cp" argument
        "-Xlog:class+path=info",
        "HelloMore")
      .assertNormalExit();
  }
}
