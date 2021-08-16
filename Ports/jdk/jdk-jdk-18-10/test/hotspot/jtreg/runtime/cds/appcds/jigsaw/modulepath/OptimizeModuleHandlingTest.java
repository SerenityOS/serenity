/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @requires vm.cds & !vm.graal.enabled
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds
 * @run driver OptimizeModuleHandlingTest
 * @summary test module path changes for optimization of
 *          module handling.
 *
 */

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.process.OutputAnalyzer;

public class OptimizeModuleHandlingTest {

    private static final Path USER_DIR = Paths.get(CDSTestUtils.getOutputDir());

    private static final String TEST_SRC = System.getProperty("test.src");

    private static final Path SRC_DIR = Paths.get(TEST_SRC, "src");
    private static final Path MODS_DIR = Paths.get("mody");

    // the module name of the test module
    private static final String MAIN_MODULE = "com.bars";
    private static final String TEST_MODULE = "com.foos";

    // the module main class
    private static final String MAIN_CLASS = "com.bars.Main";
    private static final String TEST_CLASS = "com.foos.Test";

    private static String PATH_LIBS = "modylibs";
    private static Path libsDir = null;
    private static Path mainJar = null;
    private static Path testJar = null;

    private static String CLASS_FOUND_MESSAGE = "com.foos.Test found";
    private static String CLASS_NOT_FOUND_MESSAGE = "java.lang.ClassNotFoundException: com.foos.Test";
    private static String OPTIMIZE_ENABLED = "optimized module handling: enabled";
    private static String OPTIMIZE_DISABLED = "optimized module handling: disabled";
    private static String MAIN_FROM_JAR = "class,load.*com.bars.Main.*[.]jar";
    private static String MAIN_FROM_CDS = "class,load.*com.bars.Main.*shared objects file";
    private static String TEST_FROM_JAR = "class,load.*com.foos.Test.*[.]jar";
    private static String TEST_FROM_CDS = "class,load.*com.foos.Test.*shared objects file";
    private static String MAP_FAILED  = "Unable to use shared archive";
    private static String PATH_SEPARATOR = File.pathSeparator;

    public static void buildTestModule() throws Exception {

        // javac -d mods/$TESTMODULE src/$TESTMODULE/**
        JarBuilder.compileModule(SRC_DIR.resolve(TEST_MODULE),
                                 MODS_DIR.resolve(TEST_MODULE),
                                 null);

        // javac -d mods/$TESTMODULE --module-path MOD_DIR src/$TESTMODULE/**
        JarBuilder.compileModule(SRC_DIR.resolve(MAIN_MODULE),
                                 MODS_DIR.resolve(MAIN_MODULE),
                                 MODS_DIR.toString());

        libsDir = Files.createTempDirectory(USER_DIR, PATH_LIBS);
        mainJar = libsDir.resolve(MAIN_MODULE + ".jar");
        testJar = libsDir.resolve(TEST_MODULE + ".jar");

        // modylibs contains both modules com.foos.jar, com.bars.jar
        // build com.foos.jar
        String classes = MODS_DIR.resolve(TEST_MODULE).toString();
        JarBuilder.createModularJar(testJar.toString(), classes, TEST_CLASS);

        // build com.bars.jar
        classes = MODS_DIR.resolve(MAIN_MODULE).toString();
        JarBuilder.createModularJar(mainJar.toString(), classes, MAIN_CLASS);
    }

    public static void main(String... args) throws Exception {
        runWithModulePath();
        runWithJarPath();
    }

    private static void tty(String... args) {
        for (String s : args) {
            System.out.print(s + " ");
        }
        System.out.print("\n");
    }

