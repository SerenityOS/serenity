/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8261090
 * @summary Test archiving of old class and interface with custom loader with dynamic CDS.
 * @requires vm.cds
 * @requires vm.cds.custom.loaders
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds
 * @compile ../customLoader/test-classes/OldClassApp.java ../test-classes/OldSuper.jasm
 * @compile ../test-classes/ChildOldSuper.java ../test-classes/GChild.java
 * @compile ../test-classes/OldInf.jasm ../test-classes/ChildOldInf.java
 * @compile ../test-classes/GChild2.java
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar oldclassapp.jar OldClassApp
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar loadees.jar OldSuper ChildOldSuper GChild
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar loadees2.jar OldInf ChildOldInf GChild2
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar WhiteBox.jar sun.hotspot.WhiteBox
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:./WhiteBox.jar OldClassAndInf
 */

import java.io.File;
import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.helpers.ClassFileInstaller;

public class OldClassAndInf extends DynamicArchiveTestBase {
    private static final String ARCHIVE_NAME = CDSTestUtils.getOutputFileName("oldclass-top.jsa");
    private static String wbJar = ClassFileInstaller.getJarPath("WhiteBox.jar");
    private static String use_whitebox_jar = "-Xbootclasspath/a:" + wbJar;
    private static String appJar = ClassFileInstaller.getJarPath("oldclassapp.jar");
    private static String mainAppClass = "OldClassApp";

    public static void main(String[] args) throws Exception {
        runTest(OldClassAndInf::testDefaultBase);
    }

    private static void testDefaultBase() throws Exception {
        System.out.println("Run test with old super class...");
        String loadeesJar = ClassFileInstaller.getJarPath("loadees.jar");
        doTest(loadeesJar, "false", "OldSuper", "ChildOldSuper", "GChild");

        System.out.println("Run test with old super interface...");
        String loadeesJar2 = ClassFileInstaller.getJarPath("loadees2.jar");
        doTest(loadeesJar2, "false", "OldInf", "ChildOldInf", "GChild2");
    }

    private static void doTest(String loadeesJar, String inArchive, String ...loadees) throws Exception {

        String[] loadeesArray = TestCommon.list(loadees);

        dump(ARCHIVE_NAME,
             TestCommon.concat(
                 TestCommon.list(
                     use_whitebox_jar,
                     "-XX:+UnlockDiagnosticVMOptions",
                     "-XX:+WhiteBoxAPI",
                     "-Xlog:cds",
                     "-Xlog:cds+dynamic=debug",
                     "-cp", appJar,
                     mainAppClass, loadeesJar, inArchive),
             loadees))
             .assertNormalExit(output -> {
                 output.shouldContain("Written dynamic archive 0x")
                       .shouldHaveExitValue(0);
                 });

        run(ARCHIVE_NAME,
            TestCommon.concat(
                TestCommon.list(
                    use_whitebox_jar,
                    "-XX:+UnlockDiagnosticVMOptions",
                    "-XX:+WhiteBoxAPI",
                    "-Xlog:class+load",
                    "-Xlog:cds=debug",
                    "-Xlog:cds+dynamic=info",
                    "-cp", appJar,
                    mainAppClass, loadeesJar, inArchive),
            loadees))
            .assertNormalExit(output -> {
                output.shouldHaveExitValue(0);
                for (String loadee : loadees) {
                    output.shouldContain(loadee + " source: shared objects file (top)");
                }
                });
    }
}
