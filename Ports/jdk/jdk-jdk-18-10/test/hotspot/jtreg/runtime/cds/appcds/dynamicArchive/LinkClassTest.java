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
 * @summary Classes loaded by the builtin class loaders should be linked
 *          during dynamic CDS dump time.
 * @requires vm.cds
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds
 *          /test/hotspot/jtreg/runtime/cds/appcds/dynamicArchive/test-classes
 * @build LinkClassApp
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar link_class_app.jar LinkClassApp Parent Child Parent2 Child2 MyShutdown
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:. LinkClassTest
 */

import jdk.test.lib.helpers.ClassFileInstaller;

public class LinkClassTest extends DynamicArchiveTestBase {
    public static void main(String[] args) throws Exception {
        runTest(LinkClassTest::test);
    }

    static void test() throws Exception {
        String topArchiveName = getNewArchiveName();
        String appJar = ClassFileInstaller.getJarPath("link_class_app.jar");
        String mainClass = "LinkClassApp";

        // dump archive with the app without calling System.exit().
        dump(topArchiveName,
            "-Xlog:class+load,cds+dynamic=info,cds",
            "-cp", appJar, mainClass)
            .assertNormalExit(output -> {
                output.shouldNotContain("Skipping Parent: Not linked")
                      .shouldNotContain("Skipping Parent2: Not linked")
                      .shouldNotContain("Skipping Child: Not linked")
                      .shouldNotContain("Skipping Child2: Not linked")
                      .shouldHaveExitValue(0);
            });

        run(topArchiveName,
            "-Xlog:class+load",
            "-cp", appJar, mainClass, "run")
            .assertNormalExit(output -> {
                output.shouldContain("Parent source: shared objects file (top)")
                      .shouldContain("Parent2 source: shared objects file (top)")
                      .shouldContain("Child source: shared objects file (top)")
                      .shouldContain("Child2 source: shared objects file (top)")
                      .shouldHaveExitValue(0);
            });

        // dump archive with the app calling System.exit().
        dump(topArchiveName,
            "-Xlog:class+load,cds+dynamic=info,cds",
            "-cp", appJar, mainClass, "callExit")
            .assertNormalExit(output -> {
                output.shouldNotContain("Skipping Parent: Not linked")
                      .shouldNotContain("Skipping Parent2: Not linked")
                      .shouldNotContain("Skipping Child: Not linked")
                      .shouldNotContain("Skipping Child2: Not linked")
                      .shouldHaveExitValue(0);
            });

        run(topArchiveName,
            "-Xlog:class+load",
            "-cp", appJar, mainClass, "run")
            .assertNormalExit(output -> {
                output.shouldContain("Parent source: shared objects file (top)")
                      .shouldContain("Parent2 source: shared objects file (top)")
                      .shouldContain("Child source: shared objects file (top)")
                      .shouldContain("Child2 source: shared objects file (top)")
                      .shouldHaveExitValue(0);
            });
    }
}
