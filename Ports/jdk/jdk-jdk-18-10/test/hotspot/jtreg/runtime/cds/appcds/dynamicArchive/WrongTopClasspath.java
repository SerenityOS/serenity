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

import java.io.File;

/*
 * @test
 * @summary correct classpath for bottom archive, but bad classpath for top archive
 * @requires vm.cds
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds /test/hotspot/jtreg/runtime/cds/appcds/test-classes
 * @build GenericTestApp sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar WhiteBox.jar sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar GenericTestApp.jar GenericTestApp
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar WrongJar.jar GenericTestApp
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:./WhiteBox.jar WrongTopClasspath
 */

import jdk.test.lib.helpers.ClassFileInstaller;

public class WrongTopClasspath extends DynamicArchiveTestBase {

    public static void main(String[] args) throws Exception {
        runTest(WrongTopClasspath::test);
    }

    static void test(String args[]) throws Exception {
        String topArchiveName = getNewArchiveName("top");
        String baseArchiveName = getNewArchiveName("base");
        TestCommon.dumpBaseArchive(baseArchiveName);

        String appJar    = ClassFileInstaller.getJarPath("GenericTestApp.jar");
        String wrongJar  = ClassFileInstaller.getJarPath("WrongJar.jar");
        String mainClass = "GenericTestApp";

        // Dump the top archive using "-cp GenericTestApp.jar" ...
        dump2_WB(baseArchiveName, topArchiveName,
                 "-Xlog:cds*",
                 "-Xlog:cds+dynamic=debug",
                 "-cp", appJar, mainClass)
            .assertNormalExit();

        // ... but try to load the top archive using "-cp WrongJar.jar".
        // Use -Xshare:auto so top archive can fail after base archive has succeeded,
        // but the app will continue to run.
        run2_WB(baseArchiveName, topArchiveName,
                "-Xlog:cds*",
                "-Xlog:cds+dynamic=debug",
                "-Xlog:class+path=info",
                "-Xshare:auto",
                "-cp", wrongJar, mainClass,
                "assertShared:java.lang.Object",  // base archive still useable
                "assertNotShared:GenericTestApp") // but top archive is not useable
          .assertNormalExit("The top archive failed to load");
    }
}
