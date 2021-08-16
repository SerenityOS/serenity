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
 * @summary Test archiving of old class and interface with custom loader.
 * @requires vm.cds
 * @requires vm.cds.custom.loaders
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds
 * @compile test-classes/OldClassApp.java ../test-classes/OldSuper.jasm
 * @compile ../test-classes/ChildOldSuper.java ../test-classes/GChild.java
 * @compile ../test-classes/OldInf.jasm ../test-classes/ChildOldInf.java
 * @compile ../test-classes/GChild2.java
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar oldclassapp.jar OldClassApp
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar loadees.jar OldSuper ChildOldSuper GChild
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar loadees2.jar OldInf ChildOldInf GChild2
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar WhiteBox.jar sun.hotspot.WhiteBox
 * @run driver OldClassAndInf
 */

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.helpers.ClassFileInstaller;
import sun.hotspot.WhiteBox;

public class OldClassAndInf {
    static String wbJar = ClassFileInstaller.getJarPath("WhiteBox.jar");
    static String use_whitebox_jar = "-Xbootclasspath/a:" + wbJar;
    static String appJar = ClassFileInstaller.getJarPath("oldclassapp.jar");
    static String loadeesJar = ClassFileInstaller.getJarPath("loadees.jar");
    static String loadeesJar2 = ClassFileInstaller.getJarPath("loadees2.jar");

    public static void main(String[] args) throws Exception {

        String classlist[] = new String[] {
            "OldClassApp",
            "java/lang/Object id: 1",
            "OldSuper id: 2 super: 1 source: " + loadeesJar,
            "ChildOldSuper id: 3 super: 2 source: " + loadeesJar,
            "GChild id: 4 super: 3 source: " + loadeesJar
        };
        doTest(classlist, loadeesJar, "true",  "OldSuper", "ChildOldSuper", "GChild");

        String classlist2[] = new String[] {
            "OldClassApp",
            "java/lang/Object id: 1",
            "OldInf id: 2 super: 1 source: " + loadeesJar2,
            "ChildOldInf id: 3 super: 1 interfaces: 2 source: " + loadeesJar2,
            "GChild2 id: 4 super: 3 source: " + loadeesJar2
        };
        doTest(classlist2, loadeesJar2, "true", "OldInf", "ChildOldInf", "GChild2");
    }

    public static void doTest(String[] classlist, String loadeesJar, String inArchive, String ...loadees) throws Exception {

        OutputAnalyzer output;
        TestCommon.testDump(appJar, classlist,
                            "-Xlog:cds=debug,class+load",
                            use_whitebox_jar);

        output = TestCommon.exec(appJar,
                                 TestCommon.concat(
                                     TestCommon.list(
                                         use_whitebox_jar,
                                         "-Xlog:class+load,cds=debug",
                                         "-XX:+UnlockDiagnosticVMOptions",
                                         "-XX:+WhiteBoxAPI",
                                         "OldClassApp", loadeesJar, inArchive),
                                     loadees));

        TestCommon.checkExec(output);
        for (String loadee : loadees) {
            output.shouldContain("[class,load] " + loadee + " source: shared objects file");
        }
    }
}
