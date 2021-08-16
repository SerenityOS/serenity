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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import jdk.test.lib.cds.CDSTestUtils;

/*
 * @test
 * @summary unsupported base archive tests
 * @requires vm.cds
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds /test/hotspot/jtreg/runtime/cds/appcds/test-classes
 * @compile ../test-classes/Hello.java
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar WhiteBox.jar sun.hotspot.WhiteBox
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:./WhiteBox.jar UnsupportedBaseArchive
 */

public class UnsupportedBaseArchive extends DynamicArchiveTestBase {
    private static final Path USER_DIR = Paths.get(CDSTestUtils.getOutputDir());

    private static final String FS = File.separator;
    private static final String TEST_SRC = System.getProperty("test.src") +
        FS + ".." + FS + "jigsaw" + FS + "modulepath";

    private static final Path SRC_DIR = Paths.get(TEST_SRC, "src");
    private static final Path MODS_DIR = Paths.get("mods");

    // the module name of the test module
    private static final String TEST_MODULE = "com.simple";

    // the module main class
    private static final String MAIN_CLASS = "com.simple.Main";

    private static Path moduleDir = null;
    private static Path srcJar = null;

    private static final String warningBCP =
        "Dynamic archiving is disabled because base layer archive has appended boot classpath";

    private static final String warningModulePath =
        "Dynamic archiving is disabled because base layer archive has module path";

    public static void buildTestModule() throws Exception {

        // javac -d mods/$TESTMODULE --module-path MOD_DIR src/$TESTMODULE/**
        JarBuilder.compileModule(SRC_DIR.resolve(TEST_MODULE),
                                 MODS_DIR.resolve(TEST_MODULE),
                                 MODS_DIR.toString());


        moduleDir = Files.createTempDirectory(USER_DIR, "mlib");
        srcJar = moduleDir.resolve(TEST_MODULE + ".jar");
        String classes = MODS_DIR.resolve(TEST_MODULE).toString();
        JarBuilder.createModularJar(srcJar.toString(), classes, MAIN_CLASS);
    }

    public static void main(String[] args) throws Exception {
        runTest(UnsupportedBaseArchive::test);
    }

    static void test(String args[]) throws Exception {
        String topArchiveName = getNewArchiveName("top");
        String baseArchiveName = getNewArchiveName("base");

        // create a base archive with -Xbootclasspath/a:whitebox.jar
        dumpBaseArchive_WB(baseArchiveName);

        String appJar    = JarBuilder.getOrCreateHelloJar();
        String mainClass = "Hello";

        // dumping of dynamic archive should be disabled with a warning message
        // if the base archive contains -Xbootclasspath/a entries.
        dump2_WB(baseArchiveName, topArchiveName,
             "-Xlog:cds*",
             "-Xlog:cds+dynamic=debug",
             "-Xlog:class+path=info",
             "-cp", appJar, mainClass)
            .assertNormalExit(warningBCP);

        // create a base archive with the --module-path option
        buildTestModule();
        baseArchiveName = getNewArchiveName("base-with-module");
        TestCommon.dumpBaseArchive(baseArchiveName,
                        "-cp", srcJar.toString(),
                        "--module-path", moduleDir.toString(),
                        "-m", TEST_MODULE);

        // dumping of dynamic archive should be disabled with a warning message
        // if the base archive contains --module-path entries.
        topArchiveName = getNewArchiveName("top-with-module");
        dump2(baseArchiveName, topArchiveName,
              "-Xlog:cds*",
              "-Xlog:cds+dynamic=debug",
              "-Xlog:class+path=info",
              "-cp", srcJar.toString(),
              "--module-path", moduleDir.toString(),
              "-m", TEST_MODULE)
            .assertNormalExit(warningModulePath);

    }
}
