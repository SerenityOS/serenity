/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @requires vm.cds
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds
 * @run driver AddOpens
 * @summary sanity test the --add-opens option
 */

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.process.OutputAnalyzer;

public class AddOpens {

    private static final Path USER_DIR = Paths.get(CDSTestUtils.getOutputDir());

    private static final String TEST_SRC = System.getProperty("test.src");

    private static final Path SRC_DIR = Paths.get(TEST_SRC, "src");
    private static final Path MODS_DIR = Paths.get("mods");

    // the module name of the test module
    private static final String TEST_MODULE1 = "com.simple";

    // the module main class
    private static final String MAIN_CLASS = "com.simple.Main";

    private static Path moduleDir = null;
    private static Path moduleDir2 = null;
    private static Path destJar = null;

    public static void buildTestModule() throws Exception {

        // javac -d mods/$TESTMODULE --module-path MOD_DIR src/$TESTMODULE/**
        JarBuilder.compileModule(SRC_DIR.resolve(TEST_MODULE1),
                                 MODS_DIR.resolve(TEST_MODULE1),
                                 MODS_DIR.toString());

        moduleDir = Files.createTempDirectory(USER_DIR, "mlib");
        moduleDir2 = Files.createTempDirectory(USER_DIR, "mlib2");

        Path srcJar = moduleDir.resolve(TEST_MODULE1 + ".jar");
        destJar = moduleDir2.resolve(TEST_MODULE1 + ".jar");
        String classes = MODS_DIR.resolve(TEST_MODULE1).toString();
        JarBuilder.createModularJar(srcJar.toString(), classes, MAIN_CLASS);
        Files.copy(srcJar, destJar);

    }

    public static void main(String... args) throws Exception {
        // compile the modules and create the modular jar files
        buildTestModule();
        String appClasses[] = {MAIN_CLASS};
        // create an archive with both -cp and --module-path in the command line.
        // Only the class in the modular jar in the --module-path will be archived;
        // the class in the modular jar in the -cp won't be archived.
        OutputAnalyzer output = TestCommon.createArchive(
                                        destJar.toString(), appClasses,
                                        "--module-path", moduleDir.toString(),
                                        "-m", TEST_MODULE1);
        TestCommon.checkDump(output);

        // run with the archive using the same command line as in dump time
        // plus the "--add-opens java.base/java.lang=com.simple" option.
        // The main class should be loaded from the archive.
        // The setaccessible(true) on the ClassLoader.defineClass method should
        // be successful.
        TestCommon.run( "-Xlog:class+load=trace",
                        "-cp", destJar.toString(),
                        "--add-opens", "java.base/java.lang=" + TEST_MODULE1,
                        "--module-path", moduleDir.toString(),
                        "-m", TEST_MODULE1, "with_add_opens")
            .assertNormalExit(
                "[class,load] com.simple.Main source: shared objects file",
                "method.setAccessible succeeded!");

    }
}
