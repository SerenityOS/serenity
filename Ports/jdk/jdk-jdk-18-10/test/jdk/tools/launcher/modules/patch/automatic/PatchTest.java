/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @library /test/lib
 * @modules jdk.compiler
 * @build PatchTest
 *        jdk.test.lib.compiler.CompilerUtils
 *        jdk.test.lib.util.JarUtils
 *        jdk.test.lib.process.ProcessTools
 * @run testng PatchTest
 * @bug 8259395
 * @summary Tests patching an automatic module
 */

import java.io.File;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;

import jdk.test.lib.compiler.CompilerUtils;
import jdk.test.lib.util.JarUtils;
import static jdk.test.lib.process.ProcessTools.*;

import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;
import static org.testng.Assert.*;

public class PatchTest {

    private static final String APP_NAME = "myapp";

    private static final String MODULE_NAME = "somelib";

    private static final String EXTEND_PATCH_NAME = "patch1";
    private static final String AUGMENT_PATCH_NAME = "patch2";

    private static final String APP_MAIN = "myapp.Main";
    private static final String EXTEND_PATCH_MAIN = "somelib.test.TestMain";
    private static final String AUGMENT_PATCH_MAIN = "somelib.Dummy";

    private static final String TEST_SRC = System.getProperty("test.src");

    private static final Path APP_SRC = Path.of(TEST_SRC, APP_NAME);
    private static final Path APP_CLASSES = Path.of("classes", APP_NAME);
    private static final Path SOMELIB_SRC = Path.of(TEST_SRC, MODULE_NAME);
    private static final Path SOMELIB_EXTEND_PATCH_SRC = Path.of(TEST_SRC, EXTEND_PATCH_NAME);
    private static final Path SOMELIB_AUGMENT_PATCH_SRC = Path.of(TEST_SRC, AUGMENT_PATCH_NAME);
    private static final Path SOMELIB_CLASSES = Path.of("classes", MODULE_NAME);
    private static final Path SOMELIB_EXTEND_PATCH_CLASSES = Path.of("classes", EXTEND_PATCH_NAME);
    private static final Path SOMELIB_AUGMENT_PATCH_CLASSES = Path.of("classes", AUGMENT_PATCH_NAME);
    private static final Path SOMELIB_JAR = Path.of("mods", MODULE_NAME + "-0.19.jar");

    private static final String MODULE_PATH = String.join(File.pathSeparator, SOMELIB_JAR.toString(), APP_CLASSES.toString());

    /**
     * The test consists of 2 modules:
     *
     * somelib - dummy automatic module.
     * myapp - explicit module, uses somelib
     *
     * And two patches:
     *
     * patch1 - adds an additional package. (extend)
     * patch2 - only replaces existing classes. (augment)
     *
     */
    @BeforeClass
    public void compile() throws Exception {
        boolean compiled;

        // create mods/somelib-0.19.jar

        compiled = CompilerUtils.compile(SOMELIB_SRC, SOMELIB_CLASSES);
        assertTrue(compiled);

        JarUtils.createJarFile(SOMELIB_JAR, SOMELIB_CLASSES);


        // compile patch 1
        compiled = CompilerUtils.compile(SOMELIB_EXTEND_PATCH_SRC, SOMELIB_EXTEND_PATCH_CLASSES,
                        "--module-path", SOMELIB_JAR.toString(),
                        "--add-modules", MODULE_NAME,
                        "--patch-module", MODULE_NAME + "=" + SOMELIB_EXTEND_PATCH_SRC);
        assertTrue(compiled);

        // compile patch 2
        compiled = CompilerUtils.compile(SOMELIB_AUGMENT_PATCH_SRC, SOMELIB_AUGMENT_PATCH_CLASSES,
                        "--module-path", SOMELIB_JAR.toString(),
                        "--add-modules", MODULE_NAME,
                        "--patch-module", MODULE_NAME + "=" + SOMELIB_AUGMENT_PATCH_SRC);
        assertTrue(compiled);

        // compile app
        compiled = CompilerUtils.compile(APP_SRC, APP_CLASSES,
                        "--module-path", SOMELIB_JAR.toString());
        assertTrue(compiled);
    }

    @Test
    public void testExtendAutomaticModuleOnModulePath() throws Exception {
        int exitValue
            = executeTestJava("--module-path", MODULE_PATH,
                              "--patch-module", MODULE_NAME + "=" + SOMELIB_EXTEND_PATCH_CLASSES,
                              "-m", APP_NAME + "/" + APP_MAIN, "patch1")
                .outputTo(System.out)
                .errorTo(System.out)
                .getExitValue();

        assertTrue(exitValue == 0);
    }

    @Test
    public void testAugmentAutomaticModuleOnModulePath() throws Exception {
        int exitValue
            = executeTestJava("--module-path", MODULE_PATH,
                              "--patch-module", MODULE_NAME + "=" + SOMELIB_AUGMENT_PATCH_CLASSES,
                              "-m", APP_NAME + "/" + APP_MAIN, "patch2")
                .outputTo(System.out)
                .errorTo(System.out)
                .getExitValue();

        assertTrue(exitValue == 0);
    }

    @Test
    public void testExtendAutomaticModuleAsInitialModule() throws Exception {
        int exitValue
            = executeTestJava("--module-path", SOMELIB_JAR.toString(),
                              "--patch-module", MODULE_NAME + "=" + SOMELIB_EXTEND_PATCH_CLASSES,
                              "-m", MODULE_NAME + "/" + EXTEND_PATCH_MAIN)
                .outputTo(System.out)
                .errorTo(System.out)
                .getExitValue();

        assertTrue(exitValue == 0);
    }

    @Test
    public void testAugmentAutomaticModuleAsInitialModule() throws Exception {
        int exitValue
            = executeTestJava("--module-path", SOMELIB_JAR.toString(),
                              "--patch-module", MODULE_NAME + "=" + SOMELIB_AUGMENT_PATCH_CLASSES,
                              "-m", MODULE_NAME + "/" + AUGMENT_PATCH_MAIN)
                .outputTo(System.out)
                .errorTo(System.out)
                .getExitValue();

        assertTrue(exitValue == 0);
    }

}
