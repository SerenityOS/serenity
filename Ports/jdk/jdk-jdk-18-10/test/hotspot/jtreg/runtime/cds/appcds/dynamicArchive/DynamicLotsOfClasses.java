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
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;
import jdk.test.lib.cds.CDSTestUtils;

/*
 * @test
 * @summary Try to archive lots of classes by searching for classes from the jrt:/ file system. With JDK 12
 *          this will produce an archive with over 30,000 classes.
 * @requires vm.cds
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds /test/hotspot/jtreg/runtime/cds/appcds/dynamicArchive/test-classes
 * @build LoadClasses
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar loadclasses.jar LoadClasses
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar whitebox.jar sun.hotspot.WhiteBox
 * @run main/othervm/timeout=500 -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:./whitebox.jar DynamicLotsOfClasses
 */

import jdk.test.lib.helpers.ClassFileInstaller;

public class DynamicLotsOfClasses extends DynamicArchiveTestBase {

    public static void main(String[] args) throws Exception {
        runTest(DynamicLotsOfClasses::testDefaultBase);
    }

    static void testDefaultBase() throws Exception {
        String topArchiveName = getNewArchiveName("top");
        doTest(topArchiveName);
    }

    private static void doTest(String topArchiveName) throws Exception {
        ArrayList<String> list = new ArrayList<>();
        TestCommon.findAllClasses(list);

        String classList = CDSTestUtils.getOutputDir() + File.separator +
                           "LotsOfClasses.list";
        List<String> lines = list;
        Path file = Paths.get(classList);
        Files.write(file, lines, Charset.forName("UTF-8"));

        String appJar = ClassFileInstaller.getJarPath("loadclasses.jar");
        String mainClass = "LoadClasses";

        String whiteBoxJar = ClassFileInstaller.getJarPath("whitebox.jar");
        String bootClassPath = "-Xbootclasspath/a:" + whiteBoxJar;
        dump(topArchiveName,
             "--add-modules",
             "ALL-SYSTEM",
             "-Xlog:hashtables",
             "-Xmx500m",
             "-Xlog:cds=debug", // test detailed metadata info printing
             "-Xlog:cds+dynamic=info",
             bootClassPath,
             "-XX:+UnlockDiagnosticVMOptions", "-XX:+WhiteBoxAPI",
             "-cp", appJar, mainClass, classList)
            .assertNormalExit("Written dynamic archive 0x",
                              "Detailed metadata info");
    }
}
