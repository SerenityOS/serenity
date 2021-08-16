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

/**
 * @test
 * @bug 8159596
 * @library /test/lib
 * @modules jdk.compiler
 *          jdk.jartool
 * @build DryRunTest jdk.test.lib.process.ProcessTools
 *        jdk.test.lib.compiler.CompilerUtils
 * @run testng DryRunTest
 * @summary Test java --dry-run
 */

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.spi.ToolProvider;

import jdk.test.lib.compiler.CompilerUtils;
import jdk.test.lib.process.ProcessTools;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

@Test
public class DryRunTest {

    private static final String TEST_SRC = System.getProperty("test.src");

    private static final Path SRC_DIR = Paths.get(TEST_SRC, "src");
    private static final Path MODS_DIR = Paths.get("mods");
    private static final Path LIBS_DIR = Paths.get("libs");

    // the module name of the test module
    private static final String TEST_MODULE = "test";
    private static final String M_MODULE = "m";

    // the module main class
    private static final String MAIN_CLASS = "jdk.test.Main";
    private static final String MAIN_CLINIT_CLASS = "jdk.test.MainWithClinit";


    @BeforeTest
    public void compileTestModule() throws Exception {

        // javac -d mods/$TESTMODULE src/$TESTMODULE/**
        assertTrue(CompilerUtils.compile(SRC_DIR.resolve(M_MODULE),
                                         MODS_DIR,
                                         "--module-source-path", SRC_DIR.toString()));

        assertTrue(CompilerUtils.compile(SRC_DIR.resolve(TEST_MODULE),
                                         MODS_DIR,
                                         "--module-source-path", SRC_DIR.toString()));

        Files.createDirectories(LIBS_DIR);

        // create JAR files with no module-info.class
        assertTrue(jar(M_MODULE, "p/Lib.class") == 0);
        assertTrue(jar(TEST_MODULE, "jdk/test/Main.class") == 0);
    }

    /**
     * Execute "java" with the given arguments, returning the exit code.
     */
    private int exec(String... args) throws Exception {
       return ProcessTools.executeTestJava(args)
                .outputTo(System.out)
                .errorTo(System.out)
                .getExitValue();
    }


    /**
     * Launch module main
     */
    public void testModule() throws Exception {
        String dir = MODS_DIR.toString();
        String mid = TEST_MODULE + "/" + MAIN_CLASS;

        // no resolution failure
        int exitValue = exec("--dry-run", "--module-path", dir, "-m", mid);
        assertTrue(exitValue == 0);
    }

    /**
     * Test dryrun that does not invoke <clinit> of the main class
     */
    public void testMainClinit() throws Exception {
        String dir = MODS_DIR.toString();
        String mid = TEST_MODULE + "/" + MAIN_CLINIT_CLASS;

        int exitValue = exec("--dry-run", "--module-path", dir, "-m", mid);
        assertTrue(exitValue == 0);

        // expect the test to fail if main class is initialized
        exitValue = exec("--module-path", dir, "-m", mid);
        assertTrue(exitValue != 0);
    }

    /**
     * Test non-existence module in --add-modules
     */
    public void testNonExistAddModules() throws Exception {
        String dir = MODS_DIR.toString();
        String mid = TEST_MODULE + "/" + MAIN_CLASS;

        int exitValue = exec("--dry-run", "--module-path", dir,
                             "--add-modules", "non.existence",
                             "-m", mid);
        assertTrue(exitValue != 0);
    }

    /**
     * Launch main class from class path
     */
    public void testClassPath() throws Exception {
        Path testJar = LIBS_DIR.resolve(TEST_MODULE + ".jar");
        String libs = testJar.toString() + File.pathSeparator +
                        LIBS_DIR.resolve(M_MODULE + ".jar").toString();

        // test pass with m.jar:test.jar
        int exitValue = exec("-classpath", libs, MAIN_CLASS);
        assertTrue(exitValue == 0);

        // m.jar is not on classpath and fails with p.Lib not found
        exitValue = exec("-classpath", testJar.toString(), MAIN_CLASS);
        assertTrue(exitValue != 0);

        // dry pass passes since main is not executed
        exitValue = exec("--dry-run", "-classpath", testJar.toString(), MAIN_CLASS);
        assertTrue(exitValue == 0);
    }

    /**
     * Test automatic modules
     */
    public void testAutomaticModule() throws Exception {
        String libs = LIBS_DIR.resolve(M_MODULE + ".jar").toString() +
                        File.pathSeparator +
                        LIBS_DIR.resolve(TEST_MODULE + ".jar").toString();
        String mid = TEST_MODULE + "/" + MAIN_CLASS;

        // test main method with and without --add-modules mm
        int exitValue = exec("--module-path", LIBS_DIR.toString(),
                             "-m", mid);
        assertTrue(exitValue == 0);
    }

    /**
     * module m not found
     */
    public void testMissingModule() throws Exception {
        String subdir = MODS_DIR.resolve(TEST_MODULE).toString();
        String mid = TEST_MODULE + "/" + MAIN_CLASS;

        // resolution failure
        int exitValue = exec("--dry-run", "--module-path", subdir, "-m", mid);
        assertTrue(exitValue != 0);
    }

    private static final ToolProvider JAR_TOOL = ToolProvider.findFirst("jar")
        .orElseThrow(() ->
            new RuntimeException("jar tool not found")
        );

    private static int jar(String name, String entries) throws IOException {
        Path jar = LIBS_DIR.resolve(name + ".jar");

        // jar --create ...
        String classes = MODS_DIR.resolve(name).toString();
        String[] args = {
            "--create",
            "--file=" + jar,
            "-C", classes, entries
        };
        return JAR_TOOL.run(System.out, System.out, args);
    }
}
