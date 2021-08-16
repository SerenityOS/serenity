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
 * @summary Test primitive box caches integrity in various scenarios (IntegerCache etc)
 * @requires vm.cds.archived.java.heap
 * @library /test/jdk/lib/testlibrary /test/lib /test/hotspot/jtreg/runtime/cds/appcds
 * @build sun.hotspot.WhiteBox
 * @compile CheckIntegerCacheApp.java
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar boxCache.jar CheckIntegerCacheApp
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar WhiteBox.jar sun.hotspot.WhiteBox
 * @run driver ArchivedIntegerCacheTest
 */

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.helpers.ClassFileInstaller;

public class ArchivedIntegerCacheTest {

    public static void main(String[] args) throws Exception {
        String wbJar = ClassFileInstaller.getJarPath("WhiteBox.jar");
        String use_whitebox_jar = "-Xbootclasspath/a:" + wbJar;
        String appJar = ClassFileInstaller.getJarPath("boxCache.jar");

        Path userDir = Paths.get(CDSTestUtils.getOutputDir());
        Path moduleDir = Files.createTempDirectory(userDir, "mods");

        //
        // Dump default archive
        //
        OutputAnalyzer output = TestCommon.dump(appJar,
                TestCommon.list("CheckIntegerCacheApp"),
                use_whitebox_jar);
        TestCommon.checkDump(output);

        // Test case 1)
        // - Default options
        System.out.println("----------------------- Test case 1 ----------------------");
        output = TestCommon.exec(appJar, use_whitebox_jar,
                "-XX:+UnlockDiagnosticVMOptions",
                "-XX:+WhiteBoxAPI",
                "CheckIntegerCacheApp",
                "127",
                "true");
        TestCommon.checkExec(output);

        // Test case 2)
        // - Default archive
        // - Larger -XX:AutoBoxCacheMax
        System.out.println("----------------------- Test case 2 ----------------------");
        output = TestCommon.exec(appJar, use_whitebox_jar,
                "-XX:+UnlockDiagnosticVMOptions",
                "-XX:+WhiteBoxAPI",
                "-XX:AutoBoxCacheMax=20000",
                "CheckIntegerCacheApp",
                "20000",
                "false");
        TestCommon.checkExec(output);

        //
        // Dump with -XX:AutoBoxCacheMax specified
        //
        output = TestCommon.dump(appJar,
                TestCommon.list("CheckIntegerCacheApp"),
                "-XX:AutoBoxCacheMax=20000",
                use_whitebox_jar);
        TestCommon.checkDump(output);

        // Test case 3)
        // - Large archived cache
        // - Default options
        System.out.println("----------------------- Test case 3 ----------------------");
        output = TestCommon.exec(appJar, use_whitebox_jar,
                "--module-path",
                moduleDir.toString(),
                "-XX:+UnlockDiagnosticVMOptions",
                "-XX:+WhiteBoxAPI",
                "CheckIntegerCacheApp",
                "127",
                "true");
        TestCommon.checkExec(output);


        // Test case 4)
        // - Large archived cache
        // - Matching options
        System.out.println("----------------------- Test case 4 ----------------------");
        output = TestCommon.exec(appJar, use_whitebox_jar,
                "--module-path",
                moduleDir.toString(),
                "-XX:+UnlockDiagnosticVMOptions",
                "-XX:+WhiteBoxAPI",
                "-XX:AutoBoxCacheMax=20000",
                "CheckIntegerCacheApp",
                "20000",
                "true");
        TestCommon.checkExec(output);

        // Test case 5)
        // - Large archived cache
        // - Larger requested cache
        System.out.println("----------------------- Test case 5 ----------------------");
        output = TestCommon.exec(appJar, use_whitebox_jar,
                "--module-path",
                moduleDir.toString(),
                "-XX:+UnlockDiagnosticVMOptions",
                "-XX:+WhiteBoxAPI",
                "-XX:AutoBoxCacheMax=30000",
                "CheckIntegerCacheApp",
                "30000",
                "false");
        TestCommon.checkExec(output);

        // Test case 6)
        // - Cache is too large to archive
        output = TestCommon.dump(appJar,
                TestCommon.list("CheckIntegerCacheApp"),
                "-XX:AutoBoxCacheMax=2000000",
                "-Xmx1g",
                "-XX:NewSize=1g",
                "-Xlog:cds+heap=info",
                "-Xlog:gc+region+cds",
                "-Xlog:gc+region=trace",
                use_whitebox_jar);
        TestCommon.checkDump(output,
            "Cannot archive the sub-graph referenced from [Ljava.lang.Integer; object",
            "humongous regions have been found and may lead to fragmentation");
    }
}
