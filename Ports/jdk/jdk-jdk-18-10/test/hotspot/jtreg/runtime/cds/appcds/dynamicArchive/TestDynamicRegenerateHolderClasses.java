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
 *
 */

/*
 * @test
 * @bug 8255493
 * @summary LambHello World test for regenerate lambda holder classes in dynamic archive
 * @requires vm.cds
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds /test/hotspot/jtreg/runtime/cds/appcds/dynamicArchive/test-classes
 * @build LambHello sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar lambhello.jar LambHello
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:. TestDynamicRegenerateHolderClasses
 */

import jdk.test.lib.helpers.ClassFileInstaller;

public class TestDynamicRegenerateHolderClasses extends DynamicArchiveTestBase {
    static String CHECK_MESSAGES[] = {"java.lang.invoke.Invokers$Holder source: shared objects file (top)",
                                      "java.lang.invoke.DirectMethodHandle$Holder source: shared objects file (top)",
                                      "java.lang.invoke.DelegatingMethodHandle$Holder source: shared objects file (top)",
                                      "java.lang.invoke.LambdaForm$Holder source: shared objects file (top)"};
    public static void main(String[] args) throws Exception {
        runTest(TestDynamicRegenerateHolderClasses::testDefaultBase);
    }

    static void testDefaultBase() throws Exception {
        String topArchiveName = getNewArchiveName("top");
        doTest(topArchiveName);
    }

    private static void doTest(String topArchiveName) throws Exception {
        String appJar = ClassFileInstaller.getJarPath("lambhello.jar");
        String mainClass = "LambHello";
        dump(topArchiveName,
              "-Xlog:cds",
              "-Xlog:cds+dynamic=debug",
              "-cp", appJar, mainClass)
            .assertNormalExit(output -> {
                    output.shouldContain("Written dynamic archive 0x");
                });
        run(topArchiveName,
             "-Xlog:class+load",
             "-Xlog:cds+dynamic=debug,cds=debug,class+load",
             "-cp", appJar, mainClass)
            .assertNormalExit(output -> {
                    output.shouldContain("LambHello source: shared objects file (top)")
                          .shouldHaveExitValue(0);
                    for (String s : CHECK_MESSAGES) {
                          output.shouldContain(s);
                    }
                });
    }
}
