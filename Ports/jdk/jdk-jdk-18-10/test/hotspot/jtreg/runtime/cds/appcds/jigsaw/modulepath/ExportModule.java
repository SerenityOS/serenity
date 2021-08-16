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
 * @run driver ExportModule
 * @summary Tests involve exporting a module from the module path to a jar in the -cp.
 */

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.compiler.CompilerUtils;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.Asserts;

public class ExportModule {

    private static final Path USER_DIR = Paths.get(CDSTestUtils.getOutputDir());

    private static final String TEST_SRC = System.getProperty("test.src");

    private static final Path SRC_DIR = Paths.get(TEST_SRC, "src");
    private static final Path MODS_DIR = Paths.get("mods");

    // the module name of the test module
    private static final String TEST_MODULE1 = "com.greetings";
    private static final String TEST_MODULE2 = "org.astro";

    // unnamed module package name
    private static final String PKG_NAME = "com.nomodule";

    // the module main class
    private static final String MAIN_CLASS = "com.greetings.Main";
    private static final String APP_CLASS = "org.astro.World";

    // unnamed module main class
    private static final String UNNAMED_MAIN = "com.nomodule.Main";

    private static Path moduleDir = null;
    private static Path moduleDir2 = null;
    private static Path appJar = null;
    private static Path appJar2 = null;

    public static void buildTestModule() throws Exception {

        // javac -d mods/$TESTMODULE src/$TESTMODULE/**
        JarBuilder.compileModule(SRC_DIR.resolve(TEST_MODULE2),
                                 MODS_DIR.resolve(TEST_MODULE2),
                                 null);

        // javac -d mods/$TESTMODULE --module-path MOD_DIR src/$TESTMODULE/**
        JarBuilder.compileModule(SRC_DIR.resolve(TEST_MODULE1),
                                 MODS_DIR.resolve(TEST_MODULE1),
                                 MODS_DIR.toString());

        moduleDir = Files.createTempDirectory(USER_DIR, "mlib");
        Path jar = moduleDir.resolve(TEST_MODULE2 + ".jar");
        String classes = MODS_DIR.resolve(TEST_MODULE2).toString();
        JarBuilder.createModularJar(jar.toString(), classes, null);

        moduleDir2 = Files.createTempDirectory(USER_DIR, "mlib2");
        appJar = moduleDir2.resolve(TEST_MODULE1 + ".jar");
        classes = MODS_DIR.resolve(TEST_MODULE1).toString();
        JarBuilder.createModularJar(appJar.toString(), classes, MAIN_CLASS);

        // build a non-modular jar containing the main class which
        // requires the org.astro package
        boolean compiled
            = CompilerUtils.compile(SRC_DIR.resolve(PKG_NAME),
                                    MODS_DIR.resolve(PKG_NAME),
                                    "--module-path", MODS_DIR.toString(),
                                    "--add-modules", TEST_MODULE2,
                                    "--add-exports", "org.astro/org.astro=ALL-UNNAMED");
        Asserts.assertTrue(compiled, "test package did not compile");

        appJar2 = moduleDir2.resolve(PKG_NAME + ".jar");
        classes = MODS_DIR.resolve(PKG_NAME).toString();
        JarBuilder.createModularJar(appJar2.toString(), classes, null);
    }

    public static void main(String... args) throws Exception {
        // compile the modules and create the modular jar files
        buildTestModule();
        String appClasses[] = {MAIN_CLASS, APP_CLASS};
        // create an archive with the class in the org.astro module built in the
        // previous step and the main class from the modular jar in the -cp
        // note: the main class is in the modular jar in the -cp which requires
        // the module in the --module-path
        OutputAnalyzer output = TestCommon.createArchive(
                                        appJar.toString(), appClasses,
                                        "--module-path", moduleDir.toString(),
                                        "--add-modules", TEST_MODULE2, MAIN_CLASS);
        TestCommon.checkDump(output);

        // run it using the archive
        // both the main class and the class from the org.astro module should
        // be loaded from the archive
        TestCommon.run("-Xlog:class+load=trace",
                              "-cp", appJar.toString(),
                              "--module-path", moduleDir.toString(),
                              "--add-modules", TEST_MODULE2, MAIN_CLASS)
            .assertNormalExit(
                "[class,load] org.astro.World source: shared objects file",
                "[class,load] com.greetings.Main source: shared objects file");

        String appClasses2[] = {UNNAMED_MAIN, APP_CLASS};
        // create an archive with the main class from a non-modular jar in the
        // -cp and the class from the org.astro module
        // note: the org.astro package needs to be exported to "ALL-UNNAMED"
        // module since the jar in the -cp is a non-modular jar and thus it is
        // unnmaed.
        output = TestCommon.createArchive(
                                        appJar2.toString(), appClasses2,
                                        "--module-path", moduleDir.toString(),
                                        "--add-modules", TEST_MODULE2,
                                        "--add-exports", "org.astro/org.astro=ALL-UNNAMED",
                                        UNNAMED_MAIN);
        TestCommon.checkDump(output);

        // both the main class and the class from the org.astro module should
        // be loaded from the archive
        TestCommon.run("-Xlog:class+load=trace",
                       "-cp", appJar2.toString(),
                       "--module-path", moduleDir.toString(),
                       "--add-modules", TEST_MODULE2,
                       "--add-exports", "org.astro/org.astro=ALL-UNNAMED",
                       UNNAMED_MAIN)
            .assertNormalExit(
                "[class,load] org.astro.World source: shared objects file",
                "[class,load] com.nomodule.Main source: shared objects file");
    }
}
