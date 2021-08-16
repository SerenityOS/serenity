/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static jdk.test.lib.process.ProcessTools.*;
import jdk.test.lib.compiler.CompilerUtils;
import static org.testng.Assert.*;

/**
 * @test
 * @bug 8129126 8136802 8137316 8137317 8136804 8139350
 * @library /test/lib
 * @modules jdk.compiler
 *          java.logging
 * @build GetResourceBundleTest jdk.test.lib.process.ProcessTools
 *        jdk.test.lib.compiler.CompilerUtils
 * @run testng GetResourceBundleTest
 * @summary Tests Logger.getLogger + logger.getResourceBundle in an named/unnamed module,
 *          resources are in named and unnamed modules respectively.
 *          Positive tests to ensure that a Logger can retrieve ResourceBundle in its current module.
 *          Negative tests to ensure that a Logger cannot retrieve ResourceBundle in another module.
 *          This test also verifies 8136802 8137316 8137317 8136804 8139350.
 */

public class GetResourceBundleTest {

    private static final String TEST_SRC = System.getProperty("test.src");

    private static final Path MOD_SRC_DIR = Paths.get(TEST_SRC, "src");
    private static final Path MOD_DEST_DIR = Paths.get("mods");
    private static final Path PKG_SRC_DIR = Paths.get(TEST_SRC, "pkgs");
    private static final Path PKG_DEST_DIR = Paths.get("pkgs");

    private static final String[] modules = new String[] {"m1", "m2"};

    /**
     * Compiles all modules used by the test, copy resource files.
     */
    @BeforeClass
    public void setup() throws Exception {
        // compile all modules
        for (String mn : modules) {
            Path msrc = MOD_SRC_DIR.resolve(mn);
            assertTrue(CompilerUtils.compile(msrc, MOD_DEST_DIR,
                    "--module-source-path", MOD_SRC_DIR.toString()));
        }
        assertTrue(CompilerUtils.compile(PKG_SRC_DIR, PKG_DEST_DIR,
                "--module-path", MOD_DEST_DIR.toString(), "--add-modules", String.join(",", modules)));

        // copy resource files
        String[] files = { "m1/p1/resource/p.properties", "m2/p2/resource/p.properties" };
        for(String f : files) {
            Files.copy(MOD_SRC_DIR.resolve(f), MOD_DEST_DIR.resolve(f), REPLACE_EXISTING);
        }
        String p3 = "p3/resource/p.properties";
        Files.copy(PKG_SRC_DIR.resolve(p3), PKG_DEST_DIR.resolve(p3), REPLACE_EXISTING);
    }

    @Test
    public void runWithoutSecurityManager() throws Exception {
        int exitValue = executeTestJava(
                "-cp", PKG_DEST_DIR.toString(),
                "--module-path", MOD_DEST_DIR.toString(),
                "--add-modules", String.join(",", modules),
                "p3.test.ResourceBundleTest")
                .outputTo(System.out)
                .errorTo(System.err)
                .getExitValue();
        assertTrue(exitValue == 0);
    }

    @Test
    public void runWithSecurityManager() throws Exception {
        int exitValue = executeTestJava(
                "-Djava.security.manager",
                "-cp", PKG_DEST_DIR.toString(),
                "--module-path", MOD_DEST_DIR.toString(),
                "--add-modules", String.join(",", modules),
                "p3.test.ResourceBundleTest")
                .outputTo(System.out)
                .errorTo(System.err)
                .getExitValue();
        assertTrue(exitValue == 0);
    }
}
