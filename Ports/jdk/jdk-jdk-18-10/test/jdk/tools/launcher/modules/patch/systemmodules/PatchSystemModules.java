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

/*
 * @test
 * @bug 8157068 8177844
 * @summary Patch java.base and user module with ModuleHashes attribute
 * @library /test/lib
 * @modules jdk.compiler
 * @build jdk.test.lib.compiler.CompilerUtils
 *        jdk.test.lib.util.FileUtils
 *        jdk.test.lib.Platform
 * @run testng PatchSystemModules
 */

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import jdk.test.lib.compiler.CompilerUtils;
import jdk.test.lib.util.FileUtils;
import jdk.test.lib.JDKToolFinder;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import static jdk.test.lib.process.ProcessTools.executeCommand;
import static org.testng.Assert.*;

public class PatchSystemModules {
    private static final String JAVA_HOME = System.getProperty("java.home");

    private static final Path TEST_SRC = Paths.get(System.getProperty("test.src"));

    private static final Path JMODS = Paths.get(JAVA_HOME, "jmods");
    private static final Path MODS_DIR = Paths.get("mods");
    private static final Path JARS_DIR = Paths.get("jars");
    private static final Path PATCH_DIR = Paths.get("patches");
    private static final Path IMAGE = Paths.get("image");
    private static final Path NEW_M1_JAR = JARS_DIR.resolve("new_m1.jar");

    private static final String JAVA_BASE = "java.base";
    private final String[] modules = new String[] { "m1", "m2" };

    @BeforeTest
    private void setup() throws Throwable {
        Path src = TEST_SRC.resolve("src");
        Path src1 = TEST_SRC.resolve("src1");

        for (String name : modules) {
            assertTrue(CompilerUtils.compile(src.resolve(name),
                                             MODS_DIR,
                                             "--module-source-path", src.toString()));
        }

        // compile patched source
        String patchDir = src1.resolve(JAVA_BASE).toString();
        assertTrue(CompilerUtils.compile(src1.resolve(JAVA_BASE),
                                         PATCH_DIR.resolve(JAVA_BASE),
                                         "--patch-module", "java.base=" + patchDir));
        assertTrue(CompilerUtils.compile(src1.resolve("m2"),
                                         PATCH_DIR.resolve("m2")));

        createJars();

        // create an image with only m1 and m2
        if (Files.exists(JMODS)) {
            // create an image with m1,m2
            createImage();
        }

        // compile a different version of m1
        Path tmp = Paths.get("tmp");
        assertTrue(CompilerUtils.compile(src1.resolve("m1"), tmp,
                                         "--module-path", MODS_DIR.toString(),
                                         "--module-source-path", src1.toString()));

        // package new_m1.jar
        jar("--create",
            "--file=" + NEW_M1_JAR.toString(),
            "-C", tmp.resolve("m1").toString(), ".");
    }

    /*
     * Test patching system module and user module on module path
     */
    @Test
    public void test() throws Throwable {
        Path patchedJavaBase = PATCH_DIR.resolve(JAVA_BASE);
        Path patchedM2 = PATCH_DIR.resolve("m2");

        Path home = Paths.get(JAVA_HOME);
        runTest(home,
                "--module-path", MODS_DIR.toString(),
                "-m", "m1/p1.Main", "1");
        runTest(home,
                "--patch-module", "java.base=" + patchedJavaBase,
                "--module-path", MODS_DIR.toString(),
                "-m", "m1/p1.Main", "1");

        runTest(home,
                "--patch-module", "m2=" + patchedM2.toString(),
                "--module-path", MODS_DIR.toString(),
                "-m", "m1/p1.Main", "2");
    }

    /*
     * Test --patch-module on a custom image
     */
    @Test
    public void testImage() throws Throwable {
        if (Files.notExists(JMODS))
            return;

        Path patchedJavaBase = PATCH_DIR.resolve(JAVA_BASE);
        Path patchedM2 = PATCH_DIR.resolve("m2");

        runTest(IMAGE,
                "-m", "m1/p1.Main", "1");
        runTest(IMAGE,
                "--patch-module", "java.base=" + patchedJavaBase,
                "-m", "m1/p1.Main", "1");
        runTest(IMAGE,
                "--patch-module", "m2=" + patchedM2.toString(),
                "-m", "m1/p1.Main", "2");
    }