    public static void runWithModulePath(String... extraRuntimeArgs) throws Exception {
        // compile the modules and create the modular jar files
        buildTestModule();
        String appClasses[] = {MAIN_CLASS, TEST_CLASS};
        // create an archive with the classes in the modules built in the
        // previous step
        OutputAnalyzer output = TestCommon.createArchive(
                                        null, appClasses,
                                        "--module-path",
                                        libsDir.toString(),
                                        "-m", MAIN_MODULE);
        TestCommon.checkDump(output);

        // following 1 - 4 test with CDS off
        tty("1. run with CDS off");
        TestCommon.execOff( "-p", libsDir.toString(),
                            "-m", MAIN_MODULE)
            .shouldHaveExitValue(0)
            .shouldNotContain(OPTIMIZE_ENABLED)
            .shouldContain(CLASS_FOUND_MESSAGE);
        tty("2. run with CDS off, without module path");
        TestCommon.execOff("-cp",
                           mainJar.toString(),
                           MAIN_CLASS)
            .shouldHaveExitValue(0)
            .shouldContain(CLASS_NOT_FOUND_MESSAGE);
        tty("3. run with CDS off, but with full jars in path");
        TestCommon.execOff( "-cp", mainJar.toString() + PATH_SEPARATOR + testJar.toString(),
                            MAIN_CLASS)
            .shouldHaveExitValue(0)
            .shouldNotContain(OPTIMIZE_ENABLED)
            .shouldContain(CLASS_FOUND_MESSAGE);
        tty("4. run with CDS off, only main jar on path, but given moudle path");
        TestCommon.execOff( "-cp", mainJar.toString(),
                            "--module-path", libsDir.toString(),
                            "--add-modules", TEST_MODULE,
                            MAIN_CLASS)
            .shouldHaveExitValue(0)
            .shouldNotContain(OPTIMIZE_ENABLED)
            .shouldContain(CLASS_FOUND_MESSAGE);

        // Following 5 - 10 test with CDS on
        tty("5. run with CDS on, with module path");
        String prefix[] = {"-Djava.class.path=", "-Xlog:cds", "-Xlog:class+load"};
        TestCommon.runWithModules(prefix,
                                 null,               // --upgrade-module-path
                                 libsDir.toString(), // --module-path
                                 MAIN_MODULE)        // -m
            .assertNormalExit(out -> {
                out.shouldNotContain(OPTIMIZE_ENABLED)
                   .shouldContain(OPTIMIZE_DISABLED)
                   .shouldMatch(MAIN_FROM_CDS)       // // archived Main class is for module only
                   .shouldContain(CLASS_FOUND_MESSAGE);
            });
        tty("6. run with CDS on, with module paths set correctly");
        TestCommon.run("-Xlog:cds",
                       "-Xlog:class+load",
                       "-p", libsDir.toString(),
                       "-m", MAIN_MODULE)
            .assertNormalExit(out -> {
                out.shouldContain(CLASS_FOUND_MESSAGE)
                   .shouldMatch(MAIN_FROM_CDS)
                   .shouldMatch(TEST_FROM_CDS)
                   .shouldContain(OPTIMIZE_DISABLED)
                   .shouldNotContain(OPTIMIZE_ENABLED);
            });
        tty("7. run with CDS on, with jar on path");
        TestCommon.run("-Xlog:cds",
                       "-Xlog:class+load",
                       "-cp", mainJar.toString() + PATH_SEPARATOR + testJar.toString(),
                       MAIN_CLASS)
            .assertNormalExit(out -> {
                out.shouldContain(CLASS_FOUND_MESSAGE)
                   .shouldMatch(MAIN_FROM_JAR)
                   .shouldMatch(TEST_FROM_JAR)
                   .shouldContain(OPTIMIZE_DISABLED)
                   .shouldNotContain(OPTIMIZE_ENABLED);
            });

        tty("8. run with CDS on, with --module-path, with jar should fail");
        TestCommon.run("-Xlog:cds",
                       "-Xlog:class+load",
                       "-p", libsDir.toString(),
                       "-cp", mainJar.toString(),
                       MAIN_CLASS)
            .assertNormalExit(out -> {
                out.shouldContain(CLASS_NOT_FOUND_MESSAGE)
                   .shouldMatch(MAIN_FROM_JAR)
                   .shouldNotContain(OPTIMIZE_ENABLED);
            });
        tty("9. run with CDS on, with com.foos on --module-path, with main jar on cp should pass");
        TestCommon.run("-Xlog:cds",
                       "-Xlog:class+load",
                       "--module-path", libsDir.toString(),
                       "--add-modules", TEST_MODULE,
                       "-cp", mainJar.toString(),
                       MAIN_CLASS)
            .assertNormalExit(out -> {
                out.shouldContain(CLASS_FOUND_MESSAGE)
                   .shouldMatch(MAIN_FROM_JAR)
                   .shouldMatch(TEST_FROM_CDS)
                   .shouldNotContain(OPTIMIZE_ENABLED);
            });
        tty("10. run with CDS on, --module-path, with -Xbootclasspath/a: .");
        TestCommon.run("-Xlog:cds",
                       "-Xbootclasspath/a:", ".",
                       "--module-path", libsDir.toString(),
                       MAIN_CLASS)
            .assertAbnormalExit(out -> {
                out.shouldNotContain(CLASS_FOUND_MESSAGE)
                   .shouldContain(OPTIMIZE_DISABLED)           // mapping info
                   .shouldContain("shared class paths mismatch");
            });
    }

