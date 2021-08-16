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
 * @summary Archive lambda proxy class is in the base archive. The lambda proxy
 *          class should be loaded from the base archive during runtime.
 *
 * @requires vm.cds
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds /test/hotspot/jtreg/runtime/cds/appcds/dynamicArchive/test-classes
 * @build LambHello
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar lambhello.jar LambHello
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:. LambdaInBaseArchive
 */

import jdk.test.lib.cds.CDSOptions;
import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.helpers.ClassFileInstaller;

public class LambdaInBaseArchive extends DynamicArchiveTestBase {
    public static void main(String[] args) throws Exception {
        createBaseArchive();
        runTest(LambdaInBaseArchive::testCustomBase);
    }

    static String helloBaseArchive = getNewArchiveName("base-with-hello");
    static String appJar = ClassFileInstaller.getJarPath("lambhello.jar");
    static String mainClass = "LambHello";
    static String classList = "lambhello.list";

    static void createBaseArchive() throws Exception {
        // dump class list
        CDSTestUtils.dumpClassList(classList, "-cp", appJar, mainClass);

        // create archive with the class list
        CDSOptions opts = (new CDSOptions())
            .addPrefix("-XX:ExtraSharedClassListFile=" + classList,
                       "-cp", appJar,
                       "-Xlog:class+load,cds")
            .setArchiveName(helloBaseArchive);
        CDSTestUtils.createArchiveAndCheck(opts);
    }

    // Test with custom base archive + top archive
    static void testCustomBase() throws Exception {
        String topArchiveName = getNewArchiveName("top2");
        doTest(helloBaseArchive, topArchiveName);
    }

    private static void doTest(String baseArchiveName, String topArchiveName) throws Exception {
        dump2(baseArchiveName, topArchiveName,
             "-Xlog:class+load,cds,cds+dynamic=debug",
             "-cp", appJar, mainClass)
            .assertNormalExit(output -> {
                    output.shouldContain("Written dynamic archive 0x");
                });

        run2(baseArchiveName, topArchiveName,
            "-Xlog:class+load,cds+dynamic=debug,cds=debug",
            "-cp", appJar, mainClass)
            .assertNormalExit(output -> {
                output.shouldContain("LambHello source: shared objects file")
                      .shouldMatch("class.load.*LambHello[$][$]Lambda[$].*0x.*source:.shared.objects.file")
                      .shouldNotMatch("class.load.*LambHello[$][$]Lambda[$].*0x.*source:.shared.objects.file.*(top)");
                });

    }
}
