/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test archived mirror with reference fields
 * @requires vm.cds.archived.java.heap
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds
 * @build sun.hotspot.WhiteBox
 * @compile MirrorWithReferenceFieldsApp.java
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar app.jar MirrorWithReferenceFieldsApp
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar WhiteBox.jar sun.hotspot.WhiteBox
 * @run driver MirrorWithReferenceFieldsTest
 */

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.helpers.ClassFileInstaller;
import sun.hotspot.WhiteBox;

public class MirrorWithReferenceFieldsTest {
    public static void main(String[] args) throws Exception {
        String wbJar = ClassFileInstaller.getJarPath("WhiteBox.jar");
        String use_whitebox_jar = "-Xbootclasspath/a:" + wbJar;
        String appJar = ClassFileInstaller.getJarPath("app.jar");

        String classlist[] = new String[] {
            "MirrorWithReferenceFieldsApp",
        };

        TestCommon.testDump(appJar, classlist, use_whitebox_jar);
        OutputAnalyzer output = TestCommon.exec(appJar, use_whitebox_jar,
                                                "-XX:+UnlockDiagnosticVMOptions",
                                                "-XX:+WhiteBoxAPI",
                                                "-XX:+VerifyAfterGC",
                                                "MirrorWithReferenceFieldsApp");
        try {
            TestCommon.checkExec(output, "Done");
        } catch (Exception e) {
            output.shouldContain("Archived open_archive_heap objects are not mapped");
        }
    }
}
