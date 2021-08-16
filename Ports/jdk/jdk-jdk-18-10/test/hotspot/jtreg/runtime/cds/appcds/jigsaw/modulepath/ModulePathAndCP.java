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
 * @run driver ModulePathAndCP
 * @summary 2 sets of tests: one with only --module-path in the command line;
 *          another with both -cp and --module-path in the command line.
 */

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.process.OutputAnalyzer;

public class ModulePathAndCP {

    private static final Path USER_DIR = Paths.get(CDSTestUtils.getOutputDir());

    private static final String TEST_SRC = System.getProperty("test.src");

    private static final Path SRC_DIR = Paths.get(TEST_SRC, "src");
    private static final Path MODS_DIR = Paths.get("mods");

    // the module name of the test module
    private static final String MAIN_MODULE = "com.greetings";
    private static final String APP_MODULE = "org.astro";

    // the module main class
    private static final String MAIN_CLASS = "com.greetings.Main";
    private static final String APP_CLASS = "org.astro.World";

    private static Path moduleDir = null;
    private static Path moduleDir2 = null;
    private static Path subJar = null;
    private static Path mainJar = null;
    private static Path destJar = null;

    public static void buildTestModule() throws Exception {

        // javac -d mods/$TESTMODULE src/$TESTMODULE/**
        JarBuilder.compileModule(SRC_DIR.resolve(APP_MODULE),
                                 MODS_DIR.resolve(APP_MODULE),
                                 null);

        // javac -d mods/$TESTMODULE --module-path MOD_DIR src/$TESTMODULE/**
        JarBuilder.compileModule(SRC_DIR.resolve(MAIN_MODULE),
                                 MODS_DIR.resolve(MAIN_MODULE),
                                 MODS_DIR.toString());

        moduleDir = Files.createTempDirectory(USER_DIR, "mlib");
        moduleDir2 = Files.createTempDirectory(USER_DIR, "mlib2");
        subJar = moduleDir.resolve(APP_MODULE + ".jar");
        destJar = moduleDir2.resolve(APP_MODULE + ".jar");
        String classes = MODS_DIR.resolve(APP_MODULE).toString();
        JarBuilder.createModularJar(subJar.toString(), classes, null);
        Files.copy(subJar, destJar);

        mainJar = moduleDir.resolve(MAIN_MODULE + ".jar");
        Path mainJar2 = moduleDir2.resolve(MAIN_MODULE + ".jar");
        classes = MODS_DIR.resolve(MAIN_MODULE).toString();
        JarBuilder.createModularJar(mainJar.toString(), classes, MAIN_CLASS);
        Files.copy(mainJar, mainJar2);

    }

    public static void main(String... args) throws Exception {
        run();
    }

    public static void run(String... extra_runtime_args) throws Exception {
        // compile the modules and create the modular jar files
        buildTestModule();
        String appClasses[] = {MAIN_CLASS, APP_CLASS};
        // create an archive with the classes in the modules built in the
        // previous step
        OutputAnalyzer output = TestCommon.createArchive(
                                        null, appClasses,
                                        "--module-path", moduleDir.toString(),
                                        "-m", MAIN_MODULE);
        TestCommon.checkDump(output);
        String prefix[] = {"-Djava.class.path=", "-Xlog:class+load=trace"};
        prefix = TestCommon.concat(prefix, extra_runtime_args);

        // run with the archive with the --module-path the same as the one during
        // dump time. The classes should be loaded from the archive.
        TestCommon.runWithModules(prefix,
                                  null, // --upgrade-module-path
                                  moduleDir.toString(), // --module-path
                                  MAIN_MODULE) // -m
            .assertNormalExit(out -> {
                out.shouldContain("[class,load] com.greetings.Main source: shared objects file")
                   .shouldContain("[class,load] org.astro.World source: shared objects file");
            });

        // run with the archive with the --module-path different from the one during
        // dump time. The classes should be loaded from the jar files.
        TestCommon.runWithModules(prefix,
                                  null, // --upgrade-module-path
                                  moduleDir2.toString(), // --module-path
                                  MAIN_MODULE) // -m
            .assertNormalExit(out -> {
                out.shouldMatch(".class.load. com.greetings.Main source:.*com.greetings.jar")
                   .shouldMatch(".class.load. org.astro.World source:.*org.astro.jar");
            });

        // create an archive with modular jar files in both -cp and --module-path
        String jars = subJar.toString() + System.getProperty("path.separator") +
                      mainJar.toString();
        output = TestCommon.createArchive( jars, appClasses,
                                           "-Xlog:class+load=trace",
                                           "--module-path", moduleDir.toString(),
                                           "-m", MAIN_MODULE);
        TestCommon.checkDump(output);

        // run with archive with the main class name specified before
        // the module name with the -m option. Since the -m option was specified
        // during dump time, the classes in the jar files after the -cp won't be
        // archived. Therefore, the classes won't be loaded from the archive but
        // will be loaded from the jar files.
        TestCommon.run("-Xlog:class+load=trace",
                       "-cp", jars,
                       "--module-path", moduleDir.toString(),
                       MAIN_CLASS, "-m", MAIN_MODULE)
            .assertNormalExit(out -> {
                out.shouldMatch(".class.load. com.greetings.Main source:.*com.greetings.jar")
                   .shouldMatch(".class.load. org.astro.World source:.*org.astro.jar");
            });

        // similar to the above case but without the main class name. The classes
        // should be loaded from the archive.
        TestCommon.run("-Xlog:class+load=trace",
                       "-cp", jars,
                       "--module-path", moduleDir.toString(),
                       "-m", MAIN_MODULE)
            .assertNormalExit(
              "[class,load] com.greetings.Main source: shared objects file",
              "[class,load] org.astro.World source: shared objects file");

        // create an archive with two modular jars in the --module-path
        output = TestCommon.createArchive(
                                        null, appClasses,
                                        "--module-path", jars,
                                        "-m", MAIN_MODULE);
        TestCommon.checkDump(output);

        // run with the above archive but with the modular jar containing the
        // org.astro module in a different location.
        // The org.astro.World class should be loaded from the jar.
        // The Main class should still be loaded from the archive.
        jars = destJar.toString() + System.getProperty("path.separator") +
                      mainJar.toString();
        TestCommon.runWithModules(prefix,
                                  null, // --upgrade-module-path
                                  jars, // --module-path
                                  MAIN_MODULE) // -m
            .assertNormalExit(out -> {
                out.shouldContain("[class,load] com.greetings.Main source: shared objects file")
                   .shouldMatch(".class.load. org.astro.World source:.*org.astro.jar");
            });
    }
}
