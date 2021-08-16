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
 * @summary Test how the dynamic archive handles -XX:SharedBaseAddress
 * @requires vm.cds
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds /test/hotspot/jtreg/runtime/cds/appcds/test-classes
 * @build Hello
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar hello.jar Hello
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:. SharedBaseAddressOption
 */

import jdk.test.lib.Platform;
import jdk.test.lib.helpers.ClassFileInstaller;

public class SharedBaseAddressOption extends DynamicArchiveTestBase {
    static String appJar = ClassFileInstaller.getJarPath("hello.jar");
    static String mainClass = "Hello";

    public static void main(String[] args) throws Exception {
        runTest(SharedBaseAddressOption::testCustomBase);
    }

    static void testCustomBase() throws Exception {

        // (1) -XX:SharedBaseAddress=0 -- the archives will always be relocated at runtime
        doTest("0");

        // (2) -XX:SharedBaseAddress=0x810000000
        if (Platform.is64bit()) {
            doTest("0x810000000");
        }

        // (3) -XX:SharedBaseAddress that's so high that the archive may wrap around 64-bit
        if (Platform.is64bit()) {
            doTest("0xfffffffffff00000");
        }
    }

    static void doTest(String sharedBase) throws Exception {
        String baseArchiveName = getNewArchiveName("base");
        String topArchiveName = getNewArchiveName("top");

        TestCommon.dumpBaseArchive(baseArchiveName, "-XX:SharedBaseAddress=" + sharedBase,
                        "-Xlog:cds=debug",
                        "-Xlog:cds+reloc=debug",
                        "-Xlog:cds+dynamic=debug");

        dump2(baseArchiveName, topArchiveName,
              "-Xlog:cds=debug",
              "-Xlog:cds+reloc=debug",
              "-Xlog:cds+dynamic=debug",
              "-cp", appJar, mainClass)
            .assertNormalExit();

        // a top archive specified in the base archive position
        run2(baseArchiveName, topArchiveName,
             "-Xlog:cds=debug",
             "-Xlog:cds+reloc=debug",
             "-Xlog:cds+dynamic=debug",
             "-cp", appJar, mainClass)
            .assertNormalExit();
    }
}
