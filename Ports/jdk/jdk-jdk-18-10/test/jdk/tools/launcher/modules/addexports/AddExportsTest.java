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
 * @modules java.compiler
 *          jdk.compiler
 * @build AddExportsTest jdk.test.lib.compiler.CompilerUtils
 * @run testng AddExportsTest
 * @summary Basic tests for java --add-exports
 */

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import jdk.test.lib.compiler.CompilerUtils;
import jdk.test.lib.process.OutputAnalyzer;
import static jdk.test.lib.process.ProcessTools.*;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.*;


@Test
public class AddExportsTest {

    private static final String TEST_SRC = System.getProperty("test.src");

    private static final Path SRC_DIR = Paths.get(TEST_SRC, "src");
    private static final Path MODS_DIR = Paths.get("mods");
    private static final Path UPGRADE_MODS_DIRS = Paths.get("upgrademods");

    // test module m1 that uses Unsafe
    private static final String TEST1_MODULE = "m1";
    private static final String TEST1_MAIN_CLASS = "jdk.test1.Main";

    // test module m2 uses java.compiler internals
    private static final String TEST2_MODULE = "m2";
    private static final String TEST2_MAIN_CLASS = "jdk.test2.Main";

    // test module m3 uses m4 internals
    private static final String TEST3_MODULE = "m3";
    private static final String TEST3_MAIN_CLASS = "jdk.test3.Main";
    private static final String TEST4_MODULE = "m4";


    @BeforeTest
    public void compileTestModules() throws Exception {

        // javac -d mods/m1 src/m1/**
        boolean compiled = CompilerUtils.compile(
                SRC_DIR.resolve(TEST1_MODULE),
                MODS_DIR.resolve(TEST1_MODULE),
                "--add-exports", "java.base/jdk.internal.misc=m1");
        assertTrue(compiled, "module " + TEST1_MODULE + " did not compile");

        // javac -d upgrademods/java.compiler src/java.compiler/**
        compiled = CompilerUtils.compile(
                SRC_DIR.resolve("java.compiler"),
                UPGRADE_MODS_DIRS.resolve("java.compiler"));
        assertTrue(compiled, "module java.compiler did not compile");

        // javac --upgrade-module-path upgrademods -d mods/m2 src/m2/**
        compiled = CompilerUtils.compile(
                SRC_DIR.resolve(TEST2_MODULE),
                MODS_DIR.resolve(TEST2_MODULE),
                "--upgrade-module-path", UPGRADE_MODS_DIRS.toString(),
                "--add-exports", "java.compiler/javax.tools.internal=m2");
        assertTrue(compiled, "module " + TEST2_MODULE + " did not compile");

        // javac -d mods/m3 src/m3/**
        compiled = CompilerUtils.compile(
                SRC_DIR.resolve(TEST3_MODULE),
                MODS_DIR.resolve(TEST3_MODULE));
        assertTrue(compiled, "module " + TEST3_MODULE + " did not compile");

        // javac -d mods/m4 src/m4/**
        compiled = CompilerUtils.compile(
                SRC_DIR.resolve(TEST4_MODULE),
                MODS_DIR.resolve(TEST4_MODULE));
        assertTrue(compiled, "module " + TEST4_MODULE + " did not compile");
    }

    /**
     * Sanity check with -version
     */
    public void testSanity() throws Exception {

        int exitValue
            =  executeTestJava("--add-exports", "java.base/jdk.internal.reflect=ALL-UNNAMED",
                               "-version")
                .outputTo(System.out)
                .errorTo(System.out)
                .getExitValue();

        assertTrue(exitValue == 0);
    }


    /**
     * Run class path application that uses jdk.internal.misc.Unsafe
     */
    public void testUnnamedModule() throws Exception {

        // java --add-exports java.base/jdk.internal.misc=ALL-UNNAMED \
        //      -cp mods/$TESTMODULE jdk.test.UsesUnsafe

        String classpath = MODS_DIR.resolve(TEST1_MODULE).toString();
        int exitValue
            = executeTestJava("--add-exports", "java.base/jdk.internal.misc=ALL-UNNAMED",
                              "-cp", classpath,
                              TEST1_MAIN_CLASS)
                .outputTo(System.out)
                .errorTo(System.out)
                .getExitValue();

        assertTrue(exitValue == 0);
    }


