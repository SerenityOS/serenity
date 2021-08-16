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
 * @bug 8060449 8073989 8202744
 * @summary Newly obsolete command line options should still give useful error messages when used improperly.
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @requires vm.debug == true
 * @run driver ObsoleteFlagErrorMessage
 */

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;

public class ObsoleteFlagErrorMessage {
  public static void main(String[] args) throws Exception {

    String flag = "DummyObsoleteTestFlag";

    // Case 1: Newly obsolete flags with extra junk appended should not be treated as newly obsolete (8060449)
    ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
        "-XX:" + flag + "PlusJunk", "-version");

    OutputAnalyzer output = new OutputAnalyzer(pb.start());
    output.shouldContain("Unrecognized VM option '" + flag + "PlusJunk'"); // Must identify bad option.
    output.shouldHaveExitValue(1);

    // Case 2: Newly obsolete flags should be recognized as newly obsolete (8073989)
    ProcessBuilder pb2 = ProcessTools.createJavaProcessBuilder(
        "-XX:+" + flag, "-version");

    OutputAnalyzer output2 = new OutputAnalyzer(pb2.start());
    output2.shouldContain("Ignoring option").shouldContain("support was removed");
    output2.shouldContain(flag);
  }
}
