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
 * @compile ../../test-classes/Hello.java
 * @run driver AddModules
 * @summary sanity test the --add-modules option
 */

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.process.OutputAnalyzer;

public class AddModules {

    private static final Path USER_DIR = Paths.get(CDSTestUtils.getOutputDir());

    private static final String TEST_SRC = System.getProperty("test.src");

    private static final Path SRC_DIR = Paths.get(TEST_SRC, "src");
    private static final Path MODS_DIR = Paths.get("mods");

    // the module name of the test module
    private static final String MAIN_MODULE1 = "com.greetings";
    private static final String MAIN_MODULE2 = "com.hello";
    private static final String SUB_MODULE = "org.astro";

    // the module main class
    private static final String MAIN_CLASS1 = "com.greetings.Main";
    private static final String MAIN_CLASS2 = "com.hello.Main";
    private static final String APP_CLASS = "org.astro.World";

    private static Path moduleDir = null;
    private static Path subJar = null;
    private static Path mainJar1 = null;
    private static Path mainJar2 = null;
    private static String appJar;

    public static void buildTestModule() throws Exception {

        // javac -d mods/$TESTMODULE src/$TESTMODULE/**
        JarBuilder.compileModule(SRC_DIR.resolve(SUB_MODULE),
                                 MODS_DIR.resolve(SUB_MODULE),
                                 null);

        // javac -d mods/$TESTMODULE --module-path MOD_DIR src/$TESTMODULE/**
        JarBuilder.compileModule(SRC_DIR.resolve(MAIN_MODULE1),
                                 MODS_DIR.resolve(MAIN_MODULE1),
                                 MODS_DIR.toString());

        JarBuilder.compileModule(SRC_DIR.resolve(MAIN_MODULE2),
                                 MODS_DIR.resolve(MAIN_MODULE2),
                                 MODS_DIR.toString());

        moduleDir = Files.createTempDirectory(USER_DIR, "mlib");
        subJar = moduleDir.resolve(SUB_MODULE + ".jar");
        String classes = MODS_DIR.resolve(SUB_MODULE).toString();
        JarBuilder.createModularJar(subJar.toString(), classes, null);

        mainJar1 = moduleDir.resolve(MAIN_MODULE1 + ".jar");
        classes = MODS_DIR.resolve(MAIN_MODULE1).toString();
        JarBuilder.createModularJar(mainJar1.toString(), classes, MAIN_CLASS1);

        mainJar2 = moduleDir.resolve(MAIN_MODULE2 + ".jar");
        classes = MODS_DIR.resolve(MAIN_MODULE2).toString();
        JarBuilder.createModularJar(mainJar2.toString(), classes, MAIN_CLASS2);

    }

    public static void main(String... args) throws Exception {
        appJar = JarBuilder.getOrCreateHelloJar();
        // compile the modules and create the modular jar files
        buildTestModule();
        String appClasses[] = {MAIN_CLASS1, MAIN_CLASS2, APP_CLASS};
        // create an archive with the classes in the modules built in the
        // previous step
        OutputAnalyzer output = TestCommon.createArchive(
                                        appJar, appClasses,
                                        "--module-path", moduleDir.toString(),
                                        "--add-modules",
                                        MAIN_MODULE1 + "," + MAIN_MODULE2);
        TestCommon.checkDump(output);
        String prefix[] = {"-Djava.class.path=" + appJar, "-Xlog:class+load=trace"};

        // run the com.greetings module with the archive with the --module-path
        // the same as the one during dump time.
        // The classes should be loaded from the archive.
        TestCommon.runWithModules(prefix,
                                  null, // --upgrade-module-path
                                  moduleDir.toString(), // --module-path
                                  MAIN_MODULE1) // -m
            .assertNormalExit(out -> {
                out.shouldContain("[class,load] com.greetings.Main source: shared objects file")
                   .shouldContain("[class,load] org.astro.World source: shared objects file");
            });

        // run the com.hello module with the archive with the --module-path
        // the same as the one during dump time.
        // The classes should be loaded from the archive.
        TestCommon.runWithModules(prefix,
                                  null, // --upgrade-module-path
                                  moduleDir.toString(), // --module-path
                                  MAIN_MODULE2) // -m
            .assertNormalExit(out -> {
                out.shouldContain("[class,load] com.hello.Main source: shared objects file")
                   .shouldContain("[class,load] org.astro.World source: shared objects file");
            });
    }
}
