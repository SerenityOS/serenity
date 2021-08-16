/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @modules jdk.jlink/jdk.tools.jmod
 *          jdk.compiler
 * @build AddModsTest jdk.test.lib.compiler.CompilerUtils
 * @run testng AddModsTest
 * @summary Basic test for java --add-modules
 */

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import jdk.test.lib.compiler.CompilerUtils;
import static jdk.test.lib.process.ProcessTools.*;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import static org.testng.Assert.*;


@Test
public class AddModsTest {

    private static final String TEST_SRC = System.getProperty("test.src");

    private static final Path SRC_DIR = Paths.get(TEST_SRC, "src");
    private static final Path MODS1_DIR = Paths.get("mods1");
    private static final Path MODS2_DIR = Paths.get("mods2");

    // test module / main class
    private static final String TEST_MODULE = "test";
    private static final String TEST_MAIN_CLASS = "test.Main";
    private static final String TEST_MID = TEST_MODULE + "/" + TEST_MAIN_CLASS;

    // logger module
    private static final String LOGGER_MODULE = "logger";


    @BeforeTest
    public void compile() throws Exception {
        // javac -d mods1/test src/test/**
        boolean compiled = CompilerUtils.compile(
            SRC_DIR.resolve(TEST_MODULE),
            MODS1_DIR.resolve(TEST_MODULE)
        );
        assertTrue(compiled, "test did not compile");

        // javac -d mods1/logger src/logger/**
        compiled= CompilerUtils.compile(
            SRC_DIR.resolve(LOGGER_MODULE),
            MODS2_DIR.resolve(LOGGER_MODULE)
        );
        assertTrue(compiled, "test did not compile");
    }


    /**
     * Basic test of --add-modules ALL-DEFAULT. Module java.sql should be
     * resolved and the types in that module should be visible.
     */
    public void testAddDefaultModules1() throws Exception {

        // java --add-modules ALL-DEFAULT --module-path mods1 -m test ...
        int exitValue
            = executeTestJava("--module-path", MODS1_DIR.toString(),
                              "--add-modules", "ALL-DEFAULT",
                              "-m", TEST_MID,
                              "java.sql.Connection")
                .outputTo(System.out)
                .errorTo(System.out)
                .getExitValue();

        assertTrue(exitValue == 0);
    }


    /**
     * Run test on class path to load a type in a module on the application
     * module path, uses {@code --add-modules logger}.
     */
    public void testRunWithAddMods() throws Exception {

        // java --module-path mods --add-modules logger -cp classes test.Main
        String classpath = MODS1_DIR.resolve(TEST_MODULE).toString();
        String modulepath = MODS2_DIR.toString();
        int exitValue
            = executeTestJava("--module-path", modulepath,
                              "--add-modules", LOGGER_MODULE,
                              "-cp", classpath,
                              TEST_MAIN_CLASS,
                              "logger.Logger")
                .outputTo(System.out)
                .errorTo(System.out)
                .getExitValue();

        assertTrue(exitValue == 0);
    }

     /**
      * Run application on class path that makes use of module on the
      * application module path. Does not use --add-modules and so should
      * fail at run-time.
      */
     public void testRunMissingAddMods() throws Exception {

         // java --module-path mods -cp classes test.Main
         String classpath = MODS1_DIR.resolve(TEST_MODULE).toString();
         String modulepath = MODS1_DIR.toString();
         int exitValue
             = executeTestJava("--module-path", modulepath,
                               "-cp", classpath,
                               TEST_MAIN_CLASS,
                               "logger.Logger")
                 .outputTo(System.out)
                 .errorTo(System.out)
                 .shouldContain("ClassNotFoundException")
                 .getExitValue();

         assertTrue(exitValue != 0);
     }


    /**
     * Run test on class path to load a type in a module on the application
     * module path, uses {@code --add-modules ALL-MODULE-PATH}.
     */
    public void testAddAllModulePath() throws Exception {

        // java --module-path mods --add-modules ALL-MODULE-PATH -cp classes test.Main
        String classpath = MODS1_DIR.resolve(TEST_MODULE).toString();
        String modulepath = MODS1_DIR.toString();
        int exitValue
            = executeTestJava("--module-path", modulepath,
                              "--add-modules", "ALL-MODULE-PATH",
                              "-cp", classpath,
                              TEST_MAIN_CLASS)
                .outputTo(System.out)
                .errorTo(System.out)
                .getExitValue();

        assertTrue(exitValue == 0);
    }


    /**
     * Test {@code --add-modules ALL-MODULE-PATH} without {@code --module-path}.
     */
    public void testAddAllModulePathWithNoModulePath() throws Exception {

        // java --add-modules ALL-MODULE-PATH -version
        int exitValue
            = executeTestJava("--add-modules", "ALL-MODULE-PATH",
                              "-version")
                .outputTo(System.out)
                .errorTo(System.out)
                .getExitValue();

        assertTrue(exitValue == 0);
    }


    /**
     * Tests {@code --add-modules} be specified more than once.
     */
    public void testWithMultipleAddModules() throws Exception {

        String modulepath = MODS1_DIR.toString() + File.pathSeparator +
                                MODS2_DIR.toString();
        int exitValue
            = executeTestJava("--module-path", modulepath,
            "--add-modules", LOGGER_MODULE,
            "--add-modules", TEST_MODULE,
            "-m", TEST_MID,
            "logger.Logger")
            .outputTo(System.out)
            .errorTo(System.out)
            .getExitValue();

        assertTrue(exitValue == 0);
    }


    /**
     * Attempt to run with a bad module name specified to --add-modules
     */
    public void testRunWithBadAddMods() throws Exception {

        // java --module-path mods --add-modules DoesNotExist -m test ...
        int exitValue
            = executeTestJava("--module-path", MODS1_DIR.toString(),
                              "--add-modules", "DoesNotExist",
                              "-m", TEST_MID)
                .outputTo(System.out)
                .errorTo(System.out)
                .shouldContain("DoesNotExist")
                .getExitValue();

        assertTrue(exitValue != 0);
    }

}
