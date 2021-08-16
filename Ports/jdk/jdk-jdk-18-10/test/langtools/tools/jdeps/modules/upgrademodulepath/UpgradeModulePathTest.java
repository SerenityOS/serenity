/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8187449
 * @summary Tests jdeps --upgrade-module-path
 * @library ../../lib
 * @build CompilerUtils
 * @modules jdk.jartool
 *          jdk.jdeps/com.sun.tools.jdeps
 * @run testng UpgradeModulePathTest
 */

import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class UpgradeModulePathTest {
    private static final ToolProvider JAR = ToolProvider.findFirst("jar")
        .orElseThrow(() -> new RuntimeException("jar not found"));

    private static final String TEST_SRC = System.getProperty("test.src");

    private static final Path SRC_DIR = Paths.get(TEST_SRC, "src");
    private static final Path MODS_DIR = Paths.get("mods");

    private static final String JAVA_COMPILER = "java.compiler";

    /**
     * Compiles classes used by the test
     */
    @BeforeTest
    public void compileAll() throws Exception {
        CompilerUtils.cleanDir(MODS_DIR);
        assertTrue(CompilerUtils.compileModule(SRC_DIR, MODS_DIR, JAVA_COMPILER));

        Path dir = MODS_DIR.resolve(JAVA_COMPILER);

        // create a modular JAR file for java.compiler
        JAR.run(System.out, System.err,
                "cf", "java.compiler.jar", "-C", dir.toString(), ".");

        // create a JAR file with AUTOMATIC-MODULE-NAME
        try (BufferedWriter bw = Files.newBufferedWriter(Paths.get("manifest"));
             PrintWriter writer = new PrintWriter(bw)) {
            writer.println("AUTOMATIC-MODULE-NAME: java.compiler");
        }

        JAR.run(System.out, System.err,
            "cfm", "auto.jar", "manifest", "-C", dir.toString(), "javax");
    }

    @Test
    public void testSystemModule() throws Exception {
        JdepsRunner jdepsRunner =
            JdepsRunner.run(Stream.of("-m", "java.compiler").toArray(String[]::new));

        assertTrue(jdepsRunner.outputContains("javax.tools"));
        assertTrue(jdepsRunner.outputContains("javax.lang.model"));
        assertTrue(jdepsRunner.outputContains("javax.annotation.processing"));
    }

    @Test
    public void testUpgradedModule() throws Exception {
        JdepsRunner jdepsRunner =
            JdepsRunner.run(Stream.of("--upgrade-module-path", "java.compiler.jar",
                                      "-m", "java.compiler").toArray(String[]::new));

        assertTrue(jdepsRunner.outputContains("javax.tools"));
        assertFalse(jdepsRunner.outputContains("javax.lang.model"));
        assertFalse(jdepsRunner.outputContains("javax.annotation.processing"));
    }

    @Test
    public void testAutomaticModule() throws Exception {
        JdepsRunner jdepsRunner =
            JdepsRunner.run(Stream.of("--upgrade-module-path", "auto.jar",
                                      "-m", "java.compiler").toArray(String[]::new));

        assertTrue(jdepsRunner.outputContains("javax.tools"));
        assertFalse(jdepsRunner.outputContains("javax.lang.model"));
        assertFalse(jdepsRunner.outputContains("javax.annotation.processing"));
    }
}
