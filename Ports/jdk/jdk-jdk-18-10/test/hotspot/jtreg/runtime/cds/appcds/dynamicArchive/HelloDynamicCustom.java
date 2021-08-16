/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Hello World test for dynamic archive with custom loader
 * @requires vm.cds
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds /test/hotspot/jtreg/runtime/cds/appcds/customLoader/test-classes
 * @build HelloUnload CustomLoadee jdk.test.lib.classloader.ClassUnloadCommon
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar hello.jar HelloUnload
 *                 jdk.test.lib.classloader.ClassUnloadCommon
 *                 jdk.test.lib.classloader.ClassUnloadCommon$1
 *                 jdk.test.lib.classloader.ClassUnloadCommon$TestFailure
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar hello_custom.jar CustomLoadee
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar WhiteBox.jar sun.hotspot.WhiteBox
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:./WhiteBox.jar HelloDynamicCustom
 */

import java.io.File;
import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.helpers.ClassFileInstaller;

public class HelloDynamicCustom extends DynamicArchiveTestBase {
    private static final String ARCHIVE_NAME = CDSTestUtils.getOutputFileName("top.jsa");

    public static void main(String[] args) throws Exception {
        runTest(HelloDynamicCustom::testDefaultBase);
    }

    private static void testDefaultBase() throws Exception {
        String wbJar = ClassFileInstaller.getJarPath("WhiteBox.jar");
        String use_whitebox_jar = "-Xbootclasspath/a:" + wbJar;
        String appJar = ClassFileInstaller.getJarPath("hello.jar");
        String customJarPath = ClassFileInstaller.getJarPath("hello_custom.jar");
        String mainAppClass = "HelloUnload";

        dump(ARCHIVE_NAME,
            use_whitebox_jar,
            "-XX:+UnlockDiagnosticVMOptions",
            "-XX:+WhiteBoxAPI",
            "-Xlog:cds",
            "-Xlog:cds+dynamic=debug",
            "-cp", appJar,
            mainAppClass, customJarPath, "false", "false")
            .assertNormalExit(output -> {
                output.shouldContain("Written dynamic archive 0x")
                      .shouldNotContain("klasses.*=.*CustomLoadee")
                      .shouldHaveExitValue(0);
                });

        run(ARCHIVE_NAME,
            use_whitebox_jar,
            "-XX:+UnlockDiagnosticVMOptions",
            "-XX:+WhiteBoxAPI",
            "-Xlog:class+load",
            "-Xlog:cds=debug",
            "-Xlog:cds+dynamic=info",
            "-cp", appJar,
            mainAppClass, customJarPath, "false", "true")
            .assertNormalExit(output -> {
                output.shouldContain("HelloUnload source: shared objects file")
                      .shouldContain("CustomLoadee source: shared objects file")
                      .shouldHaveExitValue(0);
                });
    }
}
