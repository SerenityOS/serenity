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

/**
 * @test
 * @requires vm.cds
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm/timeout=480 -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:. MainModuleOnly
 * @summary Test some scenarios with a main modular jar specified in the --module-path and -cp options in the command line.
 */

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.Platform;
import jdk.test.lib.cds.CDSTestUtils;

import jtreg.SkippedException;
import sun.hotspot.code.Compiler;

public class MainModuleOnly extends DynamicArchiveTestBase {

    private static final Path USER_DIR = Paths.get(CDSTestUtils.getOutputDir());

    private static final String FS = File.separator;
    private static final String TEST_SRC = System.getProperty("test.src") +
        FS + ".." + FS + "jigsaw" + FS + "modulepath";

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

    static void testDefaultBase() throws Exception {
        String topArchiveName = getNewArchiveName("top");
        doTest(topArchiveName);
    }

    public static void main(String... args) throws Exception {
        runTest(MainModuleOnly::testDefaultBase);
    }

    public static void doTest(String topArchiveName) throws Exception {
        // compile the modules and create the modular jar files
        buildTestModule();
        // create an archive with both -cp and --module-path in the command line.
        // Only the class in the modular jar in the --module-path will be archived;
        // the class in the modular jar in the -cp won't be archived.
        dump(topArchiveName,
             "-Xlog:cds+dynamic=debug,cds=debug",
             "-cp", destJar.toString(),
             "--module-path", moduleDir.toString(),
             "-m", TEST_MODULE1)
            .assertNormalExit(output -> {
                    output.shouldContain("Written dynamic archive 0x");
                 });

        // run with the archive using the same command line as in dump time.
        // The main class should be loaded from the archive.
        run(topArchiveName,
            "-Xlog:cds+dynamic=debug,cds=debug,class+load=trace",
            "-cp", destJar.toString(),
            "--module-path", moduleDir.toString(),
            "-m", TEST_MODULE1)
            .assertNormalExit(output -> {
                    output.shouldContain("[class,load] com.simple.Main source: shared objects file")
                          .shouldHaveExitValue(0);
                });

        // run with the archive with the main class name inserted before the -m.
        // The main class name will be picked up before the module name. So the
        // main class should be loaded from the jar in the -cp.
        run(topArchiveName,
            "-Xlog:cds+dynamic=debug,cds=debug,class+load=trace",
            "-cp", destJar.toString(),
            "--module-path", moduleDir.toString(),
            MAIN_CLASS, "-m", TEST_MODULE1)
            .assertNormalExit(out ->
                out.shouldMatch(".class.load. com.simple.Main source:.*com.simple.jar"));

        // run with the archive with exploded module. Since during dump time, we
        // only archive classes from the modular jar in the --module-path, the
        // main class should be loaded from the exploded module directory.
        run(topArchiveName,
            "-Xlog:cds+dynamic=debug,cds=debug,class+load=trace",
            "-cp", destJar.toString(),
            "--module-path", MODS_DIR.toString(),
            "-m", TEST_MODULE1 + "/" + MAIN_CLASS)
            .assertNormalExit(out -> {
                out.shouldMatch(".class.load. com.simple.Main source:.*com.simple")
                   .shouldContain(MODS_DIR.toString());
            });

        // run with the archive with the --upgrade-module-path option.
        // CDS will be disabled with this options and the main class will be
        // loaded from the modular jar.
        run(topArchiveName,
            "-Xlog:cds+dynamic=debug,cds=debug,class+load=trace",
            "-cp", destJar.toString(),
            "--upgrade-module-path", moduleDir.toString(),
            "--module-path", moduleDir.toString(),
            "-m", TEST_MODULE1)
            .assertSilentlyDisabledCDS(out -> {
                out.shouldHaveExitValue(0)
                   .shouldMatch("CDS is disabled when the.*option is specified")
                   .shouldMatch(".class.load. com.simple.Main source:.*com.simple.jar");
            });

        boolean skippedTest = false;
        if (!Compiler.isGraalEnabled()) {
            // run with the archive with the --limit-modules option.
            // CDS will be disabled with this options and the main class will be
            // loaded from the modular jar.
            run(topArchiveName,
                "-Xlog:cds+dynamic=debug,cds=debug,class+load=trace",
                "-cp", destJar.toString(),
                "--limit-modules", "java.base," + TEST_MODULE1,
                "--module-path", moduleDir.toString(),
                "-m", TEST_MODULE1)
                .assertSilentlyDisabledCDS(out -> {
                    out.shouldHaveExitValue(0)
                       .shouldMatch("CDS is disabled when the.*option is specified")
                       .shouldMatch(".class.load. com.simple.Main source:.*com.simple.jar");
            });
        } else {
            skippedTest = true;
        }
        // run with the archive with the --patch-module option.
        // CDS will be disabled with this options and the main class will be
        // loaded from the modular jar.
        run(topArchiveName,
            "-Xlog:cds+dynamic=debug,cds=debug,class+load=trace",
            "-cp", destJar.toString(),
            "--patch-module", TEST_MODULE1 + "=" + MODS_DIR.toString(),
            "--module-path", moduleDir.toString(),
            "-m", TEST_MODULE1)
            .assertSilentlyDisabledCDS(out -> {
                out.shouldHaveExitValue(0)
                   .shouldMatch("CDS is disabled when the.*option is specified")
                   .shouldMatch(".class.load. com.simple.Main source:.*com.simple.jar");
            });
        // modify the timestamp of the jar file
        (new File(destJar.toString())).setLastModified(System.currentTimeMillis() + 2000);
        // run with the archive and the jar with modified timestamp.
        // It should fail due to timestamp of the jar doesn't match the one
        // used during dump time.
        run(topArchiveName,
            "-Xlog:cds+dynamic=debug,cds=debug,class+load=trace",
            "-cp", destJar.toString(),
            "--module-path", moduleDir.toString(),
            "-m", TEST_MODULE1)
            .assertAbnormalExit(
                "A jar file is not the one used while building the shared archive file:");
        // create an archive with a non-empty directory in the --module-path.
        // The dumping process will exit with an error due to non-empty directory
        // in the --module-path.
        dump(topArchiveName,
             "-Xlog:cds+dynamic=debug,cds=debug",
             "-cp", destJar.toString(),
             "--module-path", MODS_DIR.toString(),
             "-m", TEST_MODULE1 + "/" + MAIN_CLASS)
            .assertAbnormalExit(output -> {
                output.shouldMatch("Error: non-empty directory.*com.simple");
                });

        // test module path with very long length
        //
        // This test can't be run on the windows platform due to an existing
        // issue in ClassLoader::get_canonical_path() (JDK-8190737).
        if (Platform.isWindows()) {
            System.out.println("Long module path test cannot be tested on the Windows platform.");
            return;
        }
        Path longDir = USER_DIR;
        int pathLen = longDir.toString().length();
        int PATH_LEN = 2034;
        int MAX_DIR_LEN = 250;
        while (pathLen < PATH_LEN) {
            int remaining = PATH_LEN - pathLen;
            int subPathLen = remaining > MAX_DIR_LEN ? MAX_DIR_LEN : remaining;
            char[] chars = new char[subPathLen];
            Arrays.fill(chars, 'x');
            String subPath = new String(chars);
            longDir = Paths.get(longDir.toString(), subPath);
            pathLen = longDir.toString().length();
        }
        File longDirFile = new File(longDir.toString());
        try {
            longDirFile.mkdirs();
        } catch (Exception e) {
            throw e;
        }
        Path longDirJar = longDir.resolve(TEST_MODULE1 + ".jar");
        // IOException results from the Files.copy() call on platform
        // such as MacOS X. Test can't be proceeded further with the
        // exception.
        try {
            Files.copy(destJar, longDirJar);
        } catch (java.io.IOException ioe) {
            System.out.println("Caught IOException from Files.copy(). Cannot continue.");
            return;
        }
        dump(topArchiveName,
             "-Xlog:cds+dynamic=debug,cds=debug",
             "-cp", destJar.toString(),
             "-Xlog:exceptions=trace",
             "--module-path", longDirJar.toString(),
             "-m", TEST_MODULE1)
            .ifAbnormalExit(output -> {
                output.shouldMatch("os::stat error.*CDS dump aborted");
                });

        if (skippedTest) {
            throw new SkippedException("Skipped --limit-modules test; it can't be run with Graal enabled");
        }
    }
}
