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
 */

/**
 * @test
 * @requires vm.cds
 * @requires vm.gc != "Z"
 * @summary Testing handling of CDS region alignment
 * @comment ZGC may exit the VM if -XX:+UseLargePages is specified but
 *          unavailable. Since this test is independent of the actual GC type, let's
 *          disable it if ZGC is used.
 * @bug 8236847
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds/test-classes
 * @build sun.hotspot.WhiteBox
 * @build Hello
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar hello.jar Hello
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:. SharedRegionAlignmentTest
 */


import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.helpers.ClassFileInstaller;
import sun.hotspot.WhiteBox;

public class SharedRegionAlignmentTest {
    static String appJar = ClassFileInstaller.getJarPath("hello.jar");
    static String mainClass = "Hello";
    static String logArg = "-Xlog:cds";

    static void testCombo() throws Exception {
        // Test the following combinations:
        // Dump (3 combinations): largePageArgs
        // Run  (3 combinations): largePageArgs
        String UseLargePages = "-XX:+UseLargePages";
        String checkString = "Core region alignment: " +
                             WhiteBox.getWhiteBox().metaspaceSharedRegionAlignment();

        String [][] largePageArgs = {
            {}, // default
            {UseLargePages}
        };

        int dumpCase = 0;
        for (String[] dumpLP: largePageArgs) {
            dumpCase ++;
            System.out.println("============================================================");
            System.out.println("dump case (" + dumpCase + "): " + formatLargePageArgs(dumpLP));
            System.out.println("============================================================");

            OutputAnalyzer out = TestCommon.dump(appJar,
                                                 TestCommon.list(mainClass),
                                                 TestCommon.concat(dumpLP, logArg));
            out.shouldContain("Dumping shared data to file")
               .shouldContain(checkString);

            int runCase = 0;
            for (String[] runLP: largePageArgs) {
                runCase++;
                System.out.println("--------------------------------------------------");
                System.out.println("run case (" + dumpCase + "." + runCase + "):" + formatLargePageArgs(runLP));
                System.out.println("--------------------------------------------------");

                TestCommon.run(TestCommon.concat(runLP, "-cp", appJar, logArg, mainClass))
                    .assertNormalExit(output -> {
                            output.shouldContain(checkString)
                                  .shouldContain("Hello World");
                        });
            }
        }
    }

    static String formatLargePageArgs(String args[]) {
        StringBuilder sb = new StringBuilder();
        for (String a : args) {
            sb.append(" ");
            sb.append(a);
        }
        return sb.toString();
    }

    public static void main(String... args) throws Exception {
        testCombo();
    }
}
