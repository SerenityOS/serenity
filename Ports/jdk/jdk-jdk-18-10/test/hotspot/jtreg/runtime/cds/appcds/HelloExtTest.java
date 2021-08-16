/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary a simple test for loading a class using the platform class loader
 *          (which used to be called the "extension loader) in AppCDS
 * @requires vm.cds
 * @library /test/lib
 * @compile test-classes/HelloExt.java
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run driver HelloExtTest
 */

import jdk.test.lib.process.OutputAnalyzer;

public class HelloExtTest {

  public static void main(String[] args) throws Exception {
    JarBuilder.build("helloExt", "HelloExt");

    String appJar = TestCommon.getTestJar("helloExt.jar");
    JarBuilder.build(true, "WhiteBox", "sun/hotspot/WhiteBox");
    String whiteBoxJar = TestCommon.getTestJar("WhiteBox.jar");
    String bootClassPath = "-Xbootclasspath/a:" + whiteBoxJar;

    TestCommon.dump(appJar,
        TestCommon.list("javax/annotation/processing/FilerException", "[Ljava/lang/Comparable;"),
        bootClassPath);

    String prefix = ".class.load. ";
    String class_pattern = ".*LambdaForm[$]MH[/][0123456789].*";
    String suffix = ".*source: shared objects file.*";
    String pattern = prefix + class_pattern + suffix;

    TestCommon.run("-XX:+UnlockDiagnosticVMOptions", "-XX:+WhiteBoxAPI",
            "-cp", appJar, bootClassPath, "-Xlog:class+load", "HelloExt")
        .assertNormalExit(output -> output.shouldNotMatch(pattern));


    TestCommon.run("-XX:+UnlockDiagnosticVMOptions", "-XX:+WhiteBoxAPI",
            "-cp", appJar, bootClassPath, "-Xlog:class+load",
            "-XX:+PrintSharedArchiveAndExit", "-XX:+PrintSharedDictionary",
            "HelloExt")
        .assertNormalExit(output ->  output.shouldNotMatch(class_pattern));
  }
}
