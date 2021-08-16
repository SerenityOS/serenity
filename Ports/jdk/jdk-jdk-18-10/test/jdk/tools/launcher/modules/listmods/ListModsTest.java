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

/**
 * @test
 * @requires !vm.graal.enabled
 * @library /test/lib
 * @modules java.se
 * @build ListModsTest jdk.test.lib.compiler.CompilerUtils
 * @run testng ListModsTest
 * @summary Basic test for java --list-modules
 */

import java.nio.file.Path;
import java.nio.file.Paths;

import jdk.test.lib.compiler.CompilerUtils;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

/**
 * Basic tests for java --list-modules
 */

public class ListModsTest {

    private static final String TEST_SRC = System.getProperty("test.src");
    private static final Path SRC_DIR = Paths.get(TEST_SRC, "src");
    private static final Path MODS_DIR = Paths.get("mods");
    private static final Path UPGRADEMODS_DIR = Paths.get("upgrademods");

    @BeforeTest
    public void setup() throws Exception {
        boolean compiled;

        // javac -d mods/m1 --module-path mods src/m1/**
        compiled = CompilerUtils.compile(
                SRC_DIR.resolve("m1"),
                MODS_DIR.resolve("m1"));
        assertTrue(compiled);

        // javac -d upgrademods/java.transaction --module-path mods src/java.transaction/**
        compiled = CompilerUtils.compile(
                SRC_DIR.resolve("java.transaction"),
                UPGRADEMODS_DIR.resolve("java.transaction"));
        assertTrue(compiled);
    }

    @Test
    public void testListAll() throws Exception {
        exec("--list-modules")
                .shouldContain("java.base")
                .shouldContain("java.xml")
                .shouldHaveExitValue(0);
    }

    @Test
    public void testListWithModulePath() throws Exception {
        exec("--list-modules", "--module-path", MODS_DIR.toString())
                .shouldContain("java.base")
                .shouldContain("m1")
                .shouldHaveExitValue(0);
    }

    @Test
    public void testListWithUpgradeModulePath() throws Exception {
        String dir = UPGRADEMODS_DIR.toString();
        exec("--list-modules", "--upgrade-module-path", dir)
                .shouldContain(UPGRADEMODS_DIR.toString())
                .shouldHaveExitValue(0);
    }

    @Test
    public void testListWithLimitMods1() throws Exception {
        exec("--limit-modules", "java.management.rmi", "--list-modules")
                .shouldContain("java.rmi")
                .shouldContain("java.base")
                .shouldNotContain("java.scripting")
                .shouldHaveExitValue(0);
    }

    @Test
    public void testListWithLimitMods2() throws Exception {
        exec("--list-modules",
                    "--module-path", MODS_DIR.toString(),
                    "--limit-modules", "java.management")
                .shouldContain("java.base")
                .shouldNotContain("m1")
                .shouldHaveExitValue(0);
    }

    /**
     * java -version --list-modules => should print version and exit
     */
    @Test
    public void testListWithPrintVersion1() throws Exception {
        exec("-version", "--list-modules")
                .shouldNotContain("java.base")
                .shouldContain("Runtime Environment")
                .shouldHaveExitValue(0);
    }

    /**
     * java --list-modules -version => should list modules and exit
     */
    @Test
    public void testListWithPrintVersion2() throws Exception {
        exec("--list-modules", "-version")
                .shouldContain("java.base")
                .shouldNotContain("Runtime Environment")
                .shouldHaveExitValue(0);
    }

    /**
     * java args... returning the OutputAnalyzer to analyzer the output
     */
    private OutputAnalyzer exec(String... args) throws Exception {
        return ProcessTools.executeTestJava(args)
                .outputTo(System.out)
                .errorTo(System.out);
    }

}
