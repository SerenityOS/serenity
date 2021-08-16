/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @library ../..
 * @library /test/lib
 * @compile ../../test-classes/Hello.java
 * @run driver OverrideTests
 * @summary AppCDS tests for overriding archived classes with -p and --upgrade-module-path
 */

/*
 * This test consists of 4 tests:
 *   1. Archive PLATFORM class and override with --upgrade-module-path.
 *   2. Archive PLATFORM class and override with -p.
 *   3. Archive APP class and override with --upgrade-module-path.
 *   4. Archive App class and override with -p.
 * For all 4 tests, the class is instantiatied and toString() is called
 * to check whether the archived version or the override version was instantiatied.
 * For tests 1 and 3, the overridden version should be instantiatied.
 * For tests 2 and 4, the archived version should be instantiated.
 *
 * This test uses the same test helper class in all 4 cases. It is located in
 * src/test/jdk/test/Main.java. It will be invoked once for each test cases,
 * with parameters to the test determining how it is run and what the
 * expected result is. See Main.java for a description of these 3 arguments.
 */

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import jdk.test.lib.Asserts;
import jdk.test.lib.cds.CDSOptions;
import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;


public class OverrideTests {
    private static final String TEST_SRC = System.getProperty("test.src");
    private static final Path SRC_DIR = Paths.get(TEST_SRC, "src");
    private static final Path MODS_DIR = Paths.get("mods");
    private static String appJar;
    // the module that is upgraded
    private static final String[] UPGRADED_MODULES = {"jdk.compiler", "java.net.http"};
    private static final Path[] UPGRADEDMODS_DIR = {Paths.get("upgradedmod1"), Paths.get("upgradedmod2")};

    // the test module
    private static final String TEST_MODULE = "test";
    private static final String MAIN_CLASS = "jdk.test.Main";

    // test classes to archive. These are both in UPGRADED_MODULES
    private static final String APP_ARCHIVE_CLASS = "com/sun/tools/javac/Main";
    private static final String PLATFORM_ARCHIVE_CLASS = "java/net/http/HttpTimeoutException";
    private static final String[] ARCHIVE_CLASSES = {APP_ARCHIVE_CLASS, PLATFORM_ARCHIVE_CLASS};
    private static String testArchiveName;


    public static void main(String[] args) throws Exception {
        appJar = JarBuilder.getOrCreateHelloJar();
        OverrideTests tests = new OverrideTests();
        tests.compileModulesAndDumpArchive();
        tests.testAppClassOverriding();
        tests.testPlatformClassOverriding();
    }

    void compileModulesAndDumpArchive() throws Exception {
        boolean compiled;
        // javac -d upgradedmods/$upgradedMod src/$upgradedMod/**
        int i = 0;
        for (String upgradedMod : UPGRADED_MODULES) {
            compiled = CompilerUtils.compile(
                SRC_DIR.resolve(UPGRADED_MODULES[i]),
                UPGRADEDMODS_DIR[i].resolve(UPGRADED_MODULES[i])
            );
            Asserts.assertTrue(compiled, UPGRADED_MODULES[i] + " did not compile");
            i++;
        }

        // javac -d mods/test --upgrade-module-path upgradedmods ...
        compiled = CompilerUtils.compile(
            SRC_DIR.resolve(TEST_MODULE),
            MODS_DIR.resolve(TEST_MODULE),
            "--upgrade-module-path", UPGRADEDMODS_DIR[0].toString() +
             System.getProperty("path.separator") + UPGRADEDMODS_DIR[1].toString()
        );
        Asserts.assertTrue(compiled, TEST_MODULE + " did not compile");

        // dump the archive with jdk.compiler and java.net.http classes in the class list
        OutputAnalyzer output  = TestCommon.dump(appJar, TestCommon.list(ARCHIVE_CLASSES));
        TestCommon.checkDump(output);
        // Make sure all the classes where successfully archived.
        for (String archiveClass : ARCHIVE_CLASSES) {
            output.shouldNotContain("Preload Warning: Cannot find " + archiveClass);
        }

        testArchiveName = TestCommon.getCurrentArchiveName();
    }