    public static void runWithJarPath(String... extraRuntimeArgs) throws Exception {
        // compile the modules and create the modular jar files
        buildTestModule();
        String appClasses[] = {MAIN_CLASS, TEST_CLASS};
        // create an archive with the classes in the modules built in the
        // previous step
        OutputAnalyzer output = TestCommon.createArchive(
                                    testJar.toString() + PATH_SEPARATOR + mainJar.toString(),
                                    appClasses);
        TestCommon.checkDump(output);

        // tests 1 - 4 test with CDS off are same as with module archive.
        tty("tests 1 - 4 test with CDS off are same as with module archive, skipped");

        // Following 5 - 9 test with CDS on
        tty("5. run with CDS on, with module path");
        String prefix[] = {"-Djava.class.path=", "-Xlog:cds"};
        TestCommon.runWithModules(prefix,
                                  null,               // --upgrade-module-path
                                  libsDir.toString(), // --module-path
                                  MAIN_MODULE)        // -m
            .assertAbnormalExit(out -> {
                out.shouldContain(MAP_FAILED)
                   .shouldNotContain(OPTIMIZE_ENABLED)
                   .shouldNotContain(CLASS_FOUND_MESSAGE);
            });
        tty("6. run with CDS on, with module paths set correctly");
        TestCommon.run("-Xlog:cds",
                       "-p", libsDir.toString(),
                       "-m", MAIN_MODULE)
            .assertAbnormalExit(out -> {
                out.shouldContain(MAP_FAILED)
                   .shouldNotContain(CLASS_FOUND_MESSAGE)
                   .shouldNotContain(OPTIMIZE_ENABLED);
            });
        tty("7. run with CDS on, with jar on path");
        TestCommon.run("-Xlog:cds",
                       "-Xlog:class+load",
                       "-cp", testJar.toString() + PATH_SEPARATOR + mainJar.toString(),
                       MAIN_CLASS)
            .assertNormalExit(out -> {
                out.shouldMatch(MAIN_FROM_CDS)
                   .shouldMatch(TEST_FROM_CDS)
                   .shouldContain(CLASS_FOUND_MESSAGE)
                   .shouldContain(OPTIMIZE_ENABLED);
            });
        tty("8. run with CDS on, with --module-path, with jars on classpath should run but not optimized");
        TestCommon.run("-Xlog:cds",
                       "-Xlog:class+load",
                       "-p", libsDir.toString(),
                       "-cp", testJar.toString() + PATH_SEPARATOR + mainJar.toString(),
                       "--add-modules=com.bars",         // Main/Test from jars
                       MAIN_CLASS)
            .assertNormalExit(out -> {
                out.shouldMatch(MAIN_FROM_JAR)
                   .shouldMatch(TEST_FROM_JAR)
                   .shouldContain(CLASS_FOUND_MESSAGE)
                   .shouldNotContain(OPTIMIZE_ENABLED);
            });
        tty("9. run with CDS on,  with main jar only on classpath should not pass");
        TestCommon.run("-Xlog:cds",
                       "-cp", mainJar.toString(),
                       MAIN_CLASS)
            .assertAbnormalExit(out -> {
                out.shouldContain(MAP_FAILED)
                   .shouldNotContain(CLASS_FOUND_MESSAGE)
                   .shouldNotContain(CLASS_NOT_FOUND_MESSAGE)
                   .shouldNotContain(OPTIMIZE_ENABLED)
                   .shouldNotContain(OPTIMIZE_DISABLED);
            });
        tty("10. run with CDS on,  with main/test jars on classpath also with -Xbootclasspath/a:  should not pass");
        TestCommon.run("-Xlog:cds",
                       "-cp", mainJar.toString() + PATH_SEPARATOR + testJar.toString(),
                       "-Xbootclasspath/a:", ".",
                       MAIN_CLASS)
            .assertAbnormalExit(out -> {
                out.shouldNotContain(CLASS_FOUND_MESSAGE)
                   .shouldNotContain(CLASS_NOT_FOUND_MESSAGE)
                   .shouldContain(OPTIMIZE_DISABLED)
                   .shouldNotContain(OPTIMIZE_ENABLED)
                   .shouldContain(MAP_FAILED);
            });
    }
}
