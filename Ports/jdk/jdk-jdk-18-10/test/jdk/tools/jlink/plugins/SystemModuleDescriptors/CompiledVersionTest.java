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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
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
 * @library /test/lib
 * @modules jdk.compiler jdk.jlink
 * @build jdk.test.lib.compiler.CompilerUtils
 *        jdk.test.lib.util.FileUtils
 *        jdk.test.lib.Platform
 *        CompiledVersionTest jdk.test.lib.process.ProcessTools
 * @run testng CompiledVersionTest
 */

public class CompiledVersionTest {
    private static final String JAVA_HOME = System.getProperty("java.home");
    private static final String TEST_SRC = System.getProperty("test.src");

    private static final Path SRC_DIR = Paths.get(TEST_SRC, "src");
    private static final Path MODS_DIR = Paths.get("mods");
    private static final Path IMAGE = Paths.get("image");
    private static final Path JMODS = Paths.get(JAVA_HOME, "jmods");
    private static final String MAIN_MID = "test/jdk.test.Main";

    // the names of the modules in this test
    private static String[] modules  = new String[] { "m1", "m2", "test"};
    private static String[] versions = new String[] { "1.0", "2-ea", "3-internal"};


    private static boolean hasJmods() {
        if (!Files.exists(JMODS)) {
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

        for (int i=0; i < modules.length; i++) {
            String mn = modules[i];
            String version = versions[i];
            Path msrc = SRC_DIR.resolve(mn);
            if (version.equals("0")) {
                assertTrue(CompilerUtils.compile(msrc, MODS_DIR,
                    "--add-exports", "java.base/jdk.internal.module=m1",
                    "--add-exports", "java.base/jdk.internal.org.objectweb.asm=m1",
                    "--module-source-path", SRC_DIR.toString()));
            } else {
                assertTrue(CompilerUtils.compile(msrc, MODS_DIR,
                    "--add-exports", "java.base/jdk.internal.module=m1",
                    "--add-exports", "java.base/jdk.internal.org.objectweb.asm=m1",
                    "--module-source-path", SRC_DIR.toString(),
                    "--module-version", version));
            }
        }

        if (Files.exists(IMAGE)) {
            FileUtils.deleteFileTreeUnchecked(IMAGE);
        }

        createImage(IMAGE, modules);
    }

    private void createImage(Path outputDir, String... modules) throws Throwable {
        Path jlink = Paths.get(JAVA_HOME, "bin", "jlink");
        String mp = JMODS.toString() + File.pathSeparator + MODS_DIR.toString();
        assertTrue(executeProcess(jlink.toString(), "--output", outputDir.toString(),
                        "--add-modules", Arrays.stream(modules).collect(Collectors.joining(",")),
                        "--module-path", mp)
                        .outputTo(System.out)
                        .errorTo(System.out)
                        .getExitValue() == 0);
    }

    /*
     * Test the image created when linking with a module with
     * no Packages attribute
     */
    @Test
    public void testCompiledVersions() throws Throwable {
        if (!hasJmods()) return;

        Path java = IMAGE.resolve("bin").resolve("java");
        Stream<String> options = Stream.concat(
            Stream.of(java.toString(), "-m", MAIN_MID, String.valueOf(modules.length)),
            Stream.concat(Arrays.stream(modules), Arrays.stream(versions))
        );

        assertTrue(executeProcess(options.toArray(String[]::new))
                        .outputTo(System.out)
                        .errorTo(System.out)
                        .getExitValue() == 0);
    }
}
