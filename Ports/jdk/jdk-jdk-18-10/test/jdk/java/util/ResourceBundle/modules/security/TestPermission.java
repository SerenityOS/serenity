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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import jdk.test.lib.compiler.CompilerUtils;
import static jdk.test.lib.process.ProcessTools.executeTestJava;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

/**
 * @test
 * @library /test/lib
 * @modules jdk.compiler
 * @build TestPermission jdk.test.lib.compiler.CompilerUtils
 * @run testng TestPermission
 * @summary Driver for testing ResourceBundle::getBundle(String, Module)
 */

public class TestPermission {

    private static final String TEST_SRC = System.getProperty("test.src");
    private static final String TEST_CLASSES = System.getProperty("test.classes");

    private static final Path SRC_DIR = Paths.get(TEST_SRC, "src");
    private static final Path MODS_DIR = Paths.get("mods");

    // the names of the modules in this test
    private static List<String> modules = Arrays.asList("test", "m1");

    /**
     * Compiles all modules used by the test
     */
    @BeforeTest
    public void compileAll() throws Exception {
        for (String mn : modules) {
            Path msrc = SRC_DIR.resolve(mn);
            assertTrue(CompilerUtils.compile(msrc, MODS_DIR, "--module-source-path", SRC_DIR.toString()));
        }
    }

    /**
     * Run the modular test
     */
    @Test
    public void runTest() throws Exception {
        int exitValue = executeTestJava("--module-path", MODS_DIR.toString(),
                                        "--add-modules", "m1",
                                        "-Djava.security.manager=allow",
                                        "-m", "test/jdk.test.Main")
                            .outputTo(System.out)
                            .errorTo(System.out)
                            .getExitValue();

        assertTrue(exitValue == 0);
    }
}
