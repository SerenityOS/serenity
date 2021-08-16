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
 * @summary Some negative tests for the SharedArchiveFile option
 * @requires vm.cds
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds /test/hotspot/jtreg/runtime/cds/appcds/test-classes
 * @build Hello
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar hello.jar Hello
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:. SharedArchiveFileOption
 */

import jdk.test.lib.helpers.ClassFileInstaller;

import java.io.File;

public class SharedArchiveFileOption extends DynamicArchiveTestBase {
    public static void main(String[] args) throws Exception {
        runTest(SharedArchiveFileOption::testCustomBase);
    }

    static String baseArchiveName2;
    static void testCustomBase() throws Exception {
        String topArchiveName = getNewArchiveName("top");
        String baseArchiveName = getNewArchiveName("base");
        baseArchiveName2 = getNewArchiveName("base2");
        TestCommon.dumpBaseArchive(baseArchiveName);
        TestCommon.dumpBaseArchive(baseArchiveName2);
        doTest(baseArchiveName, topArchiveName);
    }

    private static void doTest(String baseArchiveName, String topArchiveName) throws Exception {
        String appJar = ClassFileInstaller.getJarPath("hello.jar");
        String mainClass = "Hello";
        String dummyArchiveName = getNewArchiveName("dummy");

        // -Xshare:dump specified with -XX:ArchiveClassesAtExit
        dump2(dummyArchiveName, dummyArchiveName,
            "-Xlog:cds",
            "-Xlog:cds+dynamic=debug",
            "-Xshare:dump",
            "-cp", appJar, mainClass)
            .assertAbnormalExit(output -> {
                    output.shouldContain("-XX:ArchiveClassesAtExit cannot be used with -Xshare:dump");
                });

        // more than 1 archive file specified in -XX:SharedArchiveFile during
        // dynamic dumpgin
        String dummyArchives = dummyArchiveName + File.pathSeparator + dummyArchiveName;
        dump2(dummyArchives, dummyArchiveName,
            "-Xlog:cds",
            "-Xlog:cds+dynamic=debug",
            "-cp", appJar, mainClass)
            .assertAbnormalExit(output -> {
                    output.shouldContain("Cannot have more than 1 archive file specified in -XX:SharedArchiveFile during CDS dumping");
                });

        // normal dynamic archive dumping
        dump2(baseArchiveName, topArchiveName,
            "-Xlog:cds",
            "-Xlog:cds+dynamic=debug",
            "-cp", appJar, mainClass)
            .assertNormalExit(output -> {
                    output.shouldContain("Written dynamic archive 0x");
                });

        // same archive file specified for -XX:SharedArchiveFile and -XX:ArchiveClassesAtExit
        dump2(baseArchiveName, baseArchiveName,
            "-Xlog:cds",
            "-Xlog:cds+dynamic=debug",
            "-cp", appJar, mainClass)
            .assertAbnormalExit(output -> {
                    output.shouldContain("Cannot have the same archive file specified for -XX:SharedArchiveFile and -XX:ArchiveClassesAtExit: "
                        + baseArchiveName);
                });


        // a top archive specified in the base archive position
        run2(topArchiveName, baseArchiveName,
            "-Xlog:class+load",
            "-Xlog:cds+dynamic=debug,cds=debug",
            "-cp", appJar, mainClass)
            .assertAbnormalExit(output -> {
                    output.shouldMatch("Not a base shared archive:.*top.*.jsa");
                });

        // a base archive specified in the top archive position
        run2(baseArchiveName, baseArchiveName2,
            "-Xlog:class+load",
            "-Xlog:cds+dynamic=debug,cds=debug",
            "-cp", appJar, mainClass)
            .assertAbnormalExit(output -> {
                    output.shouldMatch("Not a top shared archive:.*base.*.jsa");
                });

        // more than 2 archives specified in the -XX:ShareArchiveFile option
        String baseArchives = baseArchiveName + File.pathSeparator + baseArchiveName2;
        run2(baseArchives, topArchiveName,
            "-Xlog:class+load",
            "-Xlog:cds+dynamic=debug,cds=debug",
            "-cp", appJar, mainClass)
            .assertAbnormalExit(output -> {
                    output.shouldContain(
                        "Cannot have more than 2 archive files specified in the -XX:SharedArchiveFile option");
                });

        // base archive not specified
        final String topArchive = File.pathSeparator + topArchiveName;
        run2(topArchive, null,
            "-Xlog:class+load",
            "-Xlog:cds+dynamic=debug,cds=debug",
            "-cp", appJar, mainClass)
            .assertAbnormalExit(output -> {
                    output.shouldContain(
                        "Base archive was not specified: " + topArchive);
                });

        // top archive not specified
        final String baseArchive = baseArchiveName + File.pathSeparator;
        run2(baseArchive, null,
            "-Xlog:class+load",
            "-Xlog:cds+dynamic=debug,cds=debug",
            "-cp", appJar, mainClass)
            .assertAbnormalExit(output -> {
                    output.shouldContain(
                        "Top archive was not specified: " + baseArchive);
                });
    }
}
