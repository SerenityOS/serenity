/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8221368
 * @library /test/lib
 * @modules jdk.compiler
 * @build LauncherErrors jdk.test.lib.compiler.CompilerUtils
 * @run testng LauncherErrors
 * @summary Test launcher error message when fails to launch main class
 */


import java.nio.file.Path;
import java.nio.file.Paths;

import jdk.test.lib.compiler.CompilerUtils;
import jdk.test.lib.process.ProcessTools;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class LauncherErrors {
    // test source location
    private static final String TEST_SRC = System.getProperty("test.src");
    private static final Path SRC_DIR = Paths.get(TEST_SRC, "src");

    // module path
    private static final Path MODS_DIR = Paths.get("mods");

    // the module name of the test module
    private static final String TEST_MODULE = "test2";
    // the main class of the test module
    private static final String MAIN_CLASS = "jdk.test2.Main";

    @BeforeTest
    public void compileTestModule() throws Exception {

        // javac -d mods/$TESTMODULE src/$TESTMODULE/**
        boolean compiled =
            CompilerUtils.compile(SRC_DIR.resolve(TEST_MODULE),
                                  MODS_DIR.resolve(TEST_MODULE),
                                  "--add-exports", "java.base/com.sun.crypto.provider=" + TEST_MODULE);

        assertTrue(compiled, "test module did not compile");
    }

    /*
     * Run jdk.test2.Main without security manager.
     */
    @Test
    public void test() throws Exception {
        String dir = MODS_DIR.toString();
        String mid = TEST_MODULE + "/" + MAIN_CLASS;

        ProcessTools.executeTestJava("--module-path", dir, "--module", mid)
                    .outputTo(System.out)
                    .errorTo(System.out)
                    .shouldHaveExitValue(0);
    }

    /*
     * Run jdk.test2.Main with security manager such that main class will
     * fail to initialize.
     */
    @Test
    public void testErrorMessage() throws Exception {
        String dir = MODS_DIR.toString();
        String mid = TEST_MODULE + "/" + MAIN_CLASS;

        ProcessTools.executeTestJava("-Djava.security.manager", "--module-path", dir, "--module", mid)
                    .outputTo(System.out)
                    .errorTo(System.out)
                    .shouldContain("Error: Unable to initialize main class " + MAIN_CLASS + " in module " + TEST_MODULE)
                    .shouldContain("Caused by: java.security.AccessControlException: access denied")
                    .shouldNotHaveExitValue(0);
    }

}
