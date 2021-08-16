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
 * @summary Test archived system module sub-graph and verify objects are archived.
 * @requires vm.cds.archived.java.heap
 * @library /test/jdk/lib/testlibrary /test/lib /test/hotspot/jtreg/runtime/cds/appcds
 * @build sun.hotspot.WhiteBox
 * @compile CheckArchivedModuleApp.java
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar app.jar CheckArchivedModuleApp
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar WhiteBox.jar sun.hotspot.WhiteBox
 * @run driver ArchivedModuleComboTest
 */

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.helpers.ClassFileInstaller;
import sun.hotspot.WhiteBox;

public class ArchivedModuleComboTest {
    public static void main(String[] args) throws Exception {
        String wbJar = ClassFileInstaller.getJarPath("WhiteBox.jar");
        String use_whitebox_jar = "-Xbootclasspath/a:" + wbJar;
        String appJar = ClassFileInstaller.getJarPath("app.jar");

        Path userDir = Paths.get(CDSTestUtils.getOutputDir());
        Path moduleDir = Files.createTempDirectory(userDir, "mods");

        //
        // Dump without --module-path, without --show-module-resolution
        //
        OutputAnalyzer output = TestCommon.dump(appJar,
                                    TestCommon.list("CheckArchivedModuleApp"),
                                    use_whitebox_jar);
        TestCommon.checkDump(output);

        // Test case 1)
        // - Dump without --module-path, without --show-module-resolution
        // - Run from -cp only and without --show-module-resolution
        //     + archived boot layer module ModuleDescriptors should be used
        //     + archived boot layer configuration should be used
        System.out.println("----------------------- Test case 1 ----------------------");
        output = TestCommon.exec(appJar, use_whitebox_jar,
                                 "-XX:+UnlockDiagnosticVMOptions",
                                 "-XX:+WhiteBoxAPI",
                                 "CheckArchivedModuleApp",
                                 "yes",
                                 "yes");
        TestCommon.checkExec(output);

        // Test case 2)
        // - Dump without --module-path, without --show-module-resolution
        // - Run from -cp only and with --show-module-resolution
        //     + archived boot layer module ModuleDescriptors should be used with
        //       --show-module-resolution (requires resolution)
        //     + archived boot layer Configuration should not be disabled
        System.out.println("----------------------- Test case 2 ----------------------");
        output = TestCommon.exec(appJar, use_whitebox_jar,
                                 "--show-module-resolution",
                                 "-XX:+UnlockDiagnosticVMOptions",
                                 "-XX:+WhiteBoxAPI",
                                 "CheckArchivedModuleApp",
                                 "yes",
                                 "no");
        TestCommon.checkExec(output, "root java.base jrt:/java.base");

        // Test case 3)
        // - Dump without --module-path, without --show-module-resolution
        // - Run with --module-path
        //    + archived boot layer module ModuleDescriptors should be disabled
        //    + archived boot layer Configuration should be disabled
        System.out.println("----------------------- Test case 3 ----------------------");
        output = TestCommon.exec(appJar, use_whitebox_jar,
                                 "--module-path",
                                 moduleDir.toString(),
                                 "-XX:+UnlockDiagnosticVMOptions",
                                 "-XX:+WhiteBoxAPI",
                                 "CheckArchivedModuleApp",
                                 "no",
                                 "no");
        TestCommon.checkExec(output);

        //
        // Dump with --module-path specified (test case 4, 5). Use an
        // empty directory as it's simple and still triggers the case
        // where system module objects are not archived.
        //
        output = TestCommon.dump(appJar,
                                 TestCommon.list("CheckArchivedModuleApp"),
                                 "--module-path",
                                 moduleDir.toString(),
                                 use_whitebox_jar);
        TestCommon.checkDump(output);

        // Test case 4)
        // - Dump with --module-path
        // - Run from -cp only, no archived boot layer module ModuleDescriptors
        //   and Configuration should be found.
        System.out.println("----------------------- Test case 4 ----------------------");
        output = TestCommon.exec(appJar, use_whitebox_jar,
                                 "-XX:+UnlockDiagnosticVMOptions",
                                 "-XX:+WhiteBoxAPI",
                                 "CheckArchivedModuleApp",
                                 "no",
                                 "no");
        TestCommon.checkExec(output);

        // Test case 5)
        // - Dump with --module-path
        // - Run with --module-path, no archived boot layer module ModuleDescriptors
        //   and Configuration should be found.
        System.out.println("----------------------- Test case 5 ----------------------");
        output = TestCommon.exec(appJar, use_whitebox_jar,
                                 "--module-path",
                                 moduleDir.toString(),
                                 "-XX:+UnlockDiagnosticVMOptions",
                                 "-XX:+WhiteBoxAPI",
                                 "CheckArchivedModuleApp",
                                 "no",
                                 "no");
        TestCommon.checkExec(output);

        //
        // Dump without --module-path, with --show-module-resolution
        //
        output = TestCommon.dump(appJar,
                                 TestCommon.list("CheckArchivedModuleApp"),
                                 "--show-module-resolution",
                                 use_whitebox_jar);
        TestCommon.checkDump(output, "root java.base jrt:/java.base");

        // Test case 6)
        // - Dump without --module-path, with --show-module-resolution
        // - Run from -cp only and without --show-module-resolution
        //     + archived boot layer module ModuleDescriptors should be used
        //     + archived boot layer Configuration should be used
        System.out.println("----------------------- Test case 6 ----------------------");
        output = TestCommon.exec(appJar, use_whitebox_jar,
                                 "-XX:+UnlockDiagnosticVMOptions",
                                 "-XX:+WhiteBoxAPI",
                                 "CheckArchivedModuleApp",
                                 "yes",
                                 "yes");
        TestCommon.checkExec(output);

        // Test case 7)
        // - Dump without --module-path, with --show-module-resolution
        // - Run from -cp only and with --show-module-resolution
        //     + archived boot layer module ModuleDescriptors should be used with
        //       --show-module-resolution (requires resolution)
        //     + archived boot layer Configuration should be disabled
        System.out.println("----------------------- Test case 7 ----------------------");
        output = TestCommon.exec(appJar, use_whitebox_jar,
                                 "--show-module-resolution",
                                 "-XX:+UnlockDiagnosticVMOptions",
                                 "-XX:+WhiteBoxAPI",
                                 "CheckArchivedModuleApp",
                                 "yes",
                                 "no");
        TestCommon.checkExec(output, "root java.base jrt:/java.base");
    }
}
