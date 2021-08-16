/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8170772
 * @library /test/lib
 * @modules jdk.compiler
 * @build CacheTest jdk.test.lib.compiler.CompilerUtils
 * @run testng CacheTest
 */

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static jdk.test.lib.process.ProcessTools.*;
import jdk.test.lib.compiler.CompilerUtils;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

@Test
public class CacheTest {

    private static final String TEST_SRC = System.getProperty("test.src");

    private static final Path SRC_DIR = Paths.get(TEST_SRC, "src");
    private static final Path MODS_DIR = Paths.get("mods");

    private static final String TEST_MODULE = "test";
    private static final String MAIN_BUNDLES_MODULE = "mainbundles";

    private static final String MAIN = "test/jdk.test.Main";
    private static final String MAIN_CLASS = "jdk.test.Main";

    @BeforeTest
    public void compileTestModules() throws Exception {

        for (String mn : new String[] {MAIN_BUNDLES_MODULE, TEST_MODULE}) {
            boolean compiled =
                CompilerUtils.compile(SRC_DIR.resolve(mn),
                                      MODS_DIR.resolve(mn),
                            "--module-path", MODS_DIR.toString());
            assertTrue(compiled, "module " + mn + " did not compile");
        }

        Path res = Paths.get("jdk", "test", "resources", "MyResources.properties");
        Path dest = MODS_DIR.resolve(MAIN_BUNDLES_MODULE).resolve(res);
        Files.createDirectories(dest.getParent());
        Files.copy(SRC_DIR.resolve(MAIN_BUNDLES_MODULE).resolve(res), dest);
    }

    /**
     * Load resource bundle in cache first.  Verify that the subsequent
     * loading of ResourceBundle from another module will not get it from cache.
     */
    @Test
    public void loadCacheFirst() throws Exception {
        assertTrue(executeTestJava("--module-path", MODS_DIR.toString(),
                                   "-m", MAIN, "cache")
                        .outputTo(System.out)
                        .errorTo(System.out)
                        .getExitValue() == 0);

        assertTrue(executeTestJava("--class-path", MODS_DIR.resolve(TEST_MODULE).toString(),
                                   "--module-path", MODS_DIR.resolve(MAIN_BUNDLES_MODULE).toString(),
                                   "--add-modules", MAIN_BUNDLES_MODULE,
                                   MAIN_CLASS, "cache")
                        .outputTo(System.out)
                        .errorTo(System.out)
                        .getExitValue() == 0);
    }

    /**
     * Load non-existent resource bundle in cache first.  Verify that
     * the subsequent loading of ResourceBundle from another module
     * can still successfully load the bundle.
     */
    @Test
    public void loadNonExistentBundleInCache() throws Exception {
        assertTrue(executeTestJava("--module-path", MODS_DIR.toString(),
                                   "-m", MAIN)
                        .outputTo(System.out)
                        .errorTo(System.out)
                        .getExitValue() == 0);

        assertTrue(executeTestJava("--class-path", MODS_DIR.resolve(TEST_MODULE).toString(),
                                   "--module-path", MODS_DIR.resolve(MAIN_BUNDLES_MODULE).toString(),
                                   "--add-modules", MAIN_BUNDLES_MODULE,
                                   MAIN_CLASS)
                        .outputTo(System.out)
                        .errorTo(System.out)
                        .getExitValue() == 0);
    }
}
