/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test -Xshare:auto for AppCDS
 * @requires vm.cds
 * @library /test/lib
 * @compile test-classes/Hello.java
 * @run driver XShareAutoWithChangedJar
 */

import jdk.test.lib.process.OutputAnalyzer;

public class XShareAutoWithChangedJar {
  public static void main(String[] args) throws Exception {
    String appJar = JarBuilder.build("XShareAutoWithChangedJar", "Hello");

    // 1. dump
    OutputAnalyzer output = TestCommon.dump(appJar, TestCommon.list("Hello"));
    TestCommon.checkDump(output);

    // 2. change the jar
    JarBuilder.build("XShareAutoWithChangedJar", "Hello");

    // 3. exec
    output = TestCommon.execAuto("-cp", appJar, "Hello");
    output.shouldContain("Hello World");
  }
}
