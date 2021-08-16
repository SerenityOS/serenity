/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Initiating and defining classloader test.
 * @requires vm.cds
 * @library /test/lib
 * @compile test-classes/Hello.java
 * @compile test-classes/HelloWB.java
 * @compile test-classes/ForNameTest.java
 * @compile test-classes/BootClassPathAppendHelper.java
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run driver ClassLoaderTest
 */

import java.io.File;
import jdk.test.lib.process.OutputAnalyzer;

public class ClassLoaderTest {
    public static void main(String[] args) throws Exception {
        JarBuilder.build(true, "ClassLoaderTest-WhiteBox", "sun/hotspot/WhiteBox");
        JarBuilder.getOrCreateHelloJar();
        JarBuilder.build("ClassLoaderTest-HelloWB", "HelloWB");
        JarBuilder.build("ClassLoaderTest-ForName", "ForNameTest");
        ClassLoaderTest test = new ClassLoaderTest();
        test.testBootLoader();
        test.testDefiningLoader();
    }

    public void testBootLoader() throws Exception {
        String appJar = TestCommon.getTestJar("ClassLoaderTest-HelloWB.jar");
        String appClasses[] = {"HelloWB"};
        String whiteBoxJar = TestCommon.getTestJar("ClassLoaderTest-WhiteBox.jar");
        String bootClassPath = "-Xbootclasspath/a:" + appJar +
            File.pathSeparator + whiteBoxJar;

        TestCommon.dump(appJar, appClasses, bootClassPath);

        TestCommon.run(
            "-XX:+UnlockDiagnosticVMOptions", "-XX:+WhiteBoxAPI",
            "-cp", appJar, bootClassPath, "HelloWB")
          .assertNormalExit(output -> output.shouldContain("HelloWB.class.getClassLoader() = null"));
    }

    public void testDefiningLoader() throws Exception {
        // The boot loader should be used to load the class when it's
        // on the bootclasspath, regardless who is the initiating classloader.
        // In this test case, the AppClassLoader is the initiating classloader.
        String helloJar = TestCommon.getTestJar("hello.jar");
        String appJar = helloJar + System.getProperty("path.separator") +
                        TestCommon.getTestJar("ClassLoaderTest-ForName.jar");
        String whiteBoxJar = TestCommon.getTestJar("ClassLoaderTest-WhiteBox.jar");
        String bootClassPath = "-Xbootclasspath/a:" + helloJar +
            File.pathSeparator + whiteBoxJar;

        // Archive the "Hello" class from the appended bootclasspath
        TestCommon.dump(helloJar, TestCommon.list("Hello"), bootClassPath);

        TestCommon.run("-XX:+UnlockDiagnosticVMOptions", "-XX:+WhiteBoxAPI",
            "-cp", appJar, bootClassPath, "-Xlog:class+path=trace", "ForNameTest")
          .assertNormalExit();
    }
}
