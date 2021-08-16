/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
 */

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jdk.test.lib.compiler.CompilerUtils;
import jdk.test.lib.util.FileUtils;

import static jdk.test.lib.process.ProcessTools.*;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

/**
 * @test
 * @bug 8142968 8173381 8174740
 * @library /test/lib
 * @modules jdk.compiler jdk.jlink
 * @modules java.base/jdk.internal.module
 * @modules java.base/jdk.internal.org.objectweb.asm
 * @build jdk.test.lib.compiler.CompilerUtils
 *        jdk.test.lib.util.FileUtils
 *        jdk.test.lib.Platform
 *        ModuleTargetHelper UserModuleTest jdk.test.lib.process.ProcessTools
 * @run testng UserModuleTest
 */

public class UserModuleTest {
    private static final String JAVA_HOME = System.getProperty("java.home");
    private static final String TEST_SRC = System.getProperty("test.src");

    private static final Path SRC_DIR = Paths.get(TEST_SRC, "src");
    private static final Path MODS_DIR = Paths.get("mods");
    private static final Path JMODS_DIR = Paths.get("jmods");

    private static final Path IMAGE = Paths.get("image");
    private static final String MAIN_MID = "m1/p1.Main";

    // the names of the modules in this test
    private static String[] modules = new String[] {"m1", "m2", "m3", "m4", "m5"};


    private static boolean hasJmods() {
        if (!Files.exists(Paths.get(JAVA_HOME, "jmods"))) {
            System.err.println("Test skipped. NO jmods directory");
            return false;
        }
        return true;
    }

    /*
     * Compiles all modules used by the test
     */
    @BeforeTest
    public void compileAll() throws Throwable {
        if (!hasJmods()) return;

        for (String mn : modules) {
            Path msrc = SRC_DIR.resolve(mn);
            assertTrue(CompilerUtils.compile(msrc, MODS_DIR,
                "--module-source-path", SRC_DIR.toString(),
                "--add-exports", "java.base/jdk.internal.module=" + mn,
                "--add-exports", "java.base/jdk.internal.org.objectweb.asm=" + mn));
        }

        if (Files.exists(IMAGE)) {
            FileUtils.deleteFileTreeUnchecked(IMAGE);
        }

        createImage(IMAGE, "m1", "m3");

        createJmods("m1", "m4");
    }

    /*
     * Test the image created when linking with a module with
     * no Packages attribute
     */
    @Test
    public void testPackagesAttribute() throws Throwable {
        if (!hasJmods()) return;

        Path java = IMAGE.resolve("bin").resolve("java");
        assertTrue(executeProcess(java.toString(),
                        "--add-exports", "java.base/jdk.internal.module=m1,m4",
                        "--add-exports", "java.base/jdk.internal.org.objectweb.asm=m1,m4",
                        "-m", MAIN_MID)
                        .outputTo(System.out)
                        .errorTo(System.out)
                        .getExitValue() == 0);
    }

    /*
     * Test the image created when linking with an open module
    */
    @Test
    public void testOpenModule() throws Throwable {
        if (!hasJmods()) return;

        Path java = IMAGE.resolve("bin").resolve("java");
        assertTrue(executeProcess(java.toString(), "-m", "m3/p3.Main")
                        .outputTo(System.out)
                        .errorTo(System.out)
                        .getExitValue() == 0);
    }

    /*
     * Disable the fast loading of system modules.
     * Parsing module-info.class
     */
    @Test
    public void disableSystemModules() throws Throwable {
        if (!hasJmods()) return;

        Path java = IMAGE.resolve("bin").resolve("java");
        assertTrue(executeProcess(java.toString(),
                                  "--add-exports", "java.base/jdk.internal.module=m1,m4",
                                  "--add-exports", "java.base/jdk.internal.org.objectweb.asm=m1,m4",
                                  "-Djdk.system.module.finder.disabledFastPath",
                                  "-m", MAIN_MID)
                        .outputTo(System.out)
                        .errorTo(System.out)
                        .getExitValue() == 0);
    }

    /*
     * Test the optimization that deduplicates Set<String> on targets of exports,
     * uses, provides.
     */
    @Test
    public void testDedupSet() throws Throwable {
        if (!hasJmods()) return;

        Path dir = Paths.get("dedupSetTest");
        createImage(dir, "m1", "m2", "m3", "m4");
        Path java = dir.resolve("bin").resolve("java");
        assertTrue(executeProcess(java.toString(),
                         "--add-exports", "java.base/jdk.internal.module=m1,m4",
                         "--add-exports", "java.base/jdk.internal.org.objectweb.asm=m1,m4",
                         "-m", MAIN_MID)
                        .outputTo(System.out)
                        .errorTo(System.out)
                        .getExitValue() == 0);
    }

    @Test
    public void testRequiresStatic() throws Throwable {
        if (!hasJmods()) return;

        Path dir = Paths.get("requiresStatic");
        createImage(dir, "m5");
        Path java = dir.resolve("bin").resolve("java");
        assertTrue(executeProcess(java.toString(), "-m", "m5/p5.Main")
                        .outputTo(System.out)
                        .errorTo(System.out)
                        .getExitValue() == 0);

        // run with m3 present
        assertTrue(executeProcess(java.toString(),
                                  "--module-path", MODS_DIR.toString(),
                                  "--add-modules", "m3",
                                  "-m", "m5/p5.Main")
                        .outputTo(System.out)
                        .errorTo(System.out)
                        .getExitValue() == 0);
    }

