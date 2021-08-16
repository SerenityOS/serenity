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
 * @summary A few edge cases where there's no class to be included in the dynamic archive.
 * @requires vm.cds & !vm.graal.enabled
 * @comment The test assumes that when "java -version" is executed, only a very limited number
 *          of classes are loaded, and all of those are loaded from the default shared archive.
 *
 *          However, when graal is used as the JIT, many extra classes are loaded during VM start-up.
 *          Some of those are loaded dynamically from jrt:/. Some classes are also defined by
 *          LambdaMetafactory. This causes complexity that cannot be easily handled by this test.
 *
 *          The VM code covered by this test can be sufficiently tested with C1/C2. So there's no need
 *          to bend over backwards to run this test with graal.
 *
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds /test/hotspot/jtreg/runtime/cds/appcds/dynamicArchive/test-classes
 * @build StrConcatApp
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar strConcatApp.jar StrConcatApp
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:. NoClassToArchive
 */

import java.io.File;
import jdk.test.lib.cds.CDSOptions;
import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.helpers.ClassFileInstaller;

public class NoClassToArchive extends DynamicArchiveTestBase {
    static final String warningMessage =
        "There is no class to be included in the dynamic archive";
    static final String classList = CDSTestUtils.getOutputFileName("classlist");
    static final String appClass = "StrConcatApp";

    public static void main(String[] args) throws Exception {
        runTest(NoClassToArchive::testDefaultBase);
        runTest(NoClassToArchive::testCustomBase);
    }

    // (1) Test with default base archive + top archive
    static void testDefaultBase() throws Exception {
        String topArchiveName = getNewArchiveName("top");
        doTest(topArchiveName);
    }

    // (2) Test with custom base archive + top archive
    static void testCustomBase() throws Exception {
        String topArchiveName = getNewArchiveName("top2");
        String baseArchiveName = getNewArchiveName("base");
        doTestCustomBase(baseArchiveName, topArchiveName);
    }

    private static void checkWarning(OutputAnalyzer output) throws Exception {
        if (output.firstMatch("bytes: [0-9]+ checksum: [0-9a-f]+") != null) {
            // Patterns like this indicate that a class was not loaded from CDS archive:
            // [info ][class,load] jdk.internal.module.DefaultRoots$$Lambda$1/0x00007f80c4512048 source: jdk.internal.module.DefaultRoots
            // [debug][class,load]  klass: 0x0000000800b77cf8 super: 0x0000000800007450 interfaces: 0x0000000800162538
            //                      loader: [loader data: 0x00007f80f416a5b0 of 'bootstrap'] bytes: 403 checksum: 753e58aa
            System.out.println("test skipped: this platform uses non-archived classes when running -version");
        } else {
            output.shouldContain(warningMessage);
        }
    }

    private static void doTest(String topArchiveName) throws Exception {
        dump(topArchiveName,
             "-Xlog:cds",
             "-Xlog:cds+dynamic=debug",
             "-Xlog:class+load=trace",
             "-version")
            .assertNormalExit(output -> checkWarning(output));

        dump(topArchiveName,
             "-Xlog:cds",
             "-Xlog:cds+dynamic=debug",
             "-Xlog:class+load=trace",
             "-help")
            .assertNormalExit(output -> {
                    // some classes will be loaded from the java.base module
                    output.shouldContain("java.text.MessageFormat source: jrt:/java.base");
                });
    }

    private static void doTestCustomBase(String baseArchiveName, String topArchiveName) throws Exception {
        String appJar = ClassFileInstaller.getJarPath("strConcatApp.jar");
        // dump class list by running the StrConcatApp
        CDSTestUtils.dumpClassList(classList, "-cp", appJar, appClass)
            .assertNormalExit(output -> {
                output.shouldContain("length = 0");
            });

        // create a custom base archive based on the class list
        TestCommon.dumpBaseArchive(baseArchiveName, "-XX:SharedClassListFile=" + classList);

        // create a dynamic archive with the custom base archive
        // no class should be included in the dynamic archive
        dump2(baseArchiveName, topArchiveName,
              "-Xlog:cds",
              "-Xlog:cds+dynamic=debug",
              "-Xlog:class+load=trace",
              "-version")
            .assertNormalExit(out -> checkWarning(out));
    }
}
