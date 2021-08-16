/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Module system initialization exception results if a module is specificed twice to --patch-module.
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @run driver PatchModuleDupModule
 */

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class PatchModuleDupModule {

  // The module system initialization should generate an ExceptionInInitializerError
  // if --patch-module is specified with the same module more than once.

  public static void main(String args[]) throws Exception {
    ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
      "--patch-module=module_one=module_one_dir",
      "--patch-module=module_one=module_one_dir",
      "-version");
    OutputAnalyzer output = new OutputAnalyzer(pb.start());
    output.shouldContain("java.lang.ExceptionInInitializerError");
    output.shouldHaveExitValue(1);
  }
}
