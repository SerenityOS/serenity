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
 * @summary
 * @requires vm.cds
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds
 *          /test/hotspot/jtreg/runtime/cds/appcds/dynamicArchive/test-classes
 * @build PredicateApp sun.hotspot.WhiteBox LambdaVerification
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar predicate_app.jar PredicateApp LambdaVerification
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar WhiteBox.jar sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:. PredicateTest
 */

import jdk.test.lib.helpers.ClassFileInstaller;

public class PredicateTest extends DynamicArchiveTestBase {
    public static void main(String[] args) throws Exception {
        runTest(PredicateTest::test);
    }

    static void test() throws Exception {
        String topArchiveName = getNewArchiveName();
        String appJar = ClassFileInstaller.getJarPath("predicate_app.jar");
        String mainClass = "PredicateApp";
        String wbJar = ClassFileInstaller.getJarPath("WhiteBox.jar");
        String use_whitebox_jar = "-Xbootclasspath/a:" + wbJar;

        dump(topArchiveName, "-XX:+UnlockDiagnosticVMOptions", "-XX:+ShowHiddenFrames",
            "-XX:+WhiteBoxAPI",
            "-Xlog:class+load=debug,cds+dynamic=info",
            use_whitebox_jar,
            "-cp", appJar, mainClass)
            .assertNormalExit(output -> {
                output.shouldContain("Archiving hidden PredicateApp$$Lambda$")
                      .shouldHaveExitValue(0);
            });

        run(topArchiveName, "-XX:+UnlockDiagnosticVMOptions", "-XX:+ShowHiddenFrames",
            "-XX:+WhiteBoxAPI",
            use_whitebox_jar,
            "-Xlog:class+load=debug,class+resolve=debug",
            "-cp", appJar, mainClass, "run")
            .assertNormalExit(output -> {
                output.shouldHaveExitValue(0);
            });
    }
}