    /**
     * APP Class Overriding Tests
     *
     * Archive APP class com.sun.tools.javac.Main from module jdk.compiler.
     *  -At run time, upgrade module jdk.compiler using --upgrade-module-path.
     *   Class.forname(Main) MUST NOT load the archived Main.
     *  -At run time, module jdk.compiler also exists in --module-path.
     *   Class.forname(Main) MUST load the archived Main.
     */
    public void testAppClassOverriding() throws Exception {
        testClassOverriding(APP_ARCHIVE_CLASS, "app");
    }

    /**
     * PLATFORM Class Overriding Tests
     *
     * Archive PLATFORM class java.net.http.HttpTimeoutException from module java.net.http.
     *  -At run time, upgrade module java.net.http using --upgrade-module-path.
     *   Class.forname(HttpTimeoutException) MUST NOT load the archived HttpTimeoutException.
     *  -At run time, module java.net.http also exists in --module-path.
     *   Class.forname(HttpTimeoutException) MUST load the archived HttpTimeoutException.
     */
    public void testPlatformClassOverriding() throws Exception {
        testClassOverriding(PLATFORM_ARCHIVE_CLASS, "platform");
    }

    /**
     * Run the test twice. Once with upgrade module on --upgrade-module-path and once with it on -p.
     * Only modules defined to the PlatformClassLoader are upgradeable.
     * Modules defined to the AppClassLoader are not upgradeble; we expect the
     * FindException to be thrown.
     */
    void testClassOverriding(String archiveClass, String loaderName) throws Exception {
        String mid = TEST_MODULE + "/" + MAIN_CLASS;
        OutputAnalyzer output;
        boolean isAppLoader = loaderName.equals("app");
        int upgradeModIdx = isAppLoader ? 0 : 1;
        String expectedException = "java.lang.module.FindException: Unable to compute the hash";
        String prefix[] = new String[3];
        prefix[0] = "-Djava.class.path=" + appJar;
        prefix[1] = "--add-modules";
        prefix[2] = "java.net.http";

        // Run the test with --upgrade-module-path set to alternate location of archiveClass
        // The alternate version of archiveClass SHOULD be found.
        TestCommon.runWithModules(prefix,
                                  UPGRADEDMODS_DIR[upgradeModIdx].toString(),
                                  MODS_DIR.toString(),
                                  mid,
                                  archiveClass, loaderName, "true") // last 3 args passed to test
            .ifNoMappingFailure(out -> out.shouldContain(expectedException));

        // Now run this same test again, but this time without AppCDS. Behavior should be the same.
        CDSOptions opts = (new CDSOptions())
            .addPrefix(prefix)
            .setArchiveName(testArchiveName).setUseVersion(false)
            .addSuffix("--upgrade-module-path", UPGRADEDMODS_DIR[upgradeModIdx].toString(),
                       "-p", MODS_DIR.toString(), "-m", mid)
            .addSuffix(archiveClass, loaderName, "true");

        output = CDSTestUtils.runWithArchive(opts);

        try {
            output.shouldContain(expectedException);
        } catch (Exception e) {
            TestCommon.checkCommonExecExceptions(output, e);
        }

        // Run the test with -p set to alternate location of archiveClass.
        // The alternate version of archiveClass SHOULD NOT be found.
        TestCommon.runWithModules(
            prefix,
            null,
            UPGRADEDMODS_DIR[upgradeModIdx].toString() + java.io.File.pathSeparator + MODS_DIR.toString(),
            mid,
            archiveClass, loaderName, "false") // last 3 args passed to test
            .assertNormalExit();

        // Now  run this same test again, but this time without AppCDS. Behavior should be the same.
        opts = (new CDSOptions())
            .addPrefix(prefix)
            .setArchiveName(testArchiveName).setUseVersion(false)
            .addSuffix("-p", MODS_DIR.toString(), "-m", mid)
            .addSuffix(archiveClass, loaderName, "false"); // params to the test class

        OutputAnalyzer out = CDSTestUtils.runWithArchive(opts);
        if (!CDSTestUtils.isUnableToMap(out))
            out.shouldHaveExitValue(0);
    }
}