    /*
     * Test a module linked in a system hashed in ModuleHashes attribute
     * cannot be upgraded
     */
    @Test
    public void upgradeHashedModule() throws Throwable {
        if (Files.notExists(JMODS))
            return;

        // Fail to upgrade m1.jar with mismatched hash
        runTestWithExitCode(getJava(IMAGE),
                "--upgrade-module-path", NEW_M1_JAR.toString(),
                "-m", "m1/p1.Main");

        // test when SystemModules fast path is not enabled, i.e. exploded image
        runTestWithExitCode(getJava(IMAGE),
                "--patch-module", "java.base=" + PATCH_DIR.resolve(JAVA_BASE),
                "--upgrade-module-path", NEW_M1_JAR.toString(),
                "-m", "m1/p1.Main");
    }

    /*
     * Test a module linked in a system hashed in ModuleHashes attribute
     * cannot be upgraded combining with --patch-module and --upgrade-module-path
     */
    @Test
    public void patchHashedModule() throws Throwable {
        if (Files.notExists(JMODS))
            return;

        // --patch-module does not disable hash check.
        // Test that a hashed module cannot be upgraded.
        runTestWithExitCode(getJava(IMAGE),
                "--patch-module", "m1=.jar",
                "--upgrade-module-path", NEW_M1_JAR.toString(),
                "-m", "m1/p1.Main");

        // test when SystemModules fast path is not enabled, i.e. exploded image
        runTestWithExitCode(getJava(IMAGE),
                "--patch-module", "java.base=" + PATCH_DIR.resolve(JAVA_BASE),
                "--patch-module", "m1=.jar",
                "--upgrade-module-path", NEW_M1_JAR.toString(),
                "-m", "m1/p1.Main");
    }

    private void runTestWithExitCode(String... options) throws Throwable {
        assertTrue(executeCommand(options)
                        .outputTo(System.out)
                        .errorTo(System.out)
                        .shouldContain("differs to expected hash")
                        .getExitValue() != 0);
    }

    private void runTest(Path image, String... opts) throws Throwable {
        String[] options =
            Stream.concat(Stream.of(getJava(image)),
                          Stream.of(opts))
                  .toArray(String[]::new);

        ProcessBuilder pb = new ProcessBuilder(options);
        int exitValue =  executeCommand(pb)
                            .outputTo(System.out)
                            .errorTo(System.out)
                            .getExitValue();

        assertTrue(exitValue == 0);
    }

    static void createJars() throws Throwable {
        FileUtils.deleteFileTreeUnchecked(JARS_DIR);

        Files.createDirectories(JARS_DIR);
        Path m1 = JARS_DIR.resolve("m1.jar");
        Path m2 = JARS_DIR.resolve("m2.jar");

        // hash m1 in m2's Hashes attribute
        jar("--create",
            "--file=" + m1.toString(),
            "-C", MODS_DIR.resolve("m1").toString(), ".");

        jar("--create",
            "--file=" + m2.toString(),
            "--module-path", JARS_DIR.toString(),
            "--hash-modules", "m1",
            "-C", MODS_DIR.resolve("m2").toString(), ".");
    }

    static void createImage() throws Throwable {
        FileUtils.deleteFileTreeUnchecked(IMAGE);

        String mpath = JARS_DIR.toString() + File.pathSeparator + JMODS.toString();
        execTool("jlink", "--module-path", mpath,
                 "--add-modules", "m1",
                 "--output", IMAGE.toString());
    }

    static void jar(String... args) throws Throwable {
        execTool("jar", args);
    }

    static void execTool(String tool, String... args) throws Throwable {
        String path = JDKToolFinder.getJDKTool(tool);
        List<String> commands = new ArrayList<>();
        commands.add(path);
        Stream.of(args).forEach(commands::add);
        ProcessBuilder pb = new ProcessBuilder(commands);
        int exitValue =  executeCommand(pb)
            .outputTo(System.out)
            .errorTo(System.out)
            .shouldNotContain("no module is recorded in hash")
            .getExitValue();

        assertTrue(exitValue == 0);
    }

    static String getJava(Path image) {
        boolean isWindows = System.getProperty("os.name").startsWith("Windows");
        Path java = image.resolve("bin").resolve(isWindows ? "java.exe" : "java");
        if (Files.notExists(java))
            throw new RuntimeException(java + " not found");
        return java.toAbsolutePath().toString();
    }
}
