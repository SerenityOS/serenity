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
 * @summary Test relative paths specified in the -cp.
 * @requires vm.cds
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds
 * @build sun.hotspot.WhiteBox
 * @compile ../test-classes/Hello.java
 * @compile ../test-classes/HelloMore.java
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:. RelativePath
 */

import java.io.File;

public class RelativePath extends DynamicArchiveTestBase {

    public static void main(String[] args) throws Exception {
        runTest(AppendClasspath::testDefaultBase);
    }

    static void testDefaultBase() throws Exception {
        String topArchiveName = getNewArchiveName("top");
        doTest(topArchiveName);
    }

    private static void doTest(String topArchiveName) throws Exception {
        String appJar = JarBuilder.getOrCreateHelloJar();
        String appJar2 = JarBuilder.build("AppendClasspath_HelloMore", "HelloMore");

        int idx = appJar.lastIndexOf(File.separator);
        String jarName = appJar.substring(idx + 1);
        String jarDir = appJar.substring(0, idx);
        // relative path starting with "."
        runWithRelativePath(null, topArchiveName, jarDir,
            "-Xlog:class+load",
            "-Xlog:cds+dynamic=debug,cds=debug",
            "-cp", "." + File.separator + "hello.jar" + File.pathSeparator + appJar2,
            "HelloMore")
            .assertNormalExit(output -> {
                    output.shouldContain("Hello source: shared objects file")
                          .shouldContain("Hello World ... More")
                          .shouldHaveExitValue(0);
                });

        // relative path starting with ".."
        idx = jarDir.lastIndexOf(File.separator);
        String jarSubDir = jarDir.substring(idx + 1);
        runWithRelativePath(null, topArchiveName, jarDir,
            "-Xlog:class+load",
            "-Xlog:cds+dynamic=debug,cds=debug",
            "-cp",
            ".." + File.separator + jarSubDir + File.separator + "hello.jar" + File.pathSeparator + appJar2,
            "HelloMore")
            .assertNormalExit(output -> {
                    output.shouldContain("Hello source: shared objects file")
                          .shouldContain("Hello World ... More")
                          .shouldHaveExitValue(0);
                });

    }
}
