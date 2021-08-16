/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary CDS dump should abort if a class file contains a bad BSM.
 * @requires vm.cds
 * @library /test/lib
 * @compile test-classes/WrongBSM.jcod
 * @run driver BadBSM
 */

import jdk.test.lib.process.OutputAnalyzer;

public class BadBSM {

  public static void main(String[] args) throws Exception {
    JarBuilder.build("wrongbsm", "WrongBSM");

    String appJar = TestCommon.getTestJar("wrongbsm.jar");

    OutputAnalyzer out = TestCommon.dump(appJar,
        TestCommon.list("WrongBSM",
                        "@lambda-proxy WrongBSM 7"),
        "-Xlog:cds+lambda=debug");
    out.shouldHaveExitValue(0)
       .shouldContain("resolve_indy for class WrongBSM has encountered exception: java.lang.NoSuchMethodError");
  }
}
