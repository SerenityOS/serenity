/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Testing the loading of a class with the same name in two different class loaders.
 *
 * @requires vm.cds
 * @requires vm.cds.custom.loaders
 *
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds
 * @compile test-classes/CustomLoadee.java
 *     test-classes/CustomLoadee3.java
 *     test-classes/SameNameUnrelatedLoaders.java
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run driver SameNameInTwoLoadersTest
 */

import jdk.test.lib.process.OutputAnalyzer;
import sun.hotspot.WhiteBox;


public class SameNameInTwoLoadersTest {
    private static String appJar;
    private static String customJar;
    private static String useWbParam;

    public static void main(String[] args) throws Exception {
        appJar = JarBuilder.build("SameNameInTwoLoadersTest",
            "SameNameUnrelatedLoaders");

        customJar = JarBuilder.build("SameNameInTwoLoadersTest_custom", "CustomLoadee", "CustomLoadee3");

        useWbParam = "-Xbootclasspath/a:" +
            JarBuilder.build(true, "WhiteBox", "sun/hotspot/WhiteBox");;

        // ====== unrelated loaders
        executeTestCase(getClassList_FP(),
            "SameNameUnrelatedLoaders", "FpBoth");
    }

    private static void executeTestCase(String[] classlist,
        String testClass, String testCaseId) throws Exception {
        classlist[0] = testClass;

        TestCommon.testDump(appJar, classlist, useWbParam);

        OutputAnalyzer output = TestCommon.exec(appJar,
                                 // command-line arguments ...
                                 "--add-opens=java.base/java.security=ALL-UNNAMED",
                                 useWbParam,
                                 "-XX:+UnlockDiagnosticVMOptions",
                                 "-XX:+WhiteBoxAPI",
                                 testClass,
                                 customJar, testCaseId);
        TestCommon.checkExec(output);
    }

    // Single entry, no loader specified (FP method)
    private static String[] getClassList_FP() {
        return new String[] {
            "SameNameUnrelatedLoaders",
            "java/lang/Object id: 1",
            "CustomLoadee id: 10 super: 1 source: " + customJar,
        };
    }
}
