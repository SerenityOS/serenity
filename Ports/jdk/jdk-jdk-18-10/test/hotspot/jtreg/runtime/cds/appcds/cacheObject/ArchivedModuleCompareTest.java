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
 * @summary Compare archived system modules with non-archived.
 * @requires vm.cds.archived.java.heap
 * @library /test/jdk/lib/testlibrary /test/lib /test/hotspot/jtreg/runtime/cds/appcds
 * @compile PrintSystemModulesApp.java
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar app.jar PrintSystemModulesApp
 * @run driver ArchivedModuleCompareTest
 */

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.helpers.ClassFileInstaller;

public class ArchivedModuleCompareTest {
    public static void main(String[] args) throws Exception {
        String appJar = ClassFileInstaller.getJarPath("app.jar");

        // Test case 1)
        // Compare the list of archived system module names with non-archived
        // list. They must be the same.
        System.out.println("---------------- Test case 1 -----------------");
        OutputAnalyzer output = TestCommon.dump(appJar,
                        TestCommon.list("PrintSystemModulesApp"));
        TestCommon.checkDump(output);

        output = TestCommon.execOff("-cp", appJar, "PrintSystemModulesApp");
        output.shouldHaveExitValue(0);
        String bootModules1 = TestCommon.filterOutLogs(output.getStdout());

        output = TestCommon.exec(appJar, "PrintSystemModulesApp");
        TestCommon.checkExec(output);
        if (output.getStderr().contains("sharing")) {
            String bootModules2 = TestCommon.filterOutLogs(output.getStdout());
            TestCommon.checkOutputStrings(bootModules1, bootModules2, ", ");
        }

        // Test case 2)
        // Verify --show-module-resolution output with the output from
        // -Xshare:off run
        System.out.println("---------------- Test case 2 -----------------");
        output = TestCommon.execOff("-cp", appJar,
                                    "--show-module-resolution",
                                    "-version");
        output.shouldHaveExitValue(0);
        String moduleResolutionOut1 = TestCommon.filterOutLogs(output.getStdout());

        output = TestCommon.exec(appJar,
                                 "--show-module-resolution",
                                 "-version");
        TestCommon.checkExec(output);
        if (output.getStderr().contains("sharing")) {
            String moduleResolutionOut2 = TestCommon.filterOutLogs(output.getStdout());
            TestCommon.checkOutputStrings(
                moduleResolutionOut1, moduleResolutionOut2, "\n");
        }
    }
}