    @Test
    public void testRequiresStatic2() throws Throwable {
        if (!hasJmods()) return;

        Path dir = Paths.get("requiresStatic2");
        createImage(dir, "m3", "m5");

        Path java = dir.resolve("bin").resolve("java");
        assertTrue(executeProcess(java.toString(), "-m", "m5/p5.Main")
                        .outputTo(System.out)
                        .errorTo(System.out)
                        .getExitValue() == 0);

        // boot layer with m3 and m5
        assertTrue(executeProcess(java.toString(),
                                  "--add-modules", "m3",
                                  "-m", "m5/p5.Main")
                        .outputTo(System.out)
                        .errorTo(System.out)
                        .getExitValue() == 0);
    }

    private void createJmods(String... modules) throws IOException {
        ModuleTargetHelper.ModuleTarget mt = ModuleTargetHelper.getJavaBaseTarget();
        if (mt == null) {
            throw new RuntimeException("ModuleTarget is missing for java.base");
        }

        String[] values = mt.targetPlatform().split("-");
        String osName = values[0];
        String osArch = values[1];

        // create JMOD files
        Files.createDirectories(JMODS_DIR);
        Stream.of(modules).forEach(mn ->
            assertTrue(jmod("create",
                "--class-path", MODS_DIR.resolve(mn).toString(),
                "--target-platform", mt.targetPlatform(),
                "--main-class", mn.replace('m', 'p') + ".Main",
                JMODS_DIR.resolve(mn + ".jmod").toString()) == 0)
        );
    }


    /**
     * Verify the module descriptor if package p4.dummy is excluded at link time.
     */
    @Test
    public void testModulePackagesAttribute() throws Throwable {
        if (!hasJmods()) return;

        // create an image using JMOD files
        Path dir = Paths.get("packagesTest");
        String mp = Paths.get(JAVA_HOME, "jmods").toString() +
            File.pathSeparator + JMODS_DIR.toString();

        Set<String> modules = Set.of("m1", "m4");
        assertTrue(JLINK_TOOL.run(System.out, System.out,
            "--output", dir.toString(),
            "--exclude-resources", "m4/p4/dummy/*",
            "--add-modules", modules.stream().collect(Collectors.joining(",")),
            "--module-path", mp) == 0);

        // verify ModuleDescriptor
        Path java = dir.resolve("bin").resolve("java");
        assertTrue(executeProcess(java.toString(),
                        "--add-exports", "java.base/jdk.internal.module=m1,m4",
                        "--add-exports", "java.base/jdk.internal.org.objectweb.asm=m1,m4",
                        "--add-modules=m1", "-m", "m4")
            .outputTo(System.out)
            .errorTo(System.out)
            .getExitValue() == 0);
    }

    /**
     * Verify the plugin to retain ModuleTarget attribute
     */
    @Test
    public void testRetainModuleTarget() throws Throwable {
        if (!hasJmods()) return;

        // create an image using JMOD files
        Path dir = Paths.get("retainModuleTargetTest");
        String mp = Paths.get(JAVA_HOME, "jmods").toString() +
            File.pathSeparator + JMODS_DIR.toString();

        Set<String> modules = Set.of("m1", "m4");
        assertTrue(JLINK_TOOL.run(System.out, System.out,
            "--output", dir.toString(),
            "--exclude-resources", "m4/p4/dummy/*",
            "--add-modules", modules.stream().collect(Collectors.joining(",")),
            "--module-path", mp) == 0);

        // verify ModuleDescriptor
        Path java = dir.resolve("bin").resolve("java");
        assertTrue(executeProcess(java.toString(),
                        "--add-exports", "java.base/jdk.internal.module=m1,m4",
                        "--add-exports", "java.base/jdk.internal.org.objectweb.asm=m1,m4",
                        "--add-modules=m1", "-m", "m4", "retainModuleTarget")
            .outputTo(System.out)
            .errorTo(System.out)
            .getExitValue() == 0);
    }

    static final ToolProvider JLINK_TOOL = ToolProvider.findFirst("jlink")
        .orElseThrow(() ->
            new RuntimeException("jlink tool not found")
        );

    static final ToolProvider JMOD_TOOL = ToolProvider.findFirst("jmod")
        .orElseThrow(() ->
            new RuntimeException("jmod tool not found")
        );

    static final String MODULE_PATH = Paths.get(JAVA_HOME, "jmods").toString()
        + File.pathSeparator + MODS_DIR.toString();

    private void createImage(Path outputDir, String... modules) throws Throwable {
        assertTrue(JLINK_TOOL.run(System.out, System.out,
            "--output", outputDir.toString(),
            "--add-modules", Arrays.stream(modules).collect(Collectors.joining(",")),
            "--module-path", MODULE_PATH) == 0);
    }

    private static int jmod(String... options) {
        System.out.println("jmod " + Arrays.asList(options));
        return JMOD_TOOL.run(System.out, System.out, options);
    }
}
