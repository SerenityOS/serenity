/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary JvmtiEnv::AddToBootstrapClassLoaderSearch and JvmtiEnv::AddToSystemClassLoaderSearch should disable AppCDS
 * @requires vm.cds
 * @library /test/jdk/lib/testlibrary /test/lib /test/hotspot/jtreg/runtime/cds/appcds
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @compile ../../test-classes/JvmtiApp.java
 * @run driver/timeout=240 JvmtiAddPath
 */

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.process.OutputAnalyzer;
import sun.hotspot.WhiteBox;

public class JvmtiAddPath {
    static String use_whitebox_jar;
    static String[] no_extra_matches = {};
    static String[] check_appcds_enabled = {
        "[class,load] ExtraClass source: shared object"
    };
    static String[] check_appcds_disabled = {
        "[class,load] ExtraClass source: file:"
    };

    private static final Path USER_DIR = Paths.get(CDSTestUtils.getOutputDir());

    private static final String TEST_SRC = System.getProperty("test.src");

    private static final Path SRC_DIR = Paths.get(TEST_SRC, "src");
    private static final Path MODS_DIR = Paths.get("mods");

    // the module name of the test module
    private static final String TEST_MODULE1 = "com.simple";

    // the module main class
    private static final String MAIN_CLASS = "com.simple.Main";

    private static Path moduleDir = null;
    private static Path mainJar = null;

    public static void buildTestModule() throws Exception {

        // javac -d mods/$TESTMODULE --module-path MOD_DIR src/$TESTMODULE/**
        JarBuilder.compileModule(SRC_DIR.resolve(TEST_MODULE1),
                                 MODS_DIR.resolve(TEST_MODULE1),
                                 MODS_DIR.toString());

        moduleDir = Files.createTempDirectory(USER_DIR, "mlib");

        mainJar = moduleDir.resolve(TEST_MODULE1 + ".jar");
        String classes = MODS_DIR.resolve(TEST_MODULE1).toString();
        JarBuilder.createModularJar(mainJar.toString(), classes, MAIN_CLASS);
    }

    static void run(String cp, String... args) throws Exception {
        run(no_extra_matches, cp, args);
    }

    static void run(String[] extra_matches, String cp, String... args) throws Exception {
        String[] opts = {"-cp", cp, "-XX:+UnlockDiagnosticVMOptions", "-XX:+WhiteBoxAPI", use_whitebox_jar};
        opts = TestCommon.concat(opts, args);
        TestCommon.run(opts).assertNormalExit(extra_matches);
    }

    public static void main(String[] args) throws Exception {
        buildTestModule();
        JarBuilder.build("jvmti_app", "JvmtiApp", "ExtraClass");
        JarBuilder.build(true, "WhiteBox", "sun/hotspot/WhiteBox");

        // In all the test cases below, appJar does not contain Hello.class. Instead, we
        // append JAR file(s) that contain Hello.class to the boot classpath, the app
        // classpath, or both, and verify that Hello.class is loaded by the expected ClassLoader.
        String appJar = TestCommon.getTestJar("jvmti_app.jar");         // contains JvmtiApp.class
        String addappJar = mainJar.toString();  // contains Main.class
        String addbootJar = mainJar.toString(); // contains Main.class
        String twoAppJars = appJar + File.pathSeparator + addappJar;
        String modulePath = "--module-path=" + moduleDir.toString();
        String wbJar = TestCommon.getTestJar("WhiteBox.jar");
        use_whitebox_jar = "-Xbootclasspath/a:" + wbJar;

        OutputAnalyzer output = TestCommon.createArchive(
                                    appJar,
                                    TestCommon.list("JvmtiApp", "ExtraClass", MAIN_CLASS),
                                    use_whitebox_jar,
                                    modulePath);
        TestCommon.checkDump(output);

        System.out.println("Test case 1: not adding module path - Hello.class should not be found");
        run(check_appcds_enabled, appJar,
            "-Xlog:class+load", "JvmtiApp", "noadd", MAIN_CLASS); // appcds should be enabled

        System.out.println("Test case 2: add to boot classpath only - should find Hello.class in boot loader");
        String[] toCheck = TestCommon.isDynamicArchive() ? check_appcds_enabled :
                           check_appcds_disabled;
        run(toCheck, appJar,
            "-Xlog:class+load=trace",
            modulePath,
            "JvmtiApp", "bootonly", addbootJar, MAIN_CLASS); // appcds should be disabled

        System.out.println("Test case 3: add to app classpath only - should find Hello.class in app loader");
        run(appJar, modulePath,
            "JvmtiApp", "apponly", addappJar, MAIN_CLASS);

        System.out.println("Test case 4: add to boot and app paths - should find Hello.class in boot loader");
        run(appJar, modulePath,
            "JvmtiApp", "appandboot", addbootJar, addappJar, MAIN_CLASS);

        System.out.println("Test case 5: add to app using -cp, but add to boot using JVMTI - should find Hello.class in boot loader");
        run(appJar, modulePath,
            "JvmtiApp", "bootonly", addappJar, MAIN_CLASS);

        System.out.println("Test case 6: add to app using AppCDS, but add to boot using JVMTI - should find Hello.class in boot loader");
        output = TestCommon.createArchive(
                     appJar, TestCommon.list("JvmtiApp", "ExtraClass"),
                     use_whitebox_jar,
                     modulePath);
        TestCommon.checkDump(output);
        run(twoAppJars, modulePath,
            "JvmtiApp", "bootonly", addappJar, MAIN_CLASS);

        System.out.println("Test case 7: add to app using AppCDS, no JVMTI calls - should find Hello.class in app loader");
        run(twoAppJars, modulePath,
            "JvmtiApp", "noadd-appcds", MAIN_CLASS);
    }
}
