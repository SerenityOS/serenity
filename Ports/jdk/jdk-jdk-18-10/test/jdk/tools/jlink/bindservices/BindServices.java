/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jdk.test.lib.compiler.CompilerUtils;
import static jdk.test.lib.process.ProcessTools.*;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

/**
 * @test
 * @bug 8174826
 * @library /test/lib
 * @modules jdk.compiler jdk.jlink
 * @build BindServices jdk.test.lib.process.ProcessTools
 *        jdk.test.lib.compiler.CompilerUtils
 * @run testng BindServices
 */

public class BindServices {
    private static final String JAVA_HOME = System.getProperty("java.home");
    private static final String TEST_SRC = System.getProperty("test.src");

    private static final Path SRC_DIR = Paths.get(TEST_SRC, "src");
    private static final Path MODS_DIR = Paths.get("mods");

    private static final String MODULE_PATH =
        Paths.get(JAVA_HOME, "jmods").toString() +
            File.pathSeparator + MODS_DIR.toString();

    // the names of the modules in this test
    private static String[] modules = new String[] {"m1", "m2", "m3"};


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
                "--module-source-path", SRC_DIR.toString()));
        }
    }

    @Test
    public void noServiceBinding() throws Throwable {
        if (!hasJmods()) return;

        Path dir = Paths.get("noServiceBinding");

        // no service binding and does not link m2,m3 providers.
        JLink.run("--output", dir.toString(),
                  "--module-path", MODULE_PATH,
                  "--add-modules", "m1").output();

        testImage(dir, "m1");
    }

    @Test
    public void fullServiceBinding() throws Throwable {
        if (!hasJmods()) return;

        Path dir = Paths.get("fullServiceBinding");

        // full service binding
        // m2 is a provider used by m1.  During service binding, when m2 is
        // resolved, m2 uses p2.T that causes m3 to be linked as it is a
        // provider to p2.T
        JLink.run("--output", dir.toString(),
                  "--module-path", MODULE_PATH,
                  "--add-modules", "m1",
                  "--bind-services",
                  "--limit-modules", "m1,m2,m3");

        testImage(dir, "m1", "m2", "m3");
    }

    @Test
    public void testVerbose() throws Throwable {
        if (!hasJmods()) return;

        Path dir = Paths.get("verbose");

        List<String> output =
            JLink.run("--output", dir.toString(),
                      "--module-path", MODULE_PATH,
                      "--add-modules", "m1",
                      "--bind-services",
                      "--verbose",
                      "--limit-modules", "m1,m2,m3").output();

        List<String> expected = List.of(
            "m1 " + MODS_DIR.resolve("m1").toUri().toString(),
            "m2 " + MODS_DIR.resolve("m2").toUri().toString(),
            "m3 " + MODS_DIR.resolve("m3").toUri().toString(),
            "java.base provides java.nio.file.spi.FileSystemProvider used by java.base",
            "m1 provides p1.S used by m1",
            "m2 provides p1.S used by m1",
            "m2 provides p2.T used by m2",
            "m3 provides p2.T used by m2",
            "m3 provides p3.S not used by any observable module"
        );

        assertTrue(output.containsAll(expected));

        testImage(dir, "m1", "m2", "m3");
    }

    @Test
    public void testVerboseAndNoBindServices() throws Throwable {
        if (!hasJmods()) return;

        Path dir = Paths.get("verboseNoBind");

        List<String> output =
            JLink.run("--output", dir.toString(),
                      "--module-path", MODULE_PATH,
                      "--verbose",
                      "--add-modules", "m1").output();

        assertTrue(output.contains("m1 provides p1.S used by m1"));

        testImage(dir, "m1");
    }

    /*
     * Tests the given ${java.home} to only contain the specified modules
     */
    private void testImage(Path javaHome, String... modules) throws Throwable {
        Path java = javaHome.resolve("bin").resolve("java");
        String[] cmd = Stream.concat(
            Stream.of(java.toString(), "-m", "m1/p1.Main"),
            Stream.of(modules)).toArray(String[]::new);

        assertTrue(executeProcess(cmd).outputTo(System.out)
                                      .errorTo(System.out)
                                      .getExitValue() == 0);
    }

    static class JLink {
        static final ToolProvider JLINK_TOOL = ToolProvider.findFirst("jlink")
            .orElseThrow(() ->
                new RuntimeException("jlink tool not found")
            );

        static JLink run(String... options) {
            JLink jlink = new JLink();
            assertTrue(jlink.execute(options) == 0);
            return jlink;
        }

        final List<String> output = new ArrayList<>();
        private int execute(String... options) {
            System.out.println("jlink " +
                Stream.of(options).collect(Collectors.joining(" ")));

            StringWriter writer = new StringWriter();
            PrintWriter pw = new PrintWriter(writer);
            int rc = JLINK_TOOL.run(pw, pw, options);
            System.out.println(writer.toString());
            Stream.of(writer.toString().split("\\v"))
                  .map(String::trim)
                  .forEach(output::add);
            return rc;
        }

        boolean contains(String s) {
            return output.contains(s);
        }

        List<String> output() {
            return output;
        }
    }
}