    /**
     * Run named module that uses jdk.internal.misc.Unsafe
     */
    public void testNamedModule() throws Exception {

        //  java --add-exports java.base/jdk.internal.misc=test \
        //       --module-path mods -m $TESTMODULE/$MAIN_CLASS

        String mid = TEST1_MODULE + "/" + TEST1_MAIN_CLASS;
        int exitValue =
            executeTestJava("--add-exports", "java.base/jdk.internal.misc=" + TEST1_MODULE,
                            "--module-path", MODS_DIR.toString(),
                            "-m", mid)
                .outputTo(System.out)
                .errorTo(System.out)
                .getExitValue();

        assertTrue(exitValue == 0);
    }

    /**
     * Test --add-exports with upgraded module
     */
    public void testWithUpgradedModule() throws Exception {

        // java --add-exports java.compiler/javax.tools.internal=m2
        //      --upgrade-module-path upgrademods --module-path mods -m ...
        String mid = TEST2_MODULE + "/" + TEST2_MAIN_CLASS;
        int exitValue = executeTestJava(
                "--add-exports", "java.compiler/javax.tools.internal=m2",
                "--upgrade-module-path", UPGRADE_MODS_DIRS.toString(),
                "--module-path", MODS_DIR.toString(),
                "-m", mid)
                .outputTo(System.out)
                .errorTo(System.out)
                .getExitValue();

        assertTrue(exitValue == 0);
    }

    /**
     * Test --add-exports with module that is added to the set of root modules
     * with --add-modules.
     */
    public void testWithAddMods() throws Exception {

        // java --add-exports m4/jdk.test4=m3 --module-path mods -m ...
        String mid = TEST3_MODULE + "/" + TEST3_MAIN_CLASS;
        int exitValue = executeTestJava(
                "--add-exports", "m4/jdk.test4=m3",
                "--module-path", MODS_DIR.toString(),
                "--add-modules", TEST4_MODULE,
                "-m", mid)
                .outputTo(System.out)
                .errorTo(System.out)
                .getExitValue();

        assertTrue(exitValue == 0);
    }


    /**
     * --add-exports and --add-opens allows duplicates
     */
    public void testWithDuplicateOption() throws Exception {

        int exitValue
            =  executeTestJava("--add-exports", "java.base/jdk.internal.reflect=ALL-UNNAMED",
                               "--add-exports", "java.base/jdk.internal.reflect=ALL-UNNAMED",
                               "--add-opens", "java.base/java.util=ALL-UNNAMED",
                               "--add-opens", "java.base/java.util=ALL-UNNAMED",
                               "-version")
                .outputTo(System.out)
                .errorTo(System.out)
                .getExitValue();

        assertTrue(exitValue == 0);
    }


    private OutputAnalyzer execJava(String... options) {
        try {
            return executeTestJava(options);
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    /**
     * Exercise --add-exports and --add-opens with unknown values.
     * Warning is emitted.
     */
    @Test(dataProvider = "unknownvalues")
    public void testWithUnknownValue(String value, String ignore) {
        Stream.of("--add-exports", "--add-opens")
            .forEach(option -> {
                //  --add-exports $VALUE -version
                int exitValue = execJava(option, value, "-version")
                    .stderrShouldMatch("WARNING: .*.monkey.*")
                    .outputTo(System.out)
                    .errorTo(System.out)
                    .getExitValue();

                assertTrue(exitValue == 0);
            });
    }


    @DataProvider(name = "unknownvalues")
    public Object[][] unknownValues() {
        return new Object[][]{

            { "java.base/jdk.internal.misc=sun.monkey", null }, // unknown target
            { "java.monkey/sun.monkey=ALL-UNNAMED",     null }, // unknown module
            { "java.base/sun.monkey=ALL-UNNAMED",       null }, // unknown package
            { "java.monkey/sun.monkey=ALL-UNNAMED",     null }, // unknown module/package

        };
    }


    /**
     * Exercise --add-exports and --add-opens with bad values
     */
    @Test(dataProvider = "badvalues")
    public void testWithBadValue(String value, String ignore) {
        Stream.of("--add-exports", "--add-opens")
            .forEach(option -> {
                //  --add-exports $VALUE -version
                int exitValue = execJava(option, value, "-version")
                    .outputTo(System.out)
                    .errorTo(System.out)
                    .getExitValue();

        assertTrue(exitValue != 0);
            });
    }

    @DataProvider(name = "badvalues")
    public Object[][] badValues() {
        return new Object[][]{

            { "java.base/jdk.internal.misc",            null }, // missing target
            { "java.base=ALL-UNNAMED",                  null }, // missing package
            { "java.base/=ALL-UNNAMED",                 null }  // missing package

        };
    }
}
