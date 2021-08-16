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
 */

/*
 * @test
 * @summary Tests jdeps option on a MR jar with missing dependences
 * @library ../lib
 * @build CompilerUtils JdepsUtil
 * @modules jdk.jdeps/com.sun.tools.jdeps
 * @run testng MissingDepsTest
 */

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertEquals;


public class MissingDepsTest {
    private static final String TEST_SRC = System.getProperty("test.src");
    private static final Path SRC_DIR = Paths.get(TEST_SRC, "src");

    private static final Path MODS_DIR = Paths.get("mods");
    private static final Path CLASSES_DIR = Paths.get("classes");

    private static final ToolProvider JAR_TOOL = ToolProvider.findFirst("jar").orElseThrow();
    private static final String VERSION = "13";

    private static final Set<String> modules = Set.of("m1", "m2");

    /**
     * Compiles classes used by the test
     */
    @BeforeTest
    public void compileAll() throws Exception {
        CompilerUtils.cleanDir(MODS_DIR);
        modules.forEach(mn ->
                assertTrue(CompilerUtils.compileModule(SRC_DIR, MODS_DIR, mn)));

        // compile a versioned class file
        Path versionedFile = Paths.get(TEST_SRC, "p/internal/X.java");
        assertTrue(CompilerUtils.compile(versionedFile, CLASSES_DIR, "-cp", MODS_DIR.resolve("m2").toString()));

        // create a modular multi-release m1.jar
        JAR_TOOL.run(System.out, System.err, "cf", "m1.jar",
                     "-C", MODS_DIR.resolve("m1").toString(), ".");
        JAR_TOOL.run(System.out, System.err, "uf", "m1.jar",
                     "--release", VERSION, "-C", CLASSES_DIR.toString(), "p/internal/X.class");
        // create a non-modular multi-release mr.jar
        JAR_TOOL.run(System.out, System.err, "cf", "mr.jar",
                     "-C", MODS_DIR.resolve("m1").toString(), "p/Foo.class",
                     "--release", VERSION, "-C", CLASSES_DIR.toString(), "p/internal/X.class");
    }

    @Test
    public void checkModuleDeps() {
        JdepsTest test = new JdepsTest();
        test.options(List.of("--module-path", "m1.jar", "--multi-release", VERSION, "--check", "m1"));
        test.checkMissingDeps();
        test.ignoreMissingDeps("requires java.management");
    }

    @Test
    public void genModuleInfo() {
        JdepsTest test = new JdepsTest();
        test.options(List.of("--generate-module-info", ".", "--multi-release", VERSION, "mr.jar"));
        test.checkMissingDeps();
        Path file = Paths.get("mr", "versions", VERSION, "module-info.java");
        test.ignoreMissingDeps(file.toString());
        assertTrue(Files.exists(file));
    }

    @Test
    public void listModuleDeps() {
        JdepsTest test = new JdepsTest();
        test.options(List.of("--list-deps", "--multi-release", VERSION, "mr.jar"));
        test.checkMissingDeps();
        test.ignoreMissingDeps("java.management");
    }

    class JdepsTest {
        // set DEBUG to true to show the jdeps output
        static final boolean DEBUG = false;
        List<String> options;
        JdepsTest options(List<String> options) {
            this.options = options;
            return this;
        }

        private void checkMissingDeps() {
            JdepsRunner jdepsRunner = new JdepsRunner(options.toArray(new String[0]));
            int rc = jdepsRunner.run(DEBUG);
            assertTrue(rc != 0);
            String regex = "\\s+13/p.internal.X\\s+->\\s+q.T\\s+not found";
            assertTrue(Arrays.stream(jdepsRunner.output()).anyMatch(l -> l.matches(regex)));
        }

        public void ignoreMissingDeps(String expected) {
            JdepsRunner jdepsRunner = new JdepsRunner(Stream.concat(Stream.of("--ignore-missing-deps"), options.stream())
                                                            .toArray(String[]::new));
            int rc = jdepsRunner.run(DEBUG);
            assertTrue(rc == 0);
            System.out.println("Expected: " + expected);
            assertTrue(Arrays.stream(jdepsRunner.output()).anyMatch(l -> l.contains(expected)));
        }
    }
}
