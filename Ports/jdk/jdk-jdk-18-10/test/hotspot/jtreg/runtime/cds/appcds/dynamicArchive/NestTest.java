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
 * @summary Archive a lambda proxy class created from a nested class. This
 *          tests the scenario when the caller class is different from the
 *          nest host.
 * @requires vm.cds
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds
 *          /test/hotspot/jtreg/runtime/cds/appcds/dynamicArchive/test-classes
 * @build NestApp sun.hotspot.WhiteBox LambdaVerification
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar nest_app.jar NestApp NestApp$InnerA NestApp$InnerB NestApp$InnerA$InnerInnerA
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar WhiteBox.jar sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:. NestTest
 */

import jdk.test.lib.helpers.ClassFileInstaller;

public class NestTest extends DynamicArchiveTestBase {
    public static void main(String[] args) throws Exception {
        runTest(NestTest::test);
    }

    static void test() throws Exception {
        String topArchiveName = getNewArchiveName();
        String appJar = ClassFileInstaller.getJarPath("nest_app.jar");
        String mainClass = "NestApp";
        String wbJar = ClassFileInstaller.getJarPath("WhiteBox.jar");
        String use_whitebox_jar = "-Xbootclasspath/a:" + wbJar;

        dump(topArchiveName,
            "-XX:+UnlockDiagnosticVMOptions",
            "-XX:+WhiteBoxAPI",
            "-Xlog:class+load=debug,class+unload=info,cds+dynamic=info",
            use_whitebox_jar,
            "-cp", appJar, mainClass)
            .assertNormalExit(output -> {
                output.shouldContain("Archiving hidden NestApp$InnerA$InnerInnerA$$Lambda$")
                      .shouldHaveExitValue(0);
            });

        run(topArchiveName,
            "-XX:+UnlockDiagnosticVMOptions",
            "-XX:+WhiteBoxAPI",
            use_whitebox_jar,
            "-Xlog:class+load=debug,class+resolve=debug,class+unload=info",
            "-cp", appJar, mainClass, "run")
            .assertNormalExit(output -> {
                output.shouldMatch(".class.load.* NestApp[$]InnerA[$]InnerInnerA[$][$]Lambda[$].*/0x.*source:.*shared.*objects.*file.*(top)")
                      .shouldHaveExitValue(0);
            });
    }
}
