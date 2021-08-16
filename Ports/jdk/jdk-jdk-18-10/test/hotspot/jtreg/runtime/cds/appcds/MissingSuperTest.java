/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @summary When super class is missing during dumping, no crash should happen.
 *
 * @requires vm.cds
 * @library /test/lib
 * @compile test-classes/MissingSuper.java
 * @run driver MissingSuperTest
 */

public class MissingSuperTest {

  public static void main(String[] args) throws Exception {
    // The classes "MissingSuperSup" and "MissingSuperIntf" are intentionally not
    // included into the jar to provoke the test condition
    JarBuilder.build("missing_super", "MissingSuper",
        "MissingSuperSub", "MissingSuperImpl");

    String appJar = TestCommon.getTestJar("missing_super.jar");
    TestCommon.test(appJar, TestCommon.list("MissingSuper",
        "MissingSuperSub",
        "MissingSuperImpl"),
        "MissingSuper");
  }
}
