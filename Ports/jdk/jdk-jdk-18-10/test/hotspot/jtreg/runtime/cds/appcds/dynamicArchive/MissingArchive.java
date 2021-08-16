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

import java.io.File;

/*
 * @test
 * @summary error handling when either (or both) of the base/top archives are missing.
 * @requires vm.cds
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds /test/hotspot/jtreg/runtime/cds/appcds/test-classes
 * @build GenericTestApp sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar WhiteBox.jar sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar GenericTestApp.jar GenericTestApp
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:./WhiteBox.jar MissingArchive
 */

import jdk.test.lib.helpers.ClassFileInstaller;

public class MissingArchive extends DynamicArchiveTestBase {
    private static final String TOP  = "top";
    private static final String BASE = "base";
    private static final String BOTH = "base/top";
    private static final String NONE = "none";

    public static void main(String[] args) throws Exception {
        runTest(MissingArchive::test, TOP);
        runTest(MissingArchive::test, BASE);
        runTest(MissingArchive::test, BOTH);
        runTest(MissingArchive::test, NONE);
    }

    static void delete(String fileName) {
        File f = new File(fileName);
        f.delete();
    }

    static void test(String args[]) throws Exception {
        String topArchiveName = getNewArchiveName("top");
        String baseArchiveName = getNewArchiveName("base");
        TestCommon.dumpBaseArchive(baseArchiveName);

        String appJar = ClassFileInstaller.getJarPath("GenericTestApp.jar");
        String mainClass = "GenericTestApp";
        dump2_WB(baseArchiveName, topArchiveName,
                 "-Xlog:cds",
                 "-Xlog:cds+dynamic=debug",
                 "-cp", appJar, mainClass)
            .assertNormalExit(output -> {
                    output.shouldContain("Written dynamic archive 0x");
                });

        // Use -Xshare:auto so top archive can fail after base archive has succeeded,
        // but the app will continue to run.
        String[] cmdline = TestCommon.concat(
            "-Xlog:cds*",
            "-Xshare:auto",
            "-cp", appJar, mainClass);


        String mode = args[0];

        if (mode.contains(BASE)) {
            delete(baseArchiveName);
            cmdline = TestCommon.concat(cmdline, "assertNotShared:java.lang.Object");
        } else {
            cmdline = TestCommon.concat(cmdline, "assertShared:java.lang.Object");
        }

        if (mode.contains(TOP)) {
            delete(topArchiveName);
        }

        if (mode.equals(NONE)) {
            cmdline = TestCommon.concat(cmdline, "assertShared:GenericTestApp");
        } else {
            cmdline = TestCommon.concat(cmdline, "assertNotShared:GenericTestApp");
        }

        run2_WB(baseArchiveName, topArchiveName, cmdline).assertNormalExit();
    }
}
