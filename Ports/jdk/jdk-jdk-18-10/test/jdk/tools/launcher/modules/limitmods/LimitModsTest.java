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
 * @requires !vm.graal.enabled
 * @library /test/lib
 * @modules java.desktop java.logging jdk.compiler
 * @build LimitModsTest jdk.test.lib.compiler.CompilerUtils
 * @run testng LimitModsTest
 * @summary Basic tests for java --limit-modules
 */

import java.nio.file.Path;
import java.nio.file.Paths;

import jdk.test.lib.compiler.CompilerUtils;
import static jdk.test.lib.process.ProcessTools.*;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

@Test
public class LimitModsTest {

    private static final String TEST_SRC = System.getProperty("test.src");

    private static final Path SRC_DIR = Paths.get(TEST_SRC, "src");
    private static final Path MODS_DIR = Paths.get("mods");

    // the module name / main class of the test module
    private static final String TEST_MODULE = "test";
    private static final String MAIN_CLASS = "jdk.test.UseAWT";


    @BeforeTest
    public void compileTestModule() throws Exception {

        // javac -d mods/$TESTMODULE src/$TESTMODULE/**
        boolean compiled
            = CompilerUtils.compile(SRC_DIR.resolve(TEST_MODULE),
                MODS_DIR.resolve(TEST_MODULE));

        assertTrue(compiled, "test module did not compile");
    }


    /**
     * Basic test of --limit-modules to limit which platform modules are observable.
     */
    public void testLimitingPlatformModules() throws Exception {
        int exitValue;

        // java --limit-modules java.base --list-modules
        exitValue = executeTestJava("--limit-modules", "java.base", "--list-modules")
            .outputTo(System.out)
            .errorTo(System.out)
            .shouldContain("java.base")
            .shouldNotContain("java.logging")
            .shouldNotContain("java.xml")
            .getExitValue();

        assertTrue(exitValue == 0);


        // java --limit-modules java.logging --list-modules
        exitValue = executeTestJava("--limit-modules", "java.logging", "--list-modules")
            .outputTo(System.out)
            .errorTo(System.out)
            .shouldContain("java.base")
            .shouldContain("java.logging")
            .shouldNotContain("java.xml")
            .getExitValue();

        assertTrue(exitValue == 0);
    }


    /**
     * Test --limit-modules with --add-modules
     */
    public void testWithAddMods() throws Exception {
        int exitValue;

        // java --limit-modules java.base --add-modules java.logging --list-modules
        exitValue = executeTestJava("--limit-modules", "java.base",
                                    "--add-modules", "java.logging",
                                    "--list-modules")
            .outputTo(System.out)
            .errorTo(System.out)
            .shouldContain("java.base")
            .shouldContain("java.logging")
            .shouldNotContain("java.xml")
            .getExitValue();

        assertTrue(exitValue == 0);


        // java --limit-modules java.base --add-modules java.sql --list-modules
        // This should fail because java.sql has dependences beyond java.base
        exitValue = executeTestJava("--limit-modules", "java.base",
                                    "--add-modules", "java.sql",
                                    "--list-modules")
            .outputTo(System.out)
            .errorTo(System.out)
            .getExitValue();

        assertTrue(exitValue != 0);
    }


    /**
     * Run class path application with --limit-modules
     */
    public void testUnnamedModule() throws Exception {
        String classpath = MODS_DIR.resolve(TEST_MODULE).toString();

        // java --limit-modules java.base -cp mods/$TESTMODULE ...
        int exitValue1
            = executeTestJava("--limit-modules", "java.base",
                              "-cp", classpath,
                              MAIN_CLASS)
                .outputTo(System.out)
                .errorTo(System.out)
                .shouldContain("NoClassDefFoundError")
                .getExitValue();

        assertTrue(exitValue1 != 0);


        // java --limit-modules java.base -cp mods/$TESTMODULE ...
        int exitValue2
            = executeTestJava("--limit-modules", "java.desktop",
                              "-cp", classpath,
                             MAIN_CLASS)
                .outputTo(System.out)
                .errorTo(System.out)
                .getExitValue();

        assertTrue(exitValue2 == 0);
    }


    /**
     * Run named module with --limit-modules
     */
    public void testNamedModule() throws Exception {

        String modulepath = MODS_DIR.toString();
        String mid = TEST_MODULE + "/" + MAIN_CLASS;

        // java --limit-modules java.base --module-path mods -m $TESTMODULE/$MAINCLASS
        int exitValue = executeTestJava("--limit-modules", "java.base",
                                        "--module-path", modulepath,
                                        "-m", mid)
                .outputTo(System.out)
                .errorTo(System.out)
                .getExitValue();

        assertTrue(exitValue != 0);

        // java --limit-modules java.desktop --module-path mods -m $TESTMODULE/$MAINCLASS
        exitValue = executeTestJava("--limit-modules", "java.desktop",
                                    "--module-path", modulepath,
                                    "-m", mid)
                .outputTo(System.out)
                .errorTo(System.out)
                .getExitValue();

        assertTrue(exitValue == 0);
    }

}
